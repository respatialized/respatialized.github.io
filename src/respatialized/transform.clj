(ns respatialized.transform
  (:require
   [clojure.string :as str]
   [meander.epsilon :as m]
   [meander.strategy.epsilon :as m*]))

; example before
(def sample-form '("first paragraph\n\nsecond paragraph"
        [:r-grid [:r-cell "first cell line\n\nsecond cell line"]
         [:r-cell "another cell"]]
                   "third paragraph"))

(defn split-into-forms
  ([text tag attr sep]
   (m/rewrites (str/split text sep)
     [_ ... ?s . _ ...]
     [~tag ~attr ?s]))
  ([tag attr sep] (fn [text] (split-into-forms text tag attr sep)))
  ([tag sep] (split-into-forms tag {} sep))
  ([tag] (split-into-forms tag #"\n\n"))
  ([] (split-into-forms :p)))


; example after
(def sample-parsed-form
  '([:r-cell {:span "row"} "first paragraph"]
    [:r-cell {:span "row"} "second paragraph"]
    [:r-grid
     [:r-cell [:p "first cell line"] [:p "second cell line"]]
     [:r-cell "another cell"]]
    [:r-cell {:span "row"} "third paragraph"]))

(comment

  ; find top-level strings
  (m/search sample-form
    (_ ... (m/pred string? ?s) . _ ...)
    ?s)

  (m/search sample-form
    (_ ... (m/pred #(= (first %) :r-grid) ?g) . _ ...)
    ?g)

  (m/rewrites sample-form
    (_ ... (m/pred string? ?s) . _ ...)
    (m/app (split-into-forms :r-cell {:span "row"} #"\n\n") ?s))


  )
