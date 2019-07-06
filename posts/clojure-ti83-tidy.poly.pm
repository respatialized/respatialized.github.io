#lang pollen
◊(define-meta title "Clojure is My TI-83, part 2: tidying data")
◊(define-meta published "2019-07-06")
◊(define-meta topics "math,clojure")
◊c[#:span "1-4" #:span-s "row"]{

In order to do my ◊link["https://cnx.org/contents/MBiUQmmY@23.31:i_O99VEg/1-6-Sampling-Experiment"]{self-assigned math homework}, I needed to ensure the data provided are in an appropriate format. Here's how the book provides the data.
}
◊c[#:span "row" #:span-s "row"]{
◊table[#:class "small"]{
Entree Cost | Under $10 | $10 to under $15 | $15 to under $20 | Over $20
San Jose | El Abuelo Taq, Pasta Mia, Emma’s Express, Bamboo Hut | Emperor’s Guard, Creekside Inn | Agenda, Gervais, Miro’s | Blake’s, Eulipia, Hayes Mansion, Germania
Palo Alto | Senor Taco, Olive Garden, Taxi’s | Ming’s, P.A. Joe’s, Stickney’s | Scott’s Seafood, Poolside Grill, Fish Market | Sundance Mine, Maddalena’s, Spago’s
Los Gatos | Mary’s Patio, Mount Everest, Sweet Pea’s, Andele Taqueria | Lindsey’s, Willow Street | Toll House | Charter House, La Maison Du Cafe
Mountain View | Maharaja, New Ma’s, Thai-Rific, Garden Fresh | Amber Indian, La Fiesta, Fiesta del Mar, Dawit | Austin’s, Shiva’s, Mazeh | Le Petit Bistro
Cupertino | Hobees, Hung Fu, Samrat, Panda Express | Santa Barb. Grill, Mand. Gourmet, Bombay Oven, Kathmandu West | Fontana’s, Blue Pheasant | Hamasushi, Helios
Sunnyvale | Chekijababi, Taj India, Full Throttle, Tia Juana, Lemon Grass | Pacific Fresh, Charley Brown’s, Cafe Cameroon, Faz, Aruba’s | Lion & Compass, The Palace, Beau Sejour
Santa Clara | Rangoli, Armadillo Willy’s, Thai Pepper, Pasand | Arthur’s, Katie’s Cafe, Pedro’s, La Galleria | Birk’s, Truya Sushi, Valley Plaza | Lakeside, Mariani’s
}
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
I can use clojure.spec to define a schema for tidy datasets, and enforce it on incoming data and the data we pass to functions. Defining one requires a choice about how to represent tidy data.
}
◊c[#:span "1-2"]{it can either be a map of column names and column vectors:
◊code{
{:cost [15 20 10]
 :city ["Palo Alto" "Mountain View" "Sunnyvale"]
 :name ["Poolside Grill" "Le Petit Bistro" "Pedro's"]
 }
}}
◊c[#:span "3-4"]{or it can be a vector of column name-column value maps:
◊code{
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

Now that I have a sample of the data that conforms to the spec, I can scrape the data out of the table and reshape it to conform to the spec.
}
