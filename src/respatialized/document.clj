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
    :code :data #_:datalist :dfn :del :em #_:embed :i #_:iframe :img
    :ins #_:input :kbd #_:label :mark #_:math #_:meter #_:noscript
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

(def string-gen (-> (h/fn string?) (h/with-test-check-gen gen/string)))
(def boolean-gen (-> (h/fn boolean?) (h/with-test-check-gen gen/boolean)))
(def int-gen (-> (h/fn int?) (h/with-test-check-gen gen/small-integer)))

(def global-attributes
  "MDN list of global HTML attributes as minimallist spec"
  (h/with-optional-entries
    (h/map)
    [:class string-gen]
    [:contenteditable (h/enum #{"true" "false" ""})]
    [:dir (h/enum #{"ltr" "rtl" "auto"})]
    [:hidden boolean-gen]
    [:id string-gen]
    [:itemid string-gen]
    [:itemprop string-gen]
    [:itemref string-gen]
    [:itemscope boolean-gen]
    [:itemtype (regex->model external-link-pattern)]
    [:lang (h/enum #{"en"})]
    [:tabindex int-gen]
    [:title string-gen]))

;; (def )


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
                                            [:text string-gen])])]
      (h/ref 'hiccup))))

(def atomic-element
  (h/alt
   [:boolean boolean-gen]
   [:decimal (h/with-test-check-gen (h/fn float?) gen/double)]
   [:int int-gen]
   [:text string-gen]))

(defn quote-kw [kw] `~(symbol kw))
(defn elem-ref [e] [e (h/ref (quote-kw e))])

(defn has-required?
  "Checks to see if at least one entry in the given map model has required keys"
  [model]
  (and
   (= :map (:type model))
   (some #(nil? (get % :optional)) (:entries model))))

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
                   [:character string-gen]
                   [:pronunciation (h/in-vector
                                    (h/cat [:tag (h/val :rt)]
                                           [:text string-gen]))])))]))])

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
                               [:fragment string-gen])])
                           (h/with-optional-entries
                             [:download string-gen]
                             [:rel string-gen]
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
                            [:value string-gen])
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
                                          [:alt string-gen]
                                          [:sizes string-gen]
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
                            [:datetime string-gen])
                          (map elem-ref phrasing-tags))
    'u (->hiccup-model :u (map elem-ref phrasing-tags))
    'var (->hiccup-model :var (map elem-ref phrasing-tags))
    'wbr (h/val [:wbr])
       ;; then other text
    'blockquote (->hiccup-model :blockquote
                                (h/with-optional-entries
                                  global-attributes
                                  [:cite string-gen])
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
               [:reversed boolean-gen]
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
                       [:crossorigin string-gen]
                       [:defer (h/val true)]
                       [:integrity string-gen]
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
                       [:type string-gen])]
                    [:content (h/? string-gen)]))
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
                               [:abbr string-gen]
                               [:colspan (->constrained-model pos-int? gen/small-integer)]
                               [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                               [:headers string-gen]
                               [:scope (h/enum #{"row" "col" "rowgroup" "colgroup" "auto"})])
                             (map elem-ref (set/difference flow-tags sectioning-tags heading-tags #{:table :footer :header}))))]
                      [:rowdata
                       (h/* (->hiccup-model
                             :td
                             (h/with-optional-entries
                               global-attributes
                               [:colspan (->constrained-model pos-int? gen/small-integer)]
                               [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                               [:headers string-gen])
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
                                     [:span int-gen])
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

(defn ->element-model
  "Produce the model specific to the given element from the global elements model."
  [elem]
  (merge (select-keys elements [:type :bindings])
         {:body
          {:type :ref :key (quote-kw elem)}}))



;; "content is palpable when it's neither empty or hidden;
;; it is content that is rendered and is substantive.
;; Elements whose model is flow content or phrasing content
;; should have at least one node which is palpable."

(defn palpable? [c]
  (some? (some
          #(valid? atomic-element %)
          (tree-seq #(and (vector? %) (keyword? (first %))) rest c))))

(def flow-content
  (apply h/alt
         (map #(h/with-condition (->element-model %)
                 (h/fn palpable?)) flow-tags)))

(def phrasing-content
  (apply h/alt
         atomic-element
         (map
          #(h/with-condition (->element-model %)
             (h/fn palpable?)) phrasing-tags)))


;; (def palpable)

(def article-outline
  "A model for an article with a clearly delineated structure"
  (h/in-vector
   (h/cat
    [:tag (h/val :article)]
    [:article-header (h/? (->element-model :header))]
    [:sections (h/+ (->element-model :section))]
    [:article-footer (h/? (->element-model :footer))])))

(comment
                                        ; example before

  (def sample-form '("first paragraph\n\nsecond paragraph"
                     [:section
                      {:class "grid"}
                      [:div {:class "1col"} "first coll line\n\nsecond col line"]
                      [:div {:class "1col"} "another cell"]]
                     "third paragraph"))

                                        ; example after
  (def sample-parsed-form
    '([:section
       [:p "first paragraph"]
       [:p "second paragraph"]]
      [:section {:class "grid"}
       [:div {:class "1col"} [:p "first cell line"] [:p "second cell line"]]
       [:div {:class "1col"} [:p "another cell"]]
       "third paragraph"])))

(defn not-in-form? [e] (and (vector? e)
                            (not (contains? phrasing-tags (first e)))))

;; already-tokenized is just the paragraph model
;; but it may be difficult to use it here
(defn already-tokenized? [e]
  (or
   (map? e)
   (and (vector? e) (= (first e) :p))))











;; group the orphans, then split the string


(defn para? [i] (and (vector? i) (= :p (first i))))
(defn in-para? [i] (or (valid? atomic-element i)
                       (and (vector? i) (phrasing-tags (first i)))))

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
                         (valid? (->element-model :p) current-elem)
                         (recur (concat rest t)
                                (conj (first (ft-split-at final (- (count final) 1)))
                                      (conj current-elem hh)))
                         :else (recur (concat rest t) (conj final [:p hh]))))
                     (valid? phrasing-content h)
                     (if (valid? (->element-model :p) current-elem)
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

(defn section? [i]
  (and (vector? i) (= :section (first i))))

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
