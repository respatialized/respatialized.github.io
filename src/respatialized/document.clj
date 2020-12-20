(ns respatialized.document
  "Namespace for document processing."
  (:require
   [clojure.string :as str]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer [counted-double-list ft-split-at ft-concat]]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [minimallist.core :as m :refer [valid? describe]]
   [minimallist.helper :as h]
   [minimallist.generator :as mg]))

(def block-level-tags
  "MDN list of block-level HTML element tags"
  #{:address :article :aside :blockquote
    :details :dialog :dd :div :dl :dt
    :fieldset :figcaption :figure :footer
    :form :h1 :h2 :h3 :h4 :h5 :h6 :header
    :hr :li :main :nav :ol :p :pre :section
    :table :ul})

(def inline-tags
  "MDN list of inline HTML element tags"
  #{:a :abbr :b :bdi :bdo :br :button
    :canvas :cite :code :data :datalist
    :del :dfn :em :embed :i :iframe :img
    :input :ins :kbd :label :mark :meter
    :noscript :object :output :picture
    :progress :q :ruby :s :samp :script
    :select :slot :small :span :strong
    :sub :sup :svg :template :time :u
    :tt :var :video :wbr})

(def metadata-tags
  "MDN list of metadata content element tags"
  #{:base :link :meta :noscript :script
    :style :title})

(def flow-tags
  "MDN list of flow content element tags"
  #{:a :abbr :aside :audio :b :bdo :bdi
    :blockquote :br :button :canvas :cite
    :code :data :datalist :del :details :dfn
    :div :dl :em :embed :fieldset :figure
    :footer :form :h1 :h2 :h3 :h4 :h5 :h6
    :header :hr :i :iframe :img :input :ins
    :kbd :label :main :map :mark :math :menu
    :meter :nav :noscript :object :ol :output
    :p :picture :pre :progress :q :ruby :s
    :samp :script :section :select :small
    :span :strong :sub :sup :svg :table
    :template :textarea :time :ul :var :video
    :wbr})

(def sectioning-tags
  "MDN list of sectioning content element tags"
  #{:article :aside :nav :section})

(def heading-tags
  "MDN list of heading content element tags"
  #{:h1 :h2 :h3 :h4 :h5 :h6})

(def phrasing-tags
  "MDN list of phrasing content element tags"
  #{:abbr :audio :b :bdo :br :button :canvas :cite
    :kbd :label :mark :math :meter :noscript
    :object :output :picture :progress :q :ruby
    :samp :script :select :small :span :strong :sub
    :sup :svg :textarea :time :var :video :wbr})

(def phrasing-subtags
  "MDN list of tags that are phrasing content when they contain only phrasing content."
  #{:a :del :ins :map})

(def embedded-tags
  "MDN list of embedded content element tags"
  #{:audio :canvas :embed :iframe :img :math
    :object :picture :svg :video})

(def interactive-tags
  "MDN list of interactive content element tags"
  #{:a :button :details :embed :iframe :label
    :select :textarea})

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
   [:text (-> (h/fn string?) (h/with-test-check-gen gen/string))]
   [:image img]
   [:br (h/val [:br])]
   [:hr (h/val [:hr])]))

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
                     #"\n\n")

  )

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
      (apply conj s (process-nexts content)))
    :else
    (let [[content] chunk]
      (apply conj [:section] (process-nexts content)))))

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

  (into [:article] sectionize (rest actual-doc))

  )
