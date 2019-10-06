#lang pollen
◊(define-meta title "Clojure is My TI-83, part 2: tidying data")
◊(define-meta published "2019-07-07")
◊(define-meta topics "math,clojure")
◊c[#:span "1-4" #:span-s "row"]{

In order to do my ◊link["https://cnx.org/contents/MBiUQmmY@23.31:i_O99VEg/1-6-Sampling-Experiment"]{self-assigned math homework}, I needed to ensure the data provided are in an appropriate format. Here's how the book provides the data.
}
◊c[#:span "row" #:span-s "row"]{
◊div[#:id "table-1-20-data"]{◊table{
Entree Cost | Under $10 | $10 to under $15 | $15 to under $20 | Over $20
San Jose | El Abuelo Taq, Pasta Mia, Emma’s Express, Bamboo Hut | Emperor’s Guard, Creekside Inn | Agenda, Gervais, Miro’s | Blake’s, Eulipia, Hayes Mansion, Germania
Palo Alto | Senor Taco, Olive Garden, Taxi’s | Ming’s, P.A. Joe’s, Stickney’s | Scott’s Seafood, Poolside Grill, Fish Market | Sundance Mine, Maddalena’s, Spago’s
Los Gatos | Mary’s Patio, Mount Everest, Sweet Pea’s, Andele Taqueria | Lindsey’s, Willow Street | Toll House | Charter House, La Maison Du Cafe
Mountain View | Maharaja, New Ma’s, Thai-Rific, Garden Fresh | Amber Indian, La Fiesta, Fiesta del Mar, Dawit | Austin’s, Shiva’s, Mazeh | Le Petit Bistro
Cupertino | Hobees, Hung Fu, Samrat, Panda Express | Santa Barb. Grill, Mand. Gourmet, Bombay Oven, Kathmandu West | Fontana’s, Blue Pheasant | Hamasushi, Helios
Sunnyvale | Chekijababi, Taj India, Full Throttle, Tia Juana, Lemon Grass | Pacific Fresh, Charley Brown’s, Cafe Cameroon, Faz, Aruba’s | Lion & Compass, The Palace, Beau Sejour
Santa Clara | Rangoli, Armadillo Willy’s, Thai Pepper, Pasand | Arthur’s, Katie’s Cafe, Pedro’s, La Galleria | Birk’s, Truya Sushi, Valley Plaza | Lakeside, Mariani’s
}}
}
◊c[#:span "1-4" #:span-s "row"]{
Perhaps easy to pick items from if you're counting them out by hand, but not the most helpful format for a statistical program to manipulate. Multiple values are bunched together in single table cells and identifying information is split across both a header row and the first column. Not much could be done with this data in its current form. In short, it needs to be tidied up.
}
◊c[#:span "1-2"]{
Hadley Wickham provides a helpful formal definition of tidy data in his ◊link["https://vita.had.co.nz/papers/tidy-data.pdf"]{classic paper} in ◊em{Journal of Statistical Software}.
}
◊c[#:span "3-4"]{
◊blockquote{
"Tidy datasets are easy to manipulate, model and visualise, and have a specific structure: each variable is a column, each observation is a row, and each type of observational unit is a table."
}
}
◊c[#:span "1-4"]{
I can use clojure.spec to define a schema for tidy datasets, and enforce it on incoming data and the data passed to functions. Defining one requires a choice about how to represent tidy data.
}
◊c[#:span "1-2"]{it can either be a map of column names and column vectors:
◊codeblock{
{:cost [15 20 10]
 :city ["Palo Alto" "Mountain View" "Sunnyvale"]
 :name ["Poolside Grill" "Le Petit Bistro" "Pedro's"]
 }
}}
◊c[#:span "3-4"]{or it can be a vector of column name-column value maps:
◊codeblock{
[
 {:cost 15 :city "Palo Alto" :name "Poolside Grill"}
 {:cost 20 :city "Mountain View" :name "Le Petit Bistro"}
 {:cost 10 :city "Palo Alto" :name "Poolside Grill"}
]
}}
◊c[#:span "1-4"]{
I chose the former for two reasons: it's less verbose, and it makes it possible to define functions that operate on individual columns (which after all, are just vectors). Here's what the spec looks like:
◊pre{◊code[#:class "language-klipse"]{
(ns respatialized.stax
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as set]))

;; all columns should be the same length
(s/def ::consistent? #(apply = (map count (vals %))))

;; only one observation per row
(s/def ::flat?  (fn [data] (every? (map #(not-any? coll? %)) (vals data))))

;; no empty rows
(defn empty-string? [i]
  (and (string? i) (empty? i)))
(s/def ::full? (fn [data]
                 (every?
                  (fn [col-data] (not-any? empty-string? col-data)) (vals data))))

;; putting it all together
(s/def ::tidy? (s/and ::consistent? ::flat? ::full?))

(def sample-target
 (s/conform ::tidy?
{:cost [15 20 10]
 :city ["Palo Alto" "Mountain View" "Sunnyvale"]
 :name ["Poolside Grill" "Le Petit Bistro" "Pedro's"]
 }))
}}

Now that I have a sample of the data that conforms to the ::tidy? spec, I can scrape the data out of the table and reshape it into a tidy form. ◊link["http://www.3jane.co.uk/posts/breaking-out-of-klipse"]{3Jane has some helpful info} on using Klipse code blocks to access HTML DOM elements in ClojureScript, which I can adapt to pull the data out of this table by its div ID.

◊pre{◊code[#:class "language-klipse"]{
(defn scrape-element-id [id]
  (->
    js/document
    ; get first panel on the page
    (.getElementById id)
    (.-firstChild)
    ))

(def table-html (scrape-element-id "table-1-20-data"))

(defn htmltable->vectors [htmltable]
  (let [row-data (->
                 (.-rows htmltable)
                 (array-seq)
                 ((fn [v] (map #(array-seq (.-cells %)) v)))
                 ((fn [v] (map (fn [i] (map #(.-innerHTML %) i)) v))))
        headers (first row-data)
        data    (rest row-data)
        ]
        (map #(zipmap headers %) data)
    ))

(def table-data (htmltable->vectors table-html))

(defn maps->map [& args]
  "Takes the given maps and zips the values of common keys into a vector."
  (apply (partial merge-with into) args))

(defn map-values
  "Same as Scala's .mapValues function -
applies the same fn to all values and preserves the keys"
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn get-obvs [row-data]
  "helper function to get the values for a row."
  (let [city (get row-data "Entree Cost")
        prices (dissoc row-data "Entree Cost")]
    (reduce (fn [m [k v]]
              (conj  m  (let [locations (clojure.string/split v #", ")
                              row-count (count locations)]
                          {:city (apply vector (repeat row-count city))
                           :price (apply vector (repeat row-count k))
                           :locations locations}))) [] prices)))

(defn filter-col
  "Returns the indices of the column data that meets the given predicate."
  ([pred]
   (fn [col]
     (apply
      sorted-set
      (keep-indexed
       #(if (pred %2) %1)
       col))))
  ([pred col] ((filter-col pred) col)))

(defn select-col [[col-name indices]]
  (fn [data] (let [col (col-name data)]
               [col-name (apply vector (map #(nth col %) indices))])))

(defn select
  "Returns the data at the given indices.
   Expects a map of column names to a vector of data indices and a map of column names to vectors of column values."
  ([ixs]
   (fn [data] (into {} (map #((select-col %) data) ixs))))
  ([ixs data] ((select ixs) data)))

(defn select-tidy
  "returns the data at the given indices for all keys in the data map."
  ([ixs]
   (fn [data]
     (map-values (fn [col] (apply vector (map #(nth col %) ixs))) data)))
   ([ixs data] ((select-tidy ixs) data)))

(defn remove-empty-rows [data]
    (let [ixs
          (map
           (fn [col] ((filter-col #(not (empty-string? %))) col))
           (vals data))
          result-ixs (apply sorted-set (apply set/intersection ixs))]
      ((select (into {} (map (fn [k] [k result-ixs]) (keys data)))) data)))

(def table-1-20-data-tidy
  ;; rather than testing in a separate file, spec can be used to enforce
  ;; a schema on incoming data.
  (s/conform ::tidy?
             (let [flattened
                   (apply maps->map
                          (map #(apply maps->map (get-obvs %)) table-data))]
               (remove-empty-rows flattened)
               )))

(select-tidy [0 13 21] table-1-20-data-tidy)
}}
In the process of making the data conform to the spec, I got a few functions for selecting and filtering tidy data for free. These will come in handy later.
}
