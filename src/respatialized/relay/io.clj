(ns respatialized.relay.io
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [respatialized.archive :as archive]
   [clojure.spec.alpha :as spec]
   [markdown.core :refer [md-to-html-string]]
   [hickory.core :as hickory]
   [clojure.walk :as walk]
   ))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (clojure.java.io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))


(defn file->edn
  "Parses the given file into EDN. Assumes no top-level delimiter"
  [file]
  (with-open [edn-file (clojure.java.io/reader file)]
    (-> (clojure.java.io/reader file)
        line-seq
        doall
        (#(map clojure.edn/read-string %)))))

(defn txt->edn
  "Parses the given text into clojure data structures.
   By default, chunks each paragraph into its own item.
   Paragraphs are considered distinct entities in the EAVT domain."
  [text]
  (->> (str/split text #"\n")
       (filter #(not (empty? %)))
       (map #(str "\"" % "\""))
       (map (fn [line] {:text (edn/read-string line)}))))



(defn md->hiccup [md-string]
  (-> md-string
      md-to-html-string
      hickory/parse
      hickory/as-hiccup))



(defn md-hiccup-table->map
  "Tidies the data in a parsed hiccup table structure into a map of vectors."
  [table-elem])

(spec/fdef md-hiccup-table->map
  :args vector?
  :ret ::archive/tidy-table)

(defn map->md-hiccup-table
  "Tidies the "
  [m]
  )

(spec/fdef map->md-hiccup-table
  :args ::archive/tidy-table)

(defn pull-table [hiccup-data])
