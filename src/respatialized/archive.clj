(ns respatialized.archive
  "Namespace for defining note schemas and specs."
  (:require [datahike.api :as data]
            [clojure.string :as str]
            [clojure.spec.alpha :as spec]
            [spec-provider.provider :as sp]
            ))

(def db-config {:backend :mem
                :host "memorydb"})


(data/create-database db-config)

(def zk-note-attributes
  "Schema for Zettelkasten paper notes."
  [{:db/ident :prose
    :db/doc "Prose text, represented as a string."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :zk-number
    :db/doc "the Zettelkasten style note number as a string - 14.2.1"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/index true}
   {:db/ident :date
    :db/doc "A date, represented as a instant."
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/index true}
   {:db/ident :uuid
    :db/doc "a uuid that uniquely identifies this entity."
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}])

(def text-file-attributes
  "Attributes for writing parsed from a plaintext file."
  [{:db/ident :file
    :db/doc "the file path of the information's source."
    :db/valueType :db.type/string
    :db/cardinality :db/cardinality/one}
   {:db/ident :prose-elements
    :db/doc "The prose elements of a given file - refs to other entities."
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(defn same-size? [colls]
  (apply = (map count colls)))
(defn col-kvs? [table-map] (spec/valid? (spec/map-of string? vector?)
                                        (dissoc table-map ::table-meta)))

(spec/def ::same-size same-size?)
(spec/def ::eq-columns #(same-size? (filter sequential? (vals %))))
(spec/def ::col-kvs col-kvs?)

(spec/def ::table-whole-meta map?)
(spec/def ::table-body-meta map?)
(spec/def ::table-header-meta map?)

(spec/def ::table-meta
  (spec/keys :req [::table-whole-meta ::table-body-meta ::table-header-meta]))

(spec/def ::tidy-table
  (spec/and
   (spec/keys :opt [::table-meta])
   ::col-kvs
   ::eq-columns))

;; this spec can and should be refined using the sequence syntax
(spec/def ::hiccup-table (spec/and vector? #(= (first %) :table)))

; so should this, but it's not quite clear how
(spec/def ::hiccup-quote (spec/and vector? #(= (first %) :blockquote)))

(defn tidy-quote [hiccup-quote]
  (->>
   hiccup-quote
   (tree-seq vector? identity)
   (filter string?)
   (map str/trim)
   (apply vector)))

(spec/fdef tidy-quote
  :args ::hiccup-quote
  :ret (spec/coll-of string?))

(defn hiccup-table-header-values [table-header]
  (->> table-header
       (drop 2)
       (map (fn [[_ _ [_ _ e]]] e))))

(defn hiccup-row-values [table-body]
  (let [rows (drop 2 table-body)
        col-count (count (drop 2 (first rows)))]
    (reduce (fn [acc r]
              (let [rvals (map (fn [[_ _ e]] e) (drop 2 r))]
                (map-indexed (fn [i v] (conj v (nth rvals i))) acc)))
            (into [] (take col-count (repeat [])))
            rows)))

(spec/fdef hiccup-table-header
  :args ::hiccup-table
  :ret (spec/coll-of string?))


(defn tidy-hiccup-table
  [[_ tattrs header body]]
  (let [hattrs (first (filter map? header))
        hvals (hiccup-table-header-values header)
        battrs (first (filter map? body))]
    (into
     {::table-meta {::table-whole-meta tattrs
                    ::table-body-meta battrs
                    ::table-header-meta hattrs}}
     (zipmap hvals (hiccup-row-values body)))))

(spec/fdef tidy-hiccup-table
  :args ::hiccup-table
  :ret ::tidy-table)
