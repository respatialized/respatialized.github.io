(ns respatialized.document
  "Namespace for document processing."
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer
    [counted-double-list ft-split-at ft-concat]]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [minimallist.core :as m :refer [valid? describe]]
   [minimallist.helper :as h]
   [minimallist.generator :as mg]))

(def block-level-tags
  "MDN list of block-level HTML element tags"
  #{:address :article :aside :blockquote
    #_:details :dialog :dd :div :dl :dt
    :fieldset :figcaption :figure :footer
    :form :h1 :h2 :h3 :h4 :h5 :h6 :header
    :hr :li :main :nav :ol :p :pre :section
    #_:table :ul})

(def inline-tags
  "MDN list of inline HTML element tags"
  #{:a :abbr :b :bdi :bdo :br #_:button
    :canvas :cite :code :data #_:datalist
    :del :dfn :em #_:embed :i #_:iframe :img
    #_:input :ins :kbd #_:label :mark #_:meter
    #_:noscript #_:object #_:output #_:picture
    #_:progress :q #_:ruby :s :samp :script
    #_:select #_:slot :small :span :strong
    :sub :sup #_:svg #_:template :time :u
    :tt :var #_:video :wbr})

(def metadata-tags
  "MDN list of metadata content element tags"
  #{:base :link :meta #_:noscript :script
    :style :title})

(def flow-tags
  "MDN list of flow content element tags"
  #{:a :abbr :aside #_:audio :b :bdo :bdi
    :blockquote :br #_:button #_:canvas :cite
    :code :data #_:datalist :del #_:details :dfn
    :div :dl :em #_:embed #_:fieldset :figure
    :footer #_:form :h1 :h2 :h3 :h4 :h5 :h6
    :header :hr :i #_:iframe :img #_:input :ins
    :kbd #_:label :main #_:map :mark #_:math #_:menu
    #_:meter :nav #_:noscript #_:object :ol #_:output
    :p #_:picture :pre #_:progress :q #_:ruby :s
    :samp :script :section #_:select :small
    :span :strong :sub :sup #_:svg :table
    #_:template #_:textarea :time :ul :var #_:video
    :wbr})

(def sectioning-tags
  "MDN list of sectioning content element tags"
  #{:article :aside :nav :section})

(def heading-tags
  "MDN list of heading content element tags"
  #{:h1 :h2 :h3 :h4 :h5 :h6})

(def phrasing-tags
  "MDN list of phrasing content element tags"
  #{:abbr #_:audio :b :bdi :bdo :br #_:button #_:canvas :cite
    :code :data #_:datalist :dfn :em #_:embed :i #_:iframe :img
    #_:input :kbd #_:label :mark #_:math #_:meter #_:noscript
    #_:object #_:output #_:picture #_:progress :q #_:ruby :samp
    :script #_:select :small :span :strong :sub :sup #_:svg
    #_:textarea :time :var #_:video :wbr})

(def phrasing-subtags
  "MDN list of tags that are phrasing content when they contain only phrasing content."
  #{:a :del :ins :map})

(def embedded-tags
  "MDN list of embedded content element tags"
  #{:audio :canvas #_:embed :iframe :img #_:math
    :object #_:picture :svg :video})

(def interactive-tags
  "MDN list of interactive content element tags"
  #{:a #_:button #_:details #_:embed #_:iframe #_:label
    #_:select #_:textarea})

;; "content is palpable when it's neither empty or hidden;
;; it is content that is rendered and is substantive.
;; Elements whose model is flow content or phrasing content
;; should have at least one node which is palpable."

(def transparent-tags
  "MDN list of transparent content tags"
  #{:ins :del :object})

(def external-link-pattern #"https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
(def internal-link-pattern #"/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(defn regex->model [re]
  (-> (h/fn #(and (string? %) re-matches re %))
      (h/with-test-check-gen
        (gen'/string-from-regex re))))

(def url
  (h/alt
   [:external (regex->model external-link-pattern)]
   [:internal (regex->model internal-link-pattern)]))

(def global-attributes
  "MDN list of global HTML attributes as minimallist spec"
  (h/with-optional-entries
    (h/map)
    [:class (h/fn string?)]
    [:contenteditable (h/enum #{"true" "false" ""})]
    [:dir (h/enum #{"ltr" "rtl" "auto"})]
    [:hidden (h/fn boolean?)]
    [:id (h/fn string?)]
    [:itemid (h/fn string?)]
    [:itemprop (h/fn string?)]
    [:itemref (h/fn string?)]
    [:itemscope (h/fn boolean?)]
    [:itemtype (regex->model external-link-pattern)]
    [:lang (h/val "en")]
    [:tabindex (h/fn int?)]
    [:title (h/fn string?)]))

;; (def )

(def q
  (h/in-vector
   (h/cat [:tag (h/val :q)]
          [:attributes
           (h/with-optional-entries
             global-attributes
             [:cite url])])))

(def doc-tree
  {:article #{:section :hr}
   :section #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :pre :span :p :div :script :strong}
   :div #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :pre :span :p :strong :script}
   :p #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :span :p :strong}
   :pre #{:em :span :a :strong :code}
   :em #{:code :span :a :strong}
   :strong #{:code :span :a :em}
   :a #{:em :span :strong :code}
   :code #{:em :a :span :strong}
   :blockquote #{:p :span :em :code :a :strong :ol :ul}
   :ol #{:li}
   :ul #{:li}
   :li #{:code :em :span :a :blockquote :strong}
   :h1 #{:code :em :span :a :strong}
   :h2 #{:code :em :span :a :strong}
   :h3 #{:code :em :span :a :strong}
   :h4 #{:code :em :span :a :strong}
   :h5 #{:code :em :span :a :strong}
   :h6 #{:code :em :span :a :strong}
   :span #{:em :strong :a}
   :script #{}
   :hr #{}})

(defn ->constrained-model
  ([pred generator max-tries-or-opts]
   (-> (h/fn pred)
       (h/with-test-check-gen
         (gen/such-that pred generator max-tries-or-opts))))
  ([pred generator]
   (-> (h/fn pred)
       (h/with-test-check-gen
         (gen/such-that pred generator)))))

(comment
  ;; example code from minimallist
  (def hiccup-model
    (h/let ['hiccup (h/alt [:node (h/in-vector (h/cat [:name (h/fn keyword?)]
                                                      [:props (h/? (h/map-of [(h/fn keyword?) (h/fn any?)]))]
                                                      [:children (h/* (h/not-inlined (h/ref 'hiccup)))]))]
                           [:element (h/alt [:nil (h/fn nil?)]
                                            [:boolean (h/fn boolean?)]
                                            [:number (h/fn number?)]
                                            [:text (h/fn string?)])])]
      (h/ref 'hiccup))))

(def attr-map
  (h/with-optional-entries (h/map)
    [:class (-> (h/fn string?) (h/with-test-check-gen gen/string-ascii))]
    [:title (-> (h/fn string?) (h/with-test-check-gen gen/string-ascii))]
    [:href url]
    [:src url]
    [:id (-> (h/fn string?) (h/with-test-check-gen gen/string-ascii))]
    [:alt (-> (h/fn string?) (h/with-test-check-gen gen/string-ascii))]
    [:lang (h/enum #{"en"})]))

(def img-attrs
  (h/with-optional-entries
    (h/with-entries attr-map [:src url])
    [:width (->constrained-model #(< 0 % 8192) gen/nat)]
    [:height (->constrained-model #(< 0 % 8192) gen/nat)]))

(def img
  (h/in-vector
   (h/cat [:tag (h/val :img)]
          [:attributes img-attrs])))

(def atomic-element
  (h/alt
   [:boolean (-> (h/fn boolean?) (h/with-test-check-gen gen/boolean))]
   [:number (-> (h/fn number?) (h/with-test-check-gen
                                 (gen/one-of [gen/small-integer
                                              gen/double])))]
   [:text (-> (h/fn string?) (h/with-test-check-gen gen/string))]))

(defn quote-kw [kw] `~(symbol kw))
(defn elem-ref [e] [e (h/ref (quote-kw e))])

(defn has-required?
  "Checks to see if at least one entry in the given map model has required keys"
  [model]
  (and
   (= :map (:type model))
   (some #(nil? (get % :optional)) (:entries model))))

(defn ->child-model
  "Helper function to be used in constructing recursive models"
  ([elem] (->child-model elem (get doc-tree elem) global-attributes))
  ([elem attrs] (->child-model elem (get doc-tree elem) attrs))
  ([elem sub-elems attrs]
   (h/in-vector
    (h/cat
     [:tag (h/val elem)]
     [:attributes
      (if (has-required? attrs) attrs (h/? attrs))]
     [:contents
      (h/* (h/not-inlined (apply h/alt
                                 atomic-element
                                 (map elem-ref
                                      sub-elems))))]))))

(def raster-span
  (h/alt
   [:row (h/val "row")]
   [:range (regex->model #"\d\-\d")]
   [:offset (regex->model #"\d\+\d")]
   [:offset-row (regex->model #"\d\.\.")]
   [:cols
    (h/alt
     [:int (->constrained-model #(< 0 % 33) gen/nat 200)]
     [:int-str (->constrained-model #(< 0 (Integer/parseInt %) 33)
                                    (gen/fmap str gen/nat) 200)])]))


(defn ->hiccup-model
  ([elem-tag attrs contents]
   (let [ms [[:tag (h/val elem-tag)]
             [:attributes (if (has-required? attrs) attrs (h/? attrs))]
             (if (= :empty contents) nil
                 [:contents
                  (h/*
                   (h/not-inlined
                    (apply h/alt
                           [:atomic-element atomic-element]
                           contents)))])]]
     (h/not-inlined
      (h/in-vector
       (apply h/cat (filter some? ms))))))
  ([elem-tag contents] (->hiccup-model elem-tag global-attributes contents)))

(comment

 ; ruby isn't supported yet
  (let
   [ruby (h/in-vector
          (h/cat
           [:tag (h/val :ruby)]
           [:attributes (h/? global-attributes)]
           [:contents
            (h/+ (h/not-inlined
                  (h/cat
                   [:character (h/fn string?)]
                   [:pronunciation (h/in-vector
                                    (h/cat [:tag (h/val :rt)]
                                           [:text (h/fn string?)]))])))]))])

  (def ruby [:ruby "漢" [:rt "kan"] "字" [:rt "ji"]])

  (def datetime-str
    (h/alt
     [:year (->constrained-model #())])))

(def elements
  (h/let
      ;; inline text elements first
      ['a (->hiccup-model :a
                          (-> global-attributes
                              (h/with-entries
                                [:href
                                 (h/alt
                                  [:link url]
                                  [:fragment (h/fn string?)])])
                              (h/with-optional-entries
                                [:download (h/fn string?)]
                                [:rel (h/fn string?)]
                                [:target (h/enum #{"_self" "_blank" "_parent" "_top"})]))
                          (map elem-ref  (set/union (set/difference flow-tags interactive-tags)
                                                    phrasing-tags)))
       'abbr (->hiccup-model :abbr (map elem-ref phrasing-tags))
       'b (->hiccup-model :b (map elem-ref phrasing-tags))
       'bdi (->hiccup-model :bdi (map elem-ref phrasing-tags))
       'bdo (->hiccup-model :bdo
                            (h/with-entries global-attributes
                              [:dir (h/enum #{"ltr" "rtl"})])
                            (map elem-ref phrasing-tags))
       'br (h/val [:br])
       'cite (->hiccup-model :cite (map elem-ref phrasing-tags))
       'code (->hiccup-model :code (map elem-ref phrasing-tags))
       'data (->hiccup-model :data
                             (h/with-entries global-attributes
                               [:value (h/fn string?)])
                             (map elem-ref phrasing-tags))
       'del (->hiccup-model :del [])
       'ins (->hiccup-model :ins [])
       'dfn (->hiccup-model :dfn
                            (map elem-ref (disj phrasing-tags :dfn)))
       'em (->hiccup-model :em (map elem-ref phrasing-tags))
       'i (->hiccup-model :i (map elem-ref phrasing-tags))
       'kbd (->hiccup-model :kbd (map elem-ref phrasing-tags))
       'mark (->hiccup-model :mark (map elem-ref phrasing-tags))
       'q (->hiccup-model :q
                          (h/with-optional-entries global-attributes
                            [:cite url])
                          (map elem-ref phrasing-tags))
       's (->hiccup-model :s (map elem-ref phrasing-tags))
       'samp (->hiccup-model :samp (map elem-ref phrasing-tags))
       'img (h/in-vector (h/cat
                          [:tag (h/val :img)]
                          [:attributes (-> global-attributes
                                           (h/with-entries [:src url])
                                           (h/with-optional-entries
                                             [:alt (h/fn string?)]
                                             [:sizes (h/fn string?)]
                                             [:width (->constrained-model #(< 0 % 8192) gen/small-integer)]
                                             [:height (->constrained-model #(< 0 % 8192) gen/small-integer)]
                                             [:loading (h/enum #{"eager" "lazy"})]
                                             [:decoding (h/enum #{"sync" "async" "auto"})]
                                             [:crossorigin (h/enum #{"anonymous" "use-credentials"})]))]))
       'small (->hiccup-model :small (map elem-ref phrasing-tags))
       'span (->hiccup-model :span (map elem-ref phrasing-tags))
       'strong (->hiccup-model :strong (map elem-ref phrasing-tags))
       'sub (->hiccup-model :sub (map elem-ref phrasing-tags))
       'sup (->hiccup-model :sup (map elem-ref phrasing-tags))
       'time (->hiccup-model :time
                             (h/with-entries
                               global-attributes
                               [:datetime (h/fn string?)])
                             (map elem-ref phrasing-tags))
       'u (->hiccup-model :u (map elem-ref phrasing-tags))
       'var (->hiccup-model :var (map elem-ref phrasing-tags))
       'wbr (h/val [:wbr])
       ;; then other text
       'blockquote (->hiccup-model :blockquote
                                   (h/with-optional-entries
                                     global-attributes
                                     [:cite (h/fn string?)])
                                   (map elem-ref flow-tags))
       'dl (h/in-vector
            (h/cat
             [:tag (h/val :dl)]
             [:attributes (h/? global-attributes)]
             [:contents
              (h/*
               (h/cat [:term
                       (h/not-inlined
                        (->hiccup-model
                         :dt
                         (map elem-ref (set/difference flow-tags sectioning-tags))))]
                      [:details
                       (h/not-inlined (->hiccup-model
                                       :dd
                                       (map elem-ref flow-tags)))]))]))
       'div (->hiccup-model :div (map elem-ref flow-tags))
       'figure
       (h/in-vector
        (h/cat
         [:tag (h/val :figure)]
         [:attributes (h/? global-attributes)]
         [:contents
          (h/alt
           [:caption-first
            (h/cat
             [:figcaption
              (-> :figcaption
                  (->hiccup-model (map elem-ref flow-tags))
                  h/not-inlined)]
             [:rest (h/*
                     (h/not-inlined
                      (apply h/alt
                             atomic-element
                             (map elem-ref flow-tags))))])]
           [:caption-last
            (h/cat
             [:rest (h/*
                     (h/not-inlined
                      (apply h/alt
                             atomic-element
                             (map elem-ref flow-tags))))]
             [:figcaption
              (-> :figcaption
                  (->hiccup-model (map elem-ref flow-tags))
                  h/not-inlined)])]
           [:no-caption
            (h/*
             (h/not-inlined
              (apply h/alt
                     atomic-element
                     (map elem-ref flow-tags))))])]))
       'hr (h/in-vector (h/cat [:tag (h/val :hr)] [:attributes (h/? global-attributes)]))
       'ol (->hiccup-model
            :ol (h/with-optional-entries global-attributes
                  [:reversed (h/fn boolean?)]
                  [:start (->constrained-model pos-int? gen/small-integer)]
                  [:type (h/enum #{"a" "A" "i" "I" "1"})])
            (conj (map elem-ref #{:script #_:template})
                  [:li
                   (->hiccup-model
                    :li
                    (h/with-optional-entries global-attributes
                      [:value (->constrained-model pos-int? gen/small-integer)])
                    (map elem-ref flow-tags))]))
       'ul (->hiccup-model :ul
                           (conj (map elem-ref #{:script #_:template})
                                 [:li (->hiccup-model :li (map elem-ref flow-tags))]))
       'p (->hiccup-model :p global-attributes (map elem-ref phrasing-tags))
       'pre (->hiccup-model :pre global-attributes (map elem-ref phrasing-tags))
       ;; sectioning
       'address (->hiccup-model
                 :address
                 (map elem-ref (set/difference flow-tags
                                               sectioning-tags
                                               #{:h1 :h2 :h3 :h4 :h5 :h6
                                                 :header :footer})))
       'article (->hiccup-model :article (map elem-ref flow-tags))
       'aside (->hiccup-model :aside (map elem-ref flow-tags))
       'footer (->hiccup-model :footer (map elem-ref (set/difference
                                                      flow-tags
                                                      #{:header :footer})))
       'header (->hiccup-model :header (map elem-ref (set/difference
                                                      flow-tags
                                                      #{:header :footer})))
       'h1 (->hiccup-model :h1 (map elem-ref phrasing-tags))
       'h2 (->hiccup-model :h2 (map elem-ref phrasing-tags))
       'h3 (->hiccup-model :h3 (map elem-ref phrasing-tags))
       'h4 (->hiccup-model :h4 (map elem-ref phrasing-tags))
       'h5 (->hiccup-model :h5 (map elem-ref phrasing-tags))
       'h6 (->hiccup-model :h6 (map elem-ref phrasing-tags))
       'main (->hiccup-model :main (map elem-ref flow-tags))
       'nav (->hiccup-model :nav (map elem-ref flow-tags))
       'section (->hiccup-model :section (map elem-ref flow-tags))
       ;; then other stuff
       'script (h/in-vector
                (h/cat [:tag (h/val :script)]
                       [:attributes
                        (h/with-optional-entries
                          global-attributes
                          [:async (h/val true)]
                          [:crossorigin (h/fn string?)]
                          [:defer (h/val true)]
                          [:integrity (h/fn string?)]
                          [:nomodule (h/val true)]
                          [:referrerpolicy (h/enum #{"no-referrer"
                                                     "no-referrer-when-downgrade"
                                                     "origin"
                                                     "origin-when-cross-origin"
                                                     "same-origin"
                                                     "strict-origin"
                                                     "strict-origin-when-cross-origin"
                                                     ""})]
                          [:src url]
                          [:type (h/fn string?)])]
                       [:content (h/? (h/fn string?))]))
      'table (h/let ['tr
                     (h/not-inlined
                      (h/in-vector
                       (h/cat
                        [:tag (h/val :tr)]
                        [:rowheader
                         (h/? (->hiccup-model
                               :th
                               (h/with-optional-entries
                                 global-attributes
                                 [:abbr (h/fn string?)]
                                 [:colspan (->constrained-model pos-int? gen/small-integer)]
                                 [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                                 [:headers (h/fn string?)]
                                 [:scope (h/enum #{"row" "col" "rowgroup" "colgroup" "auto"})])
                               (map elem-ref (set/difference flow-tags sectioning-tags heading-tags #{:table :footer :header}))))]
                        [:rowdata
                         (h/* (->hiccup-model
                               :td
                               (h/with-optional-entries
                                 global-attributes
                                 [:colspan (->constrained-model pos-int? gen/small-integer)]
                                 [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                                 [:headers (h/fn string?)])
                               (map elem-ref flow-tags)))])))]
               (h/in-vector
                (h/cat
                 [:tag (h/val :table)]
                 [:caption (h/? (->hiccup-model :caption (map elem-ref flow-tags)))]
                 [:colgroups (h/* (->hiccup-model
                                   :colgroup
                                   [(->hiccup-model
                                     :col
                                     (h/with-optional-entries
                                       global-attributes
                                       [:span (h/fn pos-int?)])
                                     :empty)]))]
                 [:header (h/? (->hiccup-model :thead [(h/ref 'tr)]))]
                 [:contents
                  (h/alt
                   [:body (->hiccup-model :tbody [(h/ref 'tr)])]
                   [:rows (h/+ (h/ref 'tr))])]
                 [:footer (h/? (->hiccup-model :tfoot [(h/ref 'tr)]))])))]
      (h/alt
       [:a (h/ref 'a)]
       [:abbr (h/ref 'abbr)]
       [:article (h/ref 'article)]
       [:aside (h/ref 'aside)]
       #_[:audio (h/ref 'audio)]
       [:b (h/ref 'b)]
       [:bdo (h/ref 'bdo)]
       [:bdi (h/ref 'bdi)]
       [:blockquote (h/ref 'blockquote)]
       [:br (h/ref 'br)]
       #_[:button (h/ref 'button)]
       #_[:canvas (h/ref 'canvas)]
       [:cite (h/ref 'cite)]
       [:code (h/ref 'code)]
       [:data (h/ref 'data)]
       #_[:datalist (h/ref 'datalist)]
       [:del (h/ref 'del)]
       #_[:details (h/ref 'details)]
       [:dfn (h/ref 'dfn)]
       [:div (h/ref 'div)]
       [:dl (h/ref 'dl)]
       [:em (h/ref 'em)]
       #_[:embed (h/ref 'embed)]
       #_[:fieldset (h/ref 'fieldset)]
       [:figure (h/ref 'figure)]
       [:footer (h/ref 'footer)]
       #_[:form (h/ref 'form)]
       [:h1 (h/ref 'h1)] [:h2 (h/ref 'h2)] [:h3 (h/ref 'h3)]
       [:h4 (h/ref 'h4)] [:h5 (h/ref 'h5)] [:h6 (h/ref 'h6)]
       [:header (h/ref 'header)]
       [:hr (h/ref 'hr)]
       [:i (h/ref 'i)]
       #_[:iframe (h/ref 'iframe)]
       [:img (h/ref 'img)]
       #_[:input (h/ref 'input)]
       [:ins (h/ref 'ins)]
       [:kbd (h/ref 'kbd)]
       #_[:label (h/ref 'label)]
       [:main (h/ref 'main)]
       #_[:map (h/ref 'map)]
       #_[:mark (h/ref 'mark)]
       #_[:math (h/ref 'math)]
       #_[:menu (h/ref 'menu)]
       #_[:meter (h/ref 'meter)]
       [:nav (h/ref 'nav)]
       #_[:noscript (h/ref 'noscript)]
       #_[:object (h/ref 'object)]
       [:ol (h/ref 'ol)]
       #_[:output (h/ref 'output)]
       [:p (h/ref 'p)]
       #_[:picture (h/ref 'picture)]
       [:pre (h/ref 'pre)]
       #_[:progress (h/ref 'progress)]
       [:q (h/ref 'q)]
       [:s (h/ref 's)]
       [:samp (h/ref 'samp)]
       [:script (h/ref 'script)]
       [:section (h/ref 'section)]
       #_[:select (h/ref 'select)]
       [:span (h/ref 'span)]
       [:strong (h/ref 'strong)]
       [:sub (h/ref 'sub)]
       [:sup (h/ref 'sup)]
       #_[:svg (h/ref 'svg)]
       [:table (h/ref 'table)]
       #_[:template (h/ref 'template)]
       #_[:textarea (h/ref 'textarea)]
       [:time (h/ref 'time)]
       [:ul (h/ref 'ul)]
       [:var (h/ref 'var)]
       [:wbr (h/ref 'wbr)])))

(comment
  (def colgroup-model
    (h/not-inlined
     (h/*
      (h/in-vector
       (h/cat [:tag (h/val :colgroup)]
              [:cols
               (h/not-inlined (h/*
                               (->hiccup-model
                                :col
                                (h/with-optional-entries
                                  global-attributes
                                  [:span (h/fn pos-int?)])
                                :empty)))])))))

  (def colgroup-model-2
    (h/*
      (h/in-vector
       (h/cat [:tag (h/val :colgroup)]
              [:cols
               (h/not-inlined (h/*
                               (->hiccup-model
                                :col
                                (h/with-optional-entries
                                  global-attributes
                                  [:span (h/fn pos-int?)])
                                :empty)))]))))

  (valid? colgroup-model [:colgroup [:col]])
  (valid? colgroup-model [:colgroup [:col]])

  )

(def p
  (h/let ['em (->child-model :em)
          'strong (->child-model :strong)
          'a (->child-model :a)
          'code (->child-model :code)
          'span (->child-model :span)
          'ol (->child-model :ol)
          'ul (->child-model :ul)
          'li (->child-model :li)
          'blockquote (->child-model :blockquote)
          'h1 (->child-model :h1)
          'h2 (->child-model :h1)
          'h3 (->child-model :h3)
          'h4 (->child-model :h4)
          'h5 (->child-model :h5)
          'h6 (->child-model :h6)
          'p (->child-model :p)]
    (h/in-vector
     (h/cat
      [:tag (h/val :p)]
      [:attributes (h/? attr-map)]
      [:contents
       (h/*
        (h/not-inlined
         (apply h/alt
                [:atomic-element atomic-element]
                (map elem-ref (:p doc-tree)))))]))))

(def subform
  (h/let ['em (->child-model :em)
          'strong (->child-model :strong)
          'a (->child-model :a)
          'pre (->child-model :pre)
          'code (->child-model :code)
          'script (->child-model :script)
          'span (->child-model :span)
          'ol (->child-model :ol)
          'ul (->child-model :ul)
          'li (->child-model :li)
          'blockquote (->child-model :blockquote)
          'h1 (->child-model :h1)
          'h2 (->child-model :h1)
          'h3 (->child-model :h3)
          'h4 (->child-model :h4)
          'h5 (->child-model :h5)
          'h6 (->child-model :h6)
          'p (->child-model :p)
          'div (->child-model :div)]
    (apply h/alt
           (map elem-ref (:section doc-tree)))))

(def grid
  (h/let ['em (->child-model :em)
          'strong (->child-model :strong)
          'a (->child-model :a)
          'pre (->child-model :pre)
          'code (->child-model :code)
          'script (->child-model :script)
          'span (->child-model :span)
          'ol (->child-model :ol)
          'ul (->child-model :ul)
          'li (->child-model :li)
          'blockquote (->child-model :blockquote)
          'h1 (->child-model :h1)
          'h2 (->child-model :h1)
          'h3 (->child-model :h3)
          'h4 (->child-model :h4)
          'h5 (->child-model :h5)
          'h6 (->child-model :h6)
          'p (->child-model :p)
          'div (->child-model :div)
          'grid-cell
          (h/in-vector
           (h/cat
            [:tag (h/val :section)]
            [:attributes
             (h/? (h/with-entries attr-map [:span raster-span]))]
            [:contents
             (h/* (h/not-inlined
                   (apply h/alt
                          [:grid (h/ref 'grid)]
                          [:atomic-element atomic-element]
                          (map elem-ref (:section doc-tree)))))]))
          'grid
          (h/in-vector
           (h/cat [:tag (h/val :r-grid)]
                  [:attributes
                   (h/with-entries attr-map
                     [:columns (->constrained-model #(< 0 % 33) gen/nat 200)])]
                  [:contents (h/* (h/not-inlined (h/ref 'grid-cell)))]))]
    (h/ref 'grid)))

(def grid-cell
  (h/let ['em (->child-model :em)
          'strong (->child-model :strong)
          'a (->child-model :a)
          'pre (->child-model :pre)
          'code (->child-model :code)
          'script (->child-model :script)
          'span (->child-model :span)
          'ol (->child-model :ol)
          'ul (->child-model :ul)
          'li (->child-model :li)
          'blockquote (->child-model :blockquote)
          'h1 (->child-model :h1)
          'h2 (->child-model :h1)
          'h3 (->child-model :h3)
          'h4 (->child-model :h4)
          'h5 (->child-model :h5)
          'h6 (->child-model :h6)
          'p (->child-model :p)
          'div (->child-model :div)
          'grid-cell
          (h/in-vector
           (h/cat
            [:tag (h/val :section)]
            [:attributes
             (h/? (h/with-entries attr-map [:span raster-span]))]
            [:contents
             (h/* (h/not-inlined
                   (apply h/alt
                          [:grid (h/ref 'grid)]
                          [:atomic-element atomic-element]
                          (map elem-ref (:section doc-tree)))))]))
          'grid
          (h/in-vector
           (h/cat [:tag (h/val :r-grid)]
                  [:attributes
                   (h/with-entries attr-map
                     [:columns (->constrained-model #(< 0 % 33) gen/nat 200)])]
                  [:contents (h/* (h/not-inlined (h/ref 'grid-cell)))]))]
    (h/ref 'grid-cell)))

; example before

(def sample-form '("first paragraph\n\nsecond paragraph"
                   [:r-grid [:section "first cell line\n\nsecond cell line"]
                    [:section "another cell"]]
                   "third paragraph"))

; example after
(def sample-parsed-form
  '([:section {:span "row"} "first paragraph"]
    [:section {:span "row"} "second paragraph"]
    [:r-grid
     [:section [:p "first cell line"] [:p "second cell line"]]
     [:section "another cell"]]
    [:section {:span "row"} "third paragraph"]))

(def full-row [:section {:span "row"}])

(defn in-form? [e] (and (vector? e)
                        (contains? (:section doc-tree) (first e))))
;; in-form is just the subform model
(defn not-in-form? [e] (and (vector? e)
                            (not (contains? (:section doc-tree) (first e)))))

;; orphan? is just h/alt on subform or atomic elements
(defn orphan? [e] (or (string? e) (in-form? e)))
(def orphan (h/alt [:atomic atomic-element]
                   [:subform subform]))

;; already-tokenized is just the paragraph model
(defn already-tokenized? [e]
  (or
   (map? e)
   (and (vector? e) (= (first e) :p))))

(def tokenized
  (h/alt [:attr-map attr-map]
         [:section grid-cell]
         [:p p]))

(defn group-orphans
  ([encloser tokenized? items]
   (let [grouper
         (fn [s]
           (mapv
            (fn [i] (cond
                      (valid? orphan (first i))
                      (into encloser (apply vector i))
                      (tokenized? (first i)) (first i)
                      :else
                      i))
            (partition-by #(cond
                             (valid? orphan %) :orphan
                             (tokenized? %) :tokenized) s)))]
     (if (keyword? (first items))
       (apply vector (first items) (grouper (rest items)))
       (grouper items))))
  ([encloser tokenized?] (fn [items] (group-orphans encloser tokenized? items))))

(comment

  (valid? tokenized [:section [:em "non-orphan text"]])

  (group-orphans [:p]
                 #(valid? tokenized %)
                 ["orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:p]
                 #(valid? tokenized %)
                 [:r-grid "orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:section {:span "row"}]
                 #(valid? tokenized %)
                 [:r-grid "orphan text"
                  [:em "with emphasis added"]
                  [:section "non-orphan text"]]))

(defn section? [i]
  (and (vector? i) (= :section (first i))))

(defn get-orphans
  ([location elem]
   (loop [loc location]
     (let [parent-type
           (if (-> loc zip/up)
             (-> loc (zip/up) (zip/node) first)
             :r-grid)]
       (cond
         (zip/end? loc) loc             ; are we at the end?
         (valid? grid-cell (zip/node loc))       ; has this node been processed?
         (recur (zip/next loc))         ; if yes, continue
         (= parent-type :section)        ; are we not in a context with orphans?
         (recur (zip/next loc))         ; then continue
         (and (zip/branch? loc)
              (some
               #(valid? orphan %)
               (zip/children loc))) ; are there orphans in the child node?
         (recur (zip/edit
                 loc
                 (group-orphans elem
                                #(valid? tokenized %))))
         :else (recur (zip/next loc))))))
  ([location] (get-orphans location full-row)))

(comment

  (def orphan-grid-zipper
    (zip/zipper not-in-form? identity (fn [_ c] c)
                (first respatialized.transform-test/orphan-trees)))

  (-> orphan-grid-zipper zip/next zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/next zip/node)
  ;; defining a custom predicate for a zipper lets you ignore inline elements and
  ;; only recurse into the subsequences that actually define child relationships

  (-> orphan-grid-zipper zip/next zip/next zip/up zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/next zip/next zip/next zip/up zip/node)
  ;; with a zipper you can always access the enclosing element
  ;; to perform contextual operations by matching on the parent element type


  (partition-by #(and (string? %) (some? (re-find #"\n\n" %))) [1 2 3 3 "a" "b" "c\n\nd" 4 3])
  ;; a standard library function that may be useful for paragraph splitting


  (zip/node (get-orphans respatialized.transform-test/orphan-zip))

  ;; this doesn't work because it partitions the entire enclosing sequence
  ;; when actually we just want to partition the orphans
  )

(comment
  (map #(if (string? %) (clojure.string/split % #"\n\n") %) [:a "b\n\nc" :d "e" "f"])
  ;; nope, needs to put all the split ones into the enclosing sequence

  (apply vector (reduce (fn [acc next]
                          (if (string? next)
                            (concat acc (clojure.string/split next #"\n\n"))
                            (concat acc (list next))))
                        []
                        (apply list [:a "b\n\nc" :d "e" "f"]))))

;; group the orphans, then split the string

(defn line-break? [i]
  (and (string? i) (some? (re-find #"\n\n" i))))

(comment
  (def sample-cdl
    (ftree/counted-double-list :section
                               "some text" "more text\n\nwith linebreak"
                               [:em "and emphasis"]
                               "another \n\n linebreak"))

  (partition-by line-break? sample-cdl)

  (reduce
   (fn [acc next]
     (let [ix (count acc)]
       (if (line-break? next) ...
           ())))
   (ftree/counted-double-list)))

(defn collect-inline [vec]
  (let [cdl (ftree/counted-double-list vec)])
  (reduce
   (fn [])))

(defn para? [i] (and (vector? i) (= :p (first i))))

(defn in-para? [i] (and (vector? i)
                        (contains? (:p doc-tree) (first i))))

(def subparagraph
  (h/let ['em (->child-model :em)
          'strong (->child-model :strong)
          'a (->child-model :a)
          'p (->child-model :p)
          'code (->child-model :code)
          'span (->child-model :span)
          'blockquote (->child-model :blockquote)
          'ol (->child-model :ol)
          'ul (->child-model :ul)
          'li (->child-model :li)
          'h1 (->child-model :h1)
          'h2 (->child-model :h1)
          'h3 (->child-model :h3)
          'h4 (->child-model :h4)
          'h5 (->child-model :h5)
          'h6 (->child-model :h6)]
    (apply h/alt
           [:atomic-element atomic-element]
           (map elem-ref (disj  (:p doc-tree) :p)))))

(defn detect-paragraphs
  "For each string in the element split it by the given regex, and insert the result into the original element. Leaves sub-elements as is and inserts them into the preceding paragraph."
  ([seq re]
   (let [v? (vector? seq)
         r (loop [s (apply ftree/counted-double-list seq)
                  final (ftree/counted-double-list)]
             (if (empty? s) final       ; base case
                 (let [h (first s) t (rest s)
                       current-elem (last final)]
                   (cond
                     (and (string? h) (re-matches #"\s+" h))
                     (recur t final)  ; skip whitespace
                     (and (string? h)
                          (some? (re-find re h)))
                     (let [[hh & tt] (str/split h re)
                           rest (map (fn [i] [:p i]) tt)]
                       (cond
                         (or (empty? hh) (re-matches #"\s+" hh)) (recur (concat rest t) final)
                         (valid? p current-elem)
                         (recur (concat rest t)
                                (conj (first (ft-split-at final (- (count final) 1)))
                                      (conj current-elem hh)))
                         :else (recur (concat rest t) (conj final [:p hh]))))
                     (valid? subparagraph h)
                     (if (valid? p current-elem)
                       (recur t
                              (conj (first (ft-split-at final (- (count final) 1)))
                                    (conj current-elem h)))
                       (recur t (conj final [:p h])))
                     :else
                     (recur t (conj final h))))))]
     (if v? (apply vector r)  r)))
  ([re] (fn [seq] (detect-paragraphs seq re)))
  ([] (detect-paragraphs #"\n\n")))

(comment
  (detect-paragraphs [:section "some\n\ntext" [:em "with emphasis"]]
                     #"\n\n"))

(defn split-strings
  "Split any strings in the sequence by the regex and insert them into the sequence. Ignore any non-strings."
  ([vec re]
   (apply
    vector
    (reduce
     (fn [acc next]
       (if (string? next)
         (concat acc (clojure.string/split next re))
         (concat acc (list next))))
     []
     (apply list vec))))
  ([re] (fn [v] (split-strings v re))))

(defn para-tokenized? [e]
  (or (map? e) (keyword? e) (para? e)))

(def enclosed
  (h/alt [:attr attr-map]
         [:form-name (h/fn keyword?)]
         [:p p]))

(defn tokenize-paragraphs [loc]
  (cond
    (zip/end? loc) loc                  ; are we at the end?
    (valid? p (zip/node loc))           ; has this node been processed?
    (recur (zip/next loc))              ; if yes, continue
    (and (zip/branch? loc)
         (some string? (zip/children loc)) ; are there strings in the child node?
         (not (every? #(valid? enclosed %) (zip/children loc))))
    (recur (zip/edit loc (detect-paragraphs #"\n\n")))
    :else (recur (zip/next loc))))

(defn- make
  "Return the node, skipping attribute maps if present."
  {:license {:source "https://github.com/davidsantiago/hickory"
             :type "Eclipse Public License, v1.0"}}
  [node children]
  (if (vector? node)
    (if (map? (second node))
      (into (subvec node 0 2) children)
      (apply vector (first node) children))
    children))

(defn- children
  "Return the node's children, skipping attribute maps if present."
  {:license {:source "https://github.com/davidsantiago/hickory"
             :type "Eclipse Public License, v1.0"}}
  [node]
  (if (vector? node)
    (if (map? (second node))
      (subvec node 2)
      (subvec node 1))
    node))

(defn form-zipper [f]
  (zip/zipper not-in-form? children make f))

(defn process-text
  ([form elem]
   (-> form
       form-zipper
       (get-orphans elem)
       zip/node
       form-zipper
       tokenize-paragraphs
       zip/node))
  ([form] (process-text form full-row)))

(comment

  (split-strings [:r-grid {:span "row"} "some text\n\nwith line break"] #"\n\n")

  (-> respatialized.transform-test/orphan-zip-2
      get-orphans
      zip/node
      form-zipper
      tokenize-paragraphs
      zip/node))

(defn section? [elem]
  (and (vector? elem) (= :section (first elem))))

(defn next? [elem]
  (= :next (first elem)))

;; seems like this may be a problem if I want to nest further
;; but the actual structure implied by the format I've chosen
;; is pretty flat - at most three levels deep:
;; [:article [:section #_ :next [:div "text"]]]

(defn process-nexts [nexts]
  (loop [[h n & rest] nexts
         res []]
    (if (empty? rest) ; base case
      (condp = [(nil? h) (nil? n)]
        [true true] res
        [false true] (conj res h)
        [false false] (conj res h n))
      (cond
        (= :next n)
        (recur (apply conj [:next] rest)
               (conj res h))
        (= :next h)
        (recur
         (drop-while #(not= % :next) rest)
         (conj res (apply conj n (take-while #(not= % :next) rest))))
        :else
        (recur
         (drop-while #(not= % :next) rest)
         (apply conj res h n (take-while #(not= % :next) rest)))))))

;; (def process-contents
;;   (comp
;;    (partition-by next?))
;;   (cond
;;     (next? (first contents))
;;     (let [[_ into-elem & rest] contents]
;;       (apply conj into-elem rest))
;;     :else contents))

(defn front-matter? [_] false)

(defn process-chunk [chunk]
  (cond
    (front-matter? chunk) chunk         ; front matter gets left as-is
    (section? (first (first chunk)))
    (let [[[s] content] chunk]
      (apply conj s
             (detect-paragraphs (process-nexts content) #"\n\n")))
    :else
    (let [[content] chunk]
      (apply conj
             [:section]
             (detect-paragraphs (process-nexts content) #"\n\n")))))

(def sectionize-contents
  (comp
   (partition-by section?)
   (partition-all 2)
   (map process-chunk)))

(comment
  (def unsectioned
    [:article
     [:p "Some text"]
     [:p "Some more text"]])

  (into [:article] sectionize (rest unsectioned))

  (def sectioned
    [:article
     [:section]
     [:p "text"]
     [:q "a quote"]
     [:section]
     [:p "more text"]])
  (into [:article] sectionize  (rest sectioned))

  (def partially-sectioned
    [:article
     [:p "text"]
     [:q "a quote"]
     [:section]
     [:p "more text"]])

  (into [:article] sectionize (rest partially-sectioned))

  (def actual-doc (-> "./content/information-cocoon.html.ct" slurp (respatialized.parse/parse-eval [:article])))

  (respatialized.build/load-deps)

  (def actual-doc-2 (-> "./content/reifying-filter-bubble-2.html.ct"
                        slurp
                        (respatialized.parse/parse-eval [:article])))

  (into [:article] sectionize-contents (rest actual-doc))

  (into [] sectionize-contents actual-doc-2))
