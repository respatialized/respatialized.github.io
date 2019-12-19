(ns respatialized.io
  (:require [clojure.string :as str]
            [clojure.edn :as edn]))

(defn txt->edn
  "Parses the given text into clojure data structures.
   By default, chunks each paragraph into its own item."
  [text]
  (->> (str/split text #"\n")
       (filter #(not (empty? %)))
       (map #(str "\"" % "\""))
       (map edn/read-string)))
