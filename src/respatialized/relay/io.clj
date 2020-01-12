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
      hickory/as-hiccup
      first))

(defn pull-tables [hiccup-data]
  (map archive/tidy-hiccup-table
       (filter #(spec/valid? ::archive/hiccup-table %)
               (tree-seq vector? identity hiccup-data))))

(defn pull-quotes [hiccup-data]
  (->> hiccup-data
   (tree-seq vector? identity)
   (filter (fn [i] (spec/valid? ::archive/hiccup-quote i)))
   (map (fn [q] (assoc {} :prose (archive/tidy-quote q))))))


;; this can and should be beefed up using the regex syntax of core.spec

(defn etn->edn
  "Naive solution: just parse out anything starting with a lozenge as a clojure function and treat everything else as a string."
  [etn]
  (if (= \◊ (first etn))
    (edn/read-string (.substring etn 1))
    etn))
