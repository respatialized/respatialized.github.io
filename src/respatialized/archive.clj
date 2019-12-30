(ns respatialized.archive
  "Namespace for defining note schemas and specs."
  (:require [datahike.api :as data]
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

(spec/def ::same-size same-size?)
(spec/def ::eq-columns #(same-size? (vals %)))

(spec/def ::tidy-table
  (spec/and (spec/map-of string? vector?)
            ::eq-columns))
