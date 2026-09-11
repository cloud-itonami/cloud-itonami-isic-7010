(ns headoffice.facts
  "Per-jurisdiction group-governance/transfer-pricing regulatory
  catalog -- the G2-style spec-basis table the Group Oversight
  Governor checks every `:report/verify` proposal against ('did the
  advisor cite an OFFICIAL public source for this jurisdiction's
  group-reporting/transfer-pricing framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official corporate-
  tax/transfer-pricing authority (see `:provenance`); they are a
  STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the unit-
  report/budget-authorization/transfer-price-benchmark/allocation-
  completion evidence set this blueprint's own Offer names;
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:actuation/finalize-
  allocation` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "国税庁 (National Tax Agency)"
          :legal-basis "租税特別措置法第66条の4 (移転価格税制 -- Special Taxation Measures Act Art. 66-4, transfer pricing)"
          :national-spec "国外関連者との取引に係る独立企業間価格算定および文書化義務"
          :provenance "https://www.nta.go.jp/law/tsutatsu/kihon/hojin/sochihou/index.htm"
          :required-evidence ["ユニット報告記録 (unit-report-record)"
                              "予算承認記録 (budget-authorization-record)"
                              "移転価格ベンチマーク記録 (transfer-price-benchmark-record)"
                              "配分完了記録 (allocation-completion-record)"]}
   "USA" {:name "United States"
          :owner-authority "Internal Revenue Service (IRS)"
          :legal-basis "Internal Revenue Code §482 -- arm's-length standard for transactions between related enterprises"
          :national-spec "Treasury Regulations §1.482 transfer pricing methods and contemporaneous documentation requirements"
          :provenance "https://www.irs.gov/businesses/international-businesses/section-482-white-paper"
          :required-evidence ["Unit-report record"
                              "Budget-authorization record"
                              "Transfer-price-benchmark record"
                              "Allocation-completion record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "HM Revenue & Customs (HMRC)"
          :legal-basis "Taxation (International and Other Provisions) Act 2010, Part 4 -- UK transfer pricing rules (OECD-aligned)"
          :national-spec "Arm's-length pricing requirement for provisions between connected enterprises"
          :provenance "https://www.gov.uk/hmrc-internal-manuals/international-manual/intm412000"
          :required-evidence ["Unit-report record"
                              "Budget-authorization record"
                              "Transfer-price-benchmark record"
                              "Allocation-completion record"]}
   "DEU" {:name "Germany"
          :owner-authority "Bundeszentralamt für Steuern (BZSt)"
          :legal-basis "Außensteuergesetz (AStG) §1 -- Verrechnungspreise zwischen nahestehenden Personen"
          :national-spec "Fremdvergleichsgrundsatz (arm's-length principle) und Dokumentationspflichten für konzerninterne Leistungsbeziehungen"
          :provenance "https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/VerrechnungspreiseUnternehmen/verrechnungspreise_node.html"
          :required-evidence ["Einheitsberichtsprotokoll (unit-report-record)"
                              "Budgetgenehmigungsprotokoll (budget-authorization-record)"
                              "Verrechnungspreis-Benchmark-Protokoll (transfer-price-benchmark-record)"
                              "Zuteilungsabschlussprotokoll (allocation-completion-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize an
  allocation on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7010 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `headoffice.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
