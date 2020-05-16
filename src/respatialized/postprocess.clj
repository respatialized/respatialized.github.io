(ns respatialized.postprocess
  "Namespace for post-processing strings after hiccup parsing."
  (:require
   [com.rpl.specter :as sp]
   [clojure.string :as string]))

(defn paragraphs
  ([elem attr s]
   (if (= {} attr)
     (map (fn [i] [elem i]) (string/split s #"\n\n"))
     (map (fn [i] [elem attr i]) (string/split s #"\n\n"))))
  ([elem attr] (fn [s] (paragraphs elem attr s)))
  ([elem] (fn [s] (paragraphs elem {} s))))

(defn cell-paragraphs [[_ attr? & texts]]
  (let [texts (if (map? attr?) texts (cons attr? texts))
        elem (if (map? attr?) [:r-cell attr?] [:r-cell])]
  (->> texts
       (filter string?)
       (map (paragraphs :p))
       (apply concat)
       (concat elem)
       (into []))))

(sp/declarepath CellWalker)
(sp/providepath CellWalker
  (sp/if-path #(or (vector? %) (seq? %))
    (sp/if-path [sp/FIRST #(= :r-cell %)]
      (sp/continue-then-stay sp/ALL CellWalker)
      [sp/ALL CellWalker])))

(sp/declarepath DivWalker)
(sp/providepath DivWalker
  (sp/if-path #(or (vector? %) (seq? %))
    (sp/if-path [sp/FIRST #(= :div %)]
      (sp/continue-then-stay sp/ALL DivWalker)
      [sp/ALL DivWalker])))

(defn tokenize [form]
  (sp/multi-transform*
   (sp/multi-path [CellWalker (sp/terminal cell-paragraphs)]
                  [sp/ALL string? (sp/terminal (paragraphs :r-cell {:span "row"}))])
   form))

(comment

  (def sample-form '("first paragraph\n\nsecond paragraph"
                     [:r-grid [:r-cell "first cell line\n\nsecond-cell-line"]
                      [:r-cell "another cell"]]))

  (sp/select DivWalker
             [:.some [:nested [:elements [:div [:div "oh my"] [:div "f"]]]]])

  (sp/select CellWalker [:r-grid [:r-cell "b"]])

  (sp/select [CellWalker sample-form string?] sample-form)

  (sp/transform* [CellWalker] cell-paragraphs sample-form)

  (sp/multi-transform* (sp/multi-path [CellWalker (sp/terminal cell-paragraphs)]
                                      [sp/ALL string? (sp/terminal (paragraphs :r-cell {:span "row"}))])
                       sample-form)

  (sp/transform [sp/ALL r-grid? sp/ALL r-cell?] identity sample-form)

  )
