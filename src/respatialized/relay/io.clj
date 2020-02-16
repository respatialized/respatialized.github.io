(ns respatialized.relay.io
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [respatialized.util :refer :all]
   [respatialized.archive :as archive]
   [clojure.spec.alpha :as spec]
   [datahike.api :as data]
   [markdown.core :refer [md-to-html-string]]
   [hickory.core :as hickory]
   [clojure.walk :as walk]
   [provisdom.spectomic.core :as spectomic]
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

(spec/def ::balanced-parenthesis
  (spec/* (spec/cat :open #{"("}
                    :p (spec/? ::balanced-parenthesis)
                    :close #{")"})))



(spec/def ::paragraph
  (spec/and string?
            (spec/conformer seq)
            (spec/cat :s #{\newline}
                      :body (spec/+ char?)
                      )))

(spec/def ::non-delim-chars (set "abcdefghijklmnopqrstuvwxyz1234567890: +-"))
(spec/def ::non-delim
  (spec/+ ::non-delim-chars))

(spec/def ::lozenge-form
  (spec/cat
   :lozenge #{\◊}
   :form (spec/cat :open-paren #{\(}
                   :body ::non-delim
                   :close-paren #{\)})))

(spec/def ::etn-text
  (spec/and string?
            (spec/conformer seq)
            (spec/*
             (spec/alt
              :text ::non-delim
              :expr ::lozenge-form))))

(defn expr->edn [form]
  (-> (select-values form [:open-paren :body :close-paren])
      flatten
      (#(apply str %))
      edn/read-string))

;; (spec/fdef expr->edn)

(defn etn->edn [etn-text]
  (let [conformed (spec/conform ::etn-text etn-text)]
    (if (= conformed :clojure.spec.alpha/invalid) etn-text
        (edn/read-string
         (apply str
                (flatten (select-values (:form conformed)
                                        [:open-paren :body :close-paren])))))))

(defn load-etn
  "Loads an ETN file. Does not attempt to evaluate any of the forms within it."
  [file]
  (-> (clojure.java.io/reader file)
      line-seq
      doall
      (#(filter (fn [i] (not (empty? i))) %))
      (#(map etn->edn %))))

