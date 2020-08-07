(ns respatialized.transform
  (:require
   [clojure.string :as str]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer [counted-double-list ft-split-at ft-concat]]
   [meander.epsilon :as m]
   [meander.strategy.epsilon :as m*]))

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

(def in-form-elems #{:em :li :ol :ul :p :a :code ;; :div
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

(defn get-orphans [loc]
  (let [parent-type
        (if (-> loc zip/up)
          (-> loc (zip/up) (zip/node) first)
          :r-grid)]
    (cond
      (zip/end? loc) loc                ; are we at the end?
      (r-cell? (zip/node loc))          ; has this node been processed?
      (recur (zip/next loc))            ; if yes, continue
      (= parent-type :r-cell)           ; are we not in a context with orphans?
      (recur (zip/next loc))            ; then continue
      (and (zip/branch? loc)
           (some orphan? (zip/children loc))) ; are there orphans in the child node?
      (recur (zip/edit loc (group-orphans [:r-cell {:span "row"}]
                                          (fn [i] (and (vector? i) (= (first i) :r-cell))))))
      :else (recur (zip/next loc)))))

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


;; split the string, then group the orphans
;;


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
(def inline-elems #{:em :ol :ul :code :span :a})
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

(defn form-zipper [f] (zip/zipper not-in-form? identity (fn [_ c] c) f))

(defn process-text [form]
  (-> form
      form-zipper
      get-orphans
      zip/node
      form-zipper
      tokenize-paragraphs
      zip/node))

(comment

  (split-strings [:r-grid {:span "row"} "some text\n\nwith line break"] #"\n\n")

  (-> respatialized.transform-test/orphan-zip-2
      get-orphans
      zip/node
      (#(zip/zipper not-in-form? identity (fn [_ c] c) %))
      tokenize-paragraphs
      zip/node))
