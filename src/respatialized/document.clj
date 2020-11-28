(ns respatialized.document
  "Namespace for document processing."
  (:require
   [clojure.string :as str]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer [counted-double-list ft-split-at ft-concat]]
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [minimallist.core :as m :refer [valid? describe]]
   [minimallist.helper :as h]
   [minimallist.generator :as mg]))

(def doc-tree
  {:r-cell #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :pre :span :p :div}
   :div #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :pre :span :p}
   :p #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a :blockquote :span :p}
   :pre #{:em :span :a}
   :em #{:code :span :a}
   :a #{:em :span}
   :code #{:em :a :span}
   :blockquote #{}
   :ol #{:li}
   :ul #{:li}
   :li #{:code :em :span :a :blockquote}
   :h1 #{:code :em :span :a}
   :h2 #{:code :em :span :a}
   :h3 #{:code :em :span :a}
   :h4 #{:code :em :span :a}
   :h5 #{:code :em :span :a}
   :h6 #{:code :em :span :a}
   :span #{}})

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

(def external-link-pattern #"https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
(def internal-link-pattern #"/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(defn regex->model [re]
  (-> (h/fn #(re-matches re %))
      (h/with-test-check-gen
        (gen'/string-from-regex re))))

(def url
  (h/alt
   [:external (regex->model external-link-pattern)]
   [:internal (regex->model internal-link-pattern)]))

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

(def image
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
   [:image image]
   [:br (h/val [:br])]
   [:hr (h/val [:hr])]))

(defn quote-kw [kw] `~(symbol kw))
(defn elem-ref [e] [e (h/ref (quote-kw e))])

(defn get-child-model [elem]
  (h/in-vector
   (h/cat
    [:tag (h/val elem)]
    [:attributes (h/? attr-map)]
    [:contents (h/* (h/not-inlined
                     (apply h/alt
                            atomic-element
                            (map elem-ref
                                 (get doc-tree elem)))))])))

(def raster-span
  (h/alt
   [:row (h/val "row")]
   [:range (regex->model #"\d\-\d")]
   [:offset (regex->model #"\d\+\d")]
   [:offset-row (regex->model #"\d\.\.")]
   [:cols (->constrained-model #(< 0 (Integer/parseInt %) 33)
                               (gen/fmap str gen/nat) 200)]))

(def p
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'code (get-child-model :code)
          'span (get-child-model :span)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'blockquote (get-child-model :blockquote)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)
          'p (get-child-model :p)]
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
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'pre (get-child-model :pre)
          'code (get-child-model :code)
          'span (get-child-model :span)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'blockquote (get-child-model :blockquote)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)
          'p (get-child-model :p)
          'div (get-child-model :div)]
    (apply h/alt
           (map elem-ref (:r-cell doc-tree)))))

(def grid
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'pre (get-child-model :pre)
          'code (get-child-model :code)
          'span (get-child-model :span)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'blockquote (get-child-model :blockquote)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)
          'p (get-child-model :p)
          'div (get-child-model :div)
          'grid-cell
          (h/in-vector
           (h/cat
            [:tag (h/val :r-cell)]
            [:attributes
             (h/? (h/with-entries attr-map [:span raster-span]))]
            [:contents
             (h/* (h/not-inlined
                   (apply h/alt
                          [:grid (h/ref 'grid)]
                          [:atomic-element atomic-element]
                          (map elem-ref (:r-cell doc-tree)))))]))
          'grid
          (h/in-vector
           (h/cat [:tag (h/val :r-grid)]
                  [:attributes
                   (h/with-entries attr-map
                     [:columns (->constrained-model #(< 0 % 33) gen/nat 200)])]
                  [:contents (h/* (h/not-inlined (h/ref 'grid-cell)))]))]
    (h/ref 'grid)))

(def grid-cell
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'pre (get-child-model :pre)
          'code (get-child-model :code)
          'span (get-child-model :span)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'blockquote (get-child-model :blockquote)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)
          'p (get-child-model :p)
          'div (get-child-model :div)
          'grid-cell
          (h/in-vector
           (h/cat
            [:tag (h/val :r-cell)]
            [:attributes
             (h/? (h/with-entries attr-map [:span raster-span]))]
            [:contents
             (h/* (h/not-inlined
                   (apply h/alt
                          [:grid (h/ref 'grid)]
                          [:atomic-element atomic-element]
                          (map elem-ref (:r-cell doc-tree)))))]))
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
                   [:r-grid [:r-cell "first cell line\n\nsecond cell line"]
                    [:r-cell "another cell"]]
                   "third paragraph"))

; example after
(def sample-parsed-form
  '([:r-cell {:span "row"} "first paragraph"]
    [:r-cell {:span "row"} "second paragraph"]
    [:r-grid
     [:r-cell [:p "first cell line"] [:p "second cell line"]]
     [:r-cell "another cell"]]
    [:r-cell {:span "row"} "third paragraph"]))

(def full-row [:r-cell {:span "row"}])

(defn in-form? [e] (and (vector? e)
                        (contains? (:r-cell doc-tree) (first e))))
;; in-form is just the subform model
(defn not-in-form? [e] (and (vector? e)
                            (not (contains? (:r-cell doc-tree) (first e)))))

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
         [:r-cell grid-cell]
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

  (valid? tokenized [:r-cell [:em "non-orphan text"]])


  (group-orphans [:p]
                 #(valid? tokenized %)
                 ["orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:p]
                 #(valid? tokenized %)
                 [:r-grid "orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:r-cell {:span "row"}]
                 #(valid? tokenized %)
                 [:r-grid "orphan text"
                  [:em "with emphasis added"]
                  [:r-cell "non-orphan text"]]))

(defn r-cell? [i]
  (and (vector? i) (= :r-cell (first i))))

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
         (= parent-type :r-cell)        ; are we not in a context with orphans?
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
    (ftree/counted-double-list :r-cell
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
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'code (get-child-model :code)
          'span (get-child-model :span)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'blockquote (get-child-model :blockquote)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)]
    (apply h/alt
       [:atomic-element atomic-element]
       (map elem-ref (disj (:p doc-tree) :p)))))

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
                     (and (string? h) (some? (re-find re h)))
                     (let [[hh & tt] (str/split h re)
                           rest (map (fn [i] [:p i]) tt)]
                       (cond
                         (empty? hh) (recur (concat rest t) final)
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
  (detect-paragraphs [:r-cell "some\n\ntext" [:em "with emphasis"]]
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

(spec/def ::inline-text
  (spec/and string?
            #(< (count %) 15000)))

(spec/def ::attr-map
  (spec/map-of #{:href :id :title :src :alt :lang
                 :span}
               string?))

(def p-pattern
  (spec/cat
   :type #{:p}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/*
    (spec/or :text string?
             :subform
             (spec/or
              :pre ::pre
              :em ::em
              :a ::a
              :code ::code
              :ol ::ol
              :ul ::ul
              :li ::li
              :header ::header)))))

(spec/def ::p
  (spec/with-gen (spec/and vector? p-pattern)
    #(sgen/fmap vec (spec/gen p-pattern))))

(def a-pattern
  (spec/cat
   :type #{:a}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/*
    (spec/or :text string?
             :subform
             (spec/or
              :em ::em
              :span ::span)))))

(spec/def ::a
  (spec/with-gen (spec/and vector? a-pattern)
    #(sgen/fmap vec (spec/gen a-pattern))))

(def em-pattern
  (spec/cat
   :type #{:em}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/*
    (spec/or :text string?
             :subform
             (spec/or
              :code ::code
              :a ::a
              :span ::span)))))

(spec/def ::em
  (spec/with-gen (spec/and vector? em-pattern)
    #(sgen/fmap vec (spec/gen em-pattern))))

(def pre-pattern
  (spec/cat
   :type #{:pre}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/* (spec/or :text string?
                    :subform
                    (spec/or :code ::code
                             :a ::a
                             :span ::span)))))

(spec/def ::pre
  (spec/with-gen (spec/and vector? pre-pattern)
    #(sgen/fmap vec (spec/gen pre-pattern))))

(def span-pattern
  (spec/cat
   :type #{:span}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/* (spec/or :text string?
                    :subform (spec/or :a ::a
                                      :em ::em)))))

(spec/def ::span
  (spec/with-gen (spec/and vector? span-pattern)
    #(sgen/fmap vec (spec/gen span-pattern))))

(def code-pattern
  (spec/cat
   :type #{:code}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/* (spec/or :text string?
                    :subform (spec/or :em ::em
                                      :a ::a
                                      :span ::span)))))

(spec/def ::code
  (spec/with-gen (spec/and vector? code-pattern)
    #(sgen/fmap vec (spec/gen code-pattern))))

(def ol-pattern
  (spec/cat :type #{:ol}
            :attr-map (spec/? ::attr-map)
            :items (spec/* ::li)))

(spec/def ::ol
  (spec/with-gen (spec/and vector? ol-pattern)
    #(sgen/fmap vec (spec/gen ol-pattern))))

(def ul-pattern
  (spec/cat :type #{:ul}
            :attr-map (spec/? ::attr-map)
            :items (spec/* ::li)))

(spec/def ::ul
  (spec/with-gen (spec/and vector? ul-pattern)
    #(sgen/fmap vec (spec/gen ul-pattern))))

(def li-pattern
  (spec/cat
   :type #{:li}
   :attr-map (spec/? ::attr-map)
   :contents
   (spec/* (spec/or :text string?
                    :subform (spec/or :code ::code
                                      :em ::em
                                      :span ::span
                                      :a ::a)))))

(spec/def ::li
  (spec/with-gen (spec/and vector? li-pattern)
    #(sgen/fmap vec (spec/gen li-pattern))))

(def header-pattern
  (spec/cat
   :type #{:h1 :h2 :h3 :h4 :h5 :h6}
   :attr-map (spec/? ::attr-map)
   :contents (spec/*
              (spec/or :text string?
                       :subform (spec/or :code ::code
                                         :em ::em
                                         :span ::span
                                         :a ::a)))))

(spec/def ::header
  (spec/with-gen (spec/and vector? header-pattern)
    #(sgen/fmap vec (spec/gen header-pattern))))

(spec/def ::in-form-elem
  (spec/or :header ::header
           :em ::em
           :a ::a
           :ul ::ul
           :ol ::ol
           :p ::p
           :pre ::pre
           :code ::code))

(def grid-cell-pattern
  (spec/cat :type #{:r-cell}
            :attr-map (spec/? ::attr-map)
            :contents (spec/* ::in-form-elem)))

(spec/def ::grid-cell
  (spec/with-gen (spec/and vector? grid-cell-pattern)
    #(sgen/fmap vec (spec/gen grid-cell-pattern))))

(def grid-pattern
  (spec/cat :type #{:r-grid}
            :attr-map (spec/? ::attr-map)
            :contents (spec/* ::grid-cell)))

(spec/def ::grid
  (spec/with-gen (spec/and vector? grid-pattern)
    #(sgen/fmap vec (spec/gen grid-pattern))))

(spec/fdef process-text
  :args
  (spec/alt
   :unary (spec/cat :form (spec/and vector? #(#{:r-grid} (first %))))
   :binary (spec/cat
            :form (spec/and vector? #(#{:r-grid} (first %)))
            :row  (spec/spec ::grid-cell)))
  :ret ::grid)

(comment

  (spec/explain ::in-form-elem [:em "text"])

  (spec/conform ::in-form-elem [:em {:id "emphasis"} "text" "more text"])

  (spec/conform ::grid-cell [:r-cell {:span "row"} [:p "text"]])
  (spec/conform ::grid-cell [:r-cell {:span "row"}])

  (spec/conform ::grid [:r-grid [:r-cell {:span "row"} [:p "text"]]])

  (process-text [:r-grid {:columns 8} "orphan text"]))
