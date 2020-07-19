(ns respatialized.postprocess
  "Namespace for post-processing strings after hiccup parsing."
  (:require
   [com.rpl.specter :as sp]
   [clojure.string :as str]))

(defn paragraphs
  ([elem attr s]
   (if (= {} attr)
     (map (fn [i] [elem i]) (str/split s #"\n\n"))
     (map (fn [i] [elem attr i]) (str/split s #"\n\n"))))
  ([elem attr] (fn [s] (paragraphs elem attr s)))
  ([elem] (fn [s] (paragraphs elem {} s))))

(defn cell-paragraphs [[_ attr? & items]]
  (let [items (if (map? attr?) items (cons attr? items))
        elem (if (map? attr?) [:r-cell attr?] [:r-cell])]
    (->> items
         (map (fn [e] (if (string? e) (paragraphs e :p) e)))
         (apply concat)
         (concat elem)
         (into []))))

(sp/declarepath CellWalker)
(sp/providepath CellWalker
                (sp/if-path #(or (vector? %) (seq? %))
                            (sp/if-path [sp/FIRST #(= :r-cell %)]
                                        (sp/continue-then-stay sp/ALL CellWalker)
                                        [sp/ALL CellWalker])))

(sp/declarepath StringWalker)
(sp/providepath StringWalker
                (sp/if-path #(or (vector? %) (seq? %))
                            (sp/if-path [sp/ALL string?]
                                        (sp/continue-then-stay sp/ALL StringWalker)
                                        [sp/ALL StringWalker])))

(sp/declarepath DivWalker)
(sp/providepath DivWalker
                (sp/if-path #(or (vector? %) (seq? %))
                            (sp/if-path [sp/FIRST #(= :div %)]
                                        (sp/continue-then-stay sp/ALL DivWalker)
                                        [sp/ALL DivWalker])))

(defn yank
  "Pulls vector elements matching the given predicate into the enclosing collection"
  [coll seq-of?]

  (sp/select* (sp/multi-path [sp/ALL #(or (not (seqable? %))
                                          (not (every? seq-of? %)))]
                             [sp/ALL #(and (seqable? %) (every? seq-of? %)) sp/ALL])
              coll))

(defn grid-elem? [i]
  (and (vector? i) #(or (= :r-grid (first i))
                        (= :r-cell (first i)))))

(defn cell? [i]
  (and (vector? i) (= :r-cell (first i))))

(defn paragraph? [i]
  (and (vector? i) (= :p (first i))))

(defn tokenize [form]
  (map (fn [i] (yank i paragraph?))
  (sp/multi-transform*
   (sp/multi-path ;[CellWalker (sp/terminal cell-paragraphs)]
    [CellWalker sp/ALL string? (sp/terminal (paragraphs :p))]
    [sp/ALL string? (sp/terminal (paragraphs :r-cell {:span "row"}))])
   form)
  ))

(comment

  (def sample-form '("first paragraph\n\nsecond paragraph"
                     [:r-grid [:r-cell "first cell line\n\nsecond-cell-line"]
                      [:r-cell "another cell"]]))

  (tokenize sample-form)

  (sp/select DivWalker
             [:.some [:nested [:elements [:div [:div "oh my"] [:div "f"]]]]])

  (sp/select CellWalker [:r-grid [:r-cell "b"]])

  (sp/select [CellWalker sample-form string?] sample-form)

  (sp/transform* [CellWalker] cell-paragraphs sample-form)

  (sp/multi-transform* (sp/multi-path [CellWalker (sp/terminal cell-paragraphs)]
                                      [sp/ALL string? (sp/terminal (paragraphs :r-cell {:span "row"}))])
                       sample-form)

  (sp/transform (sp/subselect (sp/multi-path (sp/srange 0 0) [sp/LAST sp/ALL]))
                (fn [res] [res])
                [:a :b :c [1 2 3 4]])

  ; using multi-path to yank the vector elements into the outer sequence
  (sp/select  (sp/multi-path [sp/ALL keyword?] [sp/LAST sp/ALL])
              [:a :b :c [1 2 3 4]])

  ; using multi-path to yank all vector elements into the outer sequence
  (sp/select  (sp/multi-path [sp/ALL keyword?] [sp/ALL vector? sp/ALL])
              [:a :b :c [1 2 3 4]])

  (sp/select (sp/multi-path [sp/ALL #(not (seqable? %))]
                            [sp/ALL vector? sp/ALL sp/collect])
             [:a :b :c [1 2 3 4]])

  (sp/transform (sp/multi-path [sp/ALL keyword?]
                               [sp/ALL vector? sp/ALL sp/collect])
                identity
                [:a :b :c [1 2 3 4]])

  ;; split an item into a sequence and insert it back into the enclosing sequence
  (sp/multi-transform
   (sp/multi-path [sp/ALL #(not (string? %)) (sp/terminal identity)]
                  [sp/ALL string? (sp/terminal (sp/transformed [sp/STAY] #(str/split #"\n")))])
   ["a\nb" 1 2])

  (sp/select [string? (sp/transformed [sp/ALL] #(str/split % "\n"))]
             ["a\nb" 1 2])

  (->> [:a :b :c [1 2 3 4]]
       yank-elem
       (sp/multi-transform* (sp/multi-path [sp/ALL keyword? (sp/terminal identity)]
                                           [sp/ALL int? (sp/terminal str)]))))
