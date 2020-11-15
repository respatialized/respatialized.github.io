(ns respatialized.document
  "Namespace for document processing."
  (:require
   [clojure.string :as str]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer [counted-double-list ft-split-at ft-concat]]
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   [minimallist.core :as m :refer [valid? describe]]
   [minimallist.helper :as h]
   ;; [minimallist.generator :as mg]
   ;; [minimallist.minimap :as mm]
   ))

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

(def in-form-elems #{:em :li :ol :ul :p :a :code :span
                     :blockquote :pre
                     :h1 :h2 :h3 :h4 :h5 :h6}) ; elements that should be considered part of the same form

(defn in-form? [e] (and (vector? e)
                        (contains? in-form-elems (first e))))
(defn not-in-form? [e] (and (vector? e)
                            (not (contains? in-form-elems (first e)))))

(defn orphan? [e] (or (string? e) (in-form? e)))

(defn already-tokenized? [e]
  (and (vector? e) (= (first e) :p)))

(defn group-orphans
  ([encloser tokenized? s]
   (let [grouper
         (fn [s]
           (apply vector
                  (map
                   (fn [i]
                     (cond
                       (orphan? (first i))
                       (into encloser (apply vector i))
                       (tokenized? (first i)) (first i)
                       :else
                       i))
                   (partition-by #(cond
                                    (orphan? %) :orphan
                                    (tokenized? %) :tokenized) s))))]
     (if (keyword? (first s)) (into [(first s)] (grouper (rest s)))
         (grouper s))))
  ([encloser tokenized?] (fn [s] (group-orphans encloser tokenized? s))))

(comment
  (group-orphans [:p]
                 already-tokenized?
                 ["orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:p]
                 already-tokenized?
                 [:r-grid "orphan text"
                  [:em "with emphasis added"]])

  (group-orphans [:r-cell {:span "row"}]
                 #(and (vector? %) (contains? #{:p :r-cell} (first %)))
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
         (r-cell? (zip/node loc))       ; has this node been processed?
         (recur (zip/next loc))         ; if yes, continue
         (= parent-type :r-cell)        ; are we not in a context with orphans?
         (recur (zip/next loc))         ; then continue
         (and (zip/branch? loc)
              (some orphan? (zip/children loc))) ; are there orphans in the child node?
         (recur (zip/edit loc (group-orphans elem
                                             (fn [i] (and (vector? i) (= (first i) :r-cell))))))
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
(def inline-elems #{:em :ol :ul :code :span :a :li})
(defn inline? [i] (and (vector? i) (contains? inline-elems (first i))))

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
                         (para? current-elem)
                         (recur (concat rest t)
                                (conj (first (ft-split-at final (- (count final) 1)))
                                      (conj current-elem hh)))
                         :else (recur (concat rest t) (conj final [:p hh]))))
                     (or (string? h) (inline? h))
                     (if (para? current-elem)
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

(defn tokenize-paragraphs [loc]
  (cond
    (zip/end? loc) loc                  ; are we at the end?
    (para? (zip/node loc))                         ; has this node been processed?
    (recur (zip/next loc))              ; if yes, continue
    (and (zip/branch? loc)
         (some string? (zip/children loc)) ; are there strings in the child node?
         (not (every? para-tokenized? (zip/children loc))))
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

;; SPEC SPEC SPEC SPEC

;; a valid document is a collection of grid cells
;; what's the tree shape?
;; [:body [:head ...] [:article [:r-grid [:r-cell ...]]] [:footer ...]]
;; pretty simple, really

;; a grid cell is a collection of elements that are in-form but not inline
;; (e.g. paragraphs, headers)
;;
;; why does this matter?
;; because the solution shouldn't be ad-hoc if the goal is to assign a semantics
;; to document structure.

(comment
  (spec/exercise (spec/cat :type in-form-elems
                           :attr-map (spec/? map?)
                           :contents (spec/* some?))))

;; exercising the spec reveals the need to constrain the values more
;; it also indicates that there's a hierarchy within the in-form-elems
;; e.g. a paragraph can contain emphasis but emphasis can't contain a paragraph
;; list items should only be within a list

;; part of this ordering comes from the inherent semantics of HTML
;; part of it is my own choice to impose order

(def doc-tree
  {:p #{:ul :em :h5 :h4 :ol :h6 :code :h2 :h1 :h3 :a}
   :pre #{:em :span :a}
   :em #{:code :a :span}
   :a #{:em :span}
   :code #{:em :a :span}
   :ol #{:li}
   :ul #{:li}
   :li #{:code :em :a :span}
   :h1 #{:code :em :span :a}
   :h2 #{:code :em :span :a}
   :h3 #{:code :em :span :a}
   :h4 #{:code :em :span :a}
   :h5 #{:code :em :span :a}
   :h6 #{:code :em :span :a}
   })

(comment
  ;; example code from minimallist
  (def hiccup-model
    (h/let ['hiccup (h/alt [:node (h/in-vector (h/cat [:name (h/fn keyword?)]
                                                      [:props (h/? (h/map-of [(h/fn keyword?) (h/fn any?)]))]
                                                      [:children (h/* (h/not-inlined (h/ref 'hiccup)))]))]
                           [:primitive (h/alt [:nil (h/fn nil?)]
                                              [:boolean (h/fn boolean?)]
                                              [:number (h/fn number?)]
                                              [:text (h/fn string?)])])]
      (h/ref 'hiccup))))


(def url
  (h/alt
   [:external
    (h/fn #(re-matches #"^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" %))]
   [:internal
    (h/fn #(re-matches #"^/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" %))]))

(def attr-map
  (h/with-optional-entries (h/map)
   [:class (h/fn string?)]
   [:title (h/fn string?)]
   [:href url]
   [:id (h/fn string?)]
   [:alt (h/fn string?)]
   [:lang (h/enum #{"en"})]
   [:src url]))

(def image
  (h/in-vector
   (h/cat [:tag (h/val :img)]
          [:attributes
           (h/with-optional-entries
             (h/with-entries attr-map [:src url])
             [:width (h/fn int?)]
             [:height (h/fn int?)])])))

(def primitive
  (h/alt [:nil (h/fn nil?)]
         [:boolean (h/fn boolean?)]
         [:number (h/fn number?)]
         [:text (h/fn string?)]
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
                            primitive
                            (map elem-ref
                                 (get doc-tree elem)))))])))

(def raster-span
  (h/alt [:cols (h/fn pos-int?)]
         [:range (h/fn #(re-matches #"^\d\-\d$" %))]
         [:offset (h/fn #(re-matches #"^\d\+\d$" %))]
         [:offset-row (h/fn #(re-matches #"^\d\.\.$" %))]
         [:row (h/val "row")]))

(def grid
  (h/let ['em (get-child-model :em)
          'a (get-child-model :a)
          'pre (get-child-model :pre)
          'code (get-child-model :code)
          'ol (get-child-model :ol)
          'ul (get-child-model :ul)
          'li (get-child-model :li)
          'h1 (get-child-model :h1)
          'h2 (get-child-model :h1)
          'h3 (get-child-model :h3)
          'h4 (get-child-model :h4)
          'h5 (get-child-model :h5)
          'h6 (get-child-model :h6)
          'p (get-child-model :p)
          'grid-cell
          (h/in-vector
           (h/cat
            [:tag (h/val :r-cell)]
            [:attributes (h/with-entries attr-map [:span raster-span])]
            [:contents
             (h/* (h/not-inlined (apply h/alt (map elem-ref in-form-elems))))]))
          'grid
          (h/in-vector
           (h/cat [:tag (h/val :r-grid)]
                  [:attributes (h/with-entries attr-map [:columns (h/fn pos-int?)])]
                  [:contents (h/* (h/not-inlined (h/ref 'grid-cell)))]))]
    (h/ref 'grid)))

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
    #(gen/fmap vec (spec/gen p-pattern))))

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
    #(gen/fmap vec (spec/gen a-pattern))))

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
    #(gen/fmap vec (spec/gen em-pattern))))

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
    #(gen/fmap vec (spec/gen pre-pattern))))

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
    #(gen/fmap vec (spec/gen span-pattern))))

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
    #(gen/fmap vec (spec/gen code-pattern))))

(def ol-pattern
  (spec/cat :type #{:ol}
            :attr-map (spec/? ::attr-map)
            :items (spec/* ::li)))

(spec/def ::ol
  (spec/with-gen (spec/and vector? ol-pattern)
    #(gen/fmap vec (spec/gen ol-pattern))))

(def ul-pattern
  (spec/cat :type #{:ul}
            :attr-map (spec/? ::attr-map)
            :items (spec/* ::li)))

(spec/def ::ul
  (spec/with-gen (spec/and vector? ul-pattern)
    #(gen/fmap vec (spec/gen ul-pattern))))

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
    #(gen/fmap vec (spec/gen li-pattern))))

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
    #(gen/fmap vec (spec/gen header-pattern))))

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
    #(gen/fmap vec (spec/gen grid-cell-pattern))))

(def grid-pattern
  (spec/cat :type #{:r-grid}
            :attr-map (spec/? ::attr-map)
            :contents (spec/* ::grid-cell)))

(spec/def ::grid
 (spec/with-gen (spec/and vector? grid-pattern)
   #(gen/fmap vec (spec/gen grid-pattern))))

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

  (process-text [:r-grid "orphan text"])



  )
