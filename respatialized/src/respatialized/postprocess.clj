(ns respatialized.postprocess
  (:require [clojure.string :as s]))

(def ^:const delims {:paragraph {:sep #"\n\n"
                                 :begin "<p>"
                                 :end "</p>"}
                     :linebreak {:sep #"\n"
                                 :begin ""
                                 :end "<br>"}})

(def ^:const block-matcher #"<.*>.*</.*>")

(defn detect-paragraphs [text]
  (let [tokens (s/split text (get-in delims [:paragraph :sep]))]
    (s/join
     "\n"
     (map (fn tag [t]
            (if (re-matches block-matcher t) t
                (str (get-in delims [:paragraph :begin])
                     t
                     (get-in delims [:paragraph :end]))))
          tokens))))
