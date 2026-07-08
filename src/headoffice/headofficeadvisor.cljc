(ns headoffice.headofficeadvisor
  "HeadOffice-LLM client -- the *contained intelligence node* for the
  head-office actor.

  It normalizes unit intake, drafts a per-jurisdiction group-
  reporting/transfer-pricing evidence checklist, and drafts the
  allocation-finalization action. CRITICAL: it is a smart-but-
  untrusted advisor. It returns a *proposal* (with a rationale + the
  fields it cited), never a committed record or a real allocation
  finalization. Every output is censored downstream by `headoffice.
  governor` before anything touches the SSoT, and `:actuation/
  finalize-allocation` proposals NEVER auto-commit at any phase --
  see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-allocation | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [headoffice.facts :as facts]
            [headoffice.registry :as registry]
            [headoffice.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the unit or jurisdiction. High confidence, low
  stakes."
  [_db {:keys [patch]}]
  {:summary    (str "ユニット記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :unit/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-report
  "Per-jurisdiction group-reporting/transfer-pricing evidence
  checklist draft. `:no-spec?` injects the failure mode we must
  defend against: proposing a checklist for a jurisdiction with NO
  official spec-basis in `headoffice.facts` -- the Group Oversight
  Governor must reject this (never invent a jurisdiction's
  requirements)."
  [db {:keys [subject no-spec?]}]
  (let [u (store/unit db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction u))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "headoffice.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :report/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :report/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-allocation-finalization
  "Draft the actual ALLOCATION-FINALIZATION action -- finalizing a
  real group budget/policy allocation. ALWAYS `:stake :actuation/
  finalize-allocation` -- this is a REAL-WORLD act, never a draft the
  actor may auto-run. See README `Actuation`: no phase ever adds this
  op to a phase's `:auto` set (`headoffice.phase`); the governor also
  always escalates on `:actuation/finalize-allocation`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [u (store/unit db subject)
        safe? (and u (not (registry/transfer-price-outside-arms-length-range? u))
                   (not (registry/budget-allocation-exceeds-authorized-limit? u)))]
    {:summary    (str subject " 向け配分確定提案"
                      (when u (str " (unit=" (:unit-name u) ")")))
     :rationale  (if u
                   (str "transfer-price=" (:transfer-price u)
                        " arms-length-range=[" (:arms-length-range-min u) "," (:arms-length-range-max u) "]"
                        " proposed-allocation-amount=" (:proposed-allocation-amount u)
                        " authorized-allocation-limit=" (:authorized-allocation-limit u))
                   "ユニット記録が見つかりません")
     :cites      (if u [subject] [])
     :effect     :unit/mark-finalized
     :value      {:unit-id subject}
     :stake      :actuation/finalize-allocation
     :confidence (if safe? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :unit/intake                (normalize-intake db request)
    :report/verify               (verify-report db request)
    :actuation/finalize-allocation (propose-allocation-finalization db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは本社機能事業(グループ予算・方針配分)の配分確定エージェントの"
       "助言者です。与えられた事実のみに基づき、提案を1つだけEDNマップで"
       "返します。説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:unit/upsert|:report/set|:unit/mark-finalized) "
       ":stake(:actuation/finalize-allocation か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :report/verify                 {:unit (store/unit st subject)}
    :actuation/finalize-allocation  {:unit (store/unit st subject)}
    {:unit (store/unit st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Group Oversight Governor
  escalates/holds -- an LLM hiccup can never auto-finalize an
  allocation."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :headofficeadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
