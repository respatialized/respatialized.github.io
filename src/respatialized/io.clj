(ns respatialized.io
  (:require [clojure.string :as str]
            [clojure.edn :as edn]))

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

;; I'm trying to break these pieces of writing free from their origins in plaintext files, so the fact that they were all defined in a single file is not worth preserving at the top level. Thus the canonical representation of a file is not a vector of that file's contents but rather the set of maps that all have the value of that filename for the :source-file attribute. This is a subtle distinction

;; but wait - order matters. the sequence of paragraphs needs some way of being preserved as a composite of individual paragraph maps. some ways:
;; 1a. files are entities too - just have them refer to their contents
;; {:entity 23542
;;  :attribute :filename
;;  :value "plaintext-file.txt"}
;; {:entity 23542
;;  :attribute :contents
;;  :value [52952 29587 29042]}
;; in this mode, order of paragraphs is asserted as a fact on the basis of the
;; vector of entity ids of the constituent paragraphs
;; 1b. alternatively, the facts about the paragraph order could just be composites of other facts:
;; {:entity 23542
;;  :attribute :contents
;;  :value [{:uuid ab50234 :text "opening paragraph goes here"}
;;          {:uuid ab50235 :text "second paragraph goes here"}]}
;; I don't really like 1b. it feels ad-hoc and non-relational, whereas 1a seems
;; more relationally correct but is semantically not as rich as an individual fact
;; (easily resolved by a query, though)
