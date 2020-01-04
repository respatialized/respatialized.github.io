(ns respatialized.archive-test
  (:require [clojure.test :as t]
            [respatialized.archive :refer :all]))



(t/deftest utils
  (t/testing "helper fns for specs"
    (t/is (same-size? [[1] [2] [3]]) "vectors should be correctly identified as equal in size")
    (t/is (not (same-size? [[1] [2] [3 4]])) "vectors should be correctly identified as equal in size")))

(def sample-hiccup-table
  [:table {}
   [:thead {}
    [:tr {} [:th {} "a"]]
    [:tr {} [:th {} "b"]]
    [:tr {} [:th {} "c"]]]
   [:tbody
    {}
    [:tr
     {}
     [:td {} "1"]
     [:td {} "2"]
     [:td {} "3"]]
    [:tr
     {}
     [:td {} "4"]
     [:td {} "5"]
     [:td {} "6"]]]])

(t/deftest hiccup-fns

  (t/testing "table utils"
    (t/is
     (= ["a" "b" "c"]
        (hiccup-table-header-values (nth sample-hiccup-table 2))))
    (t/is (= [["1" "4"] ["2" "5"] ["3" "6"]]
             (hiccup-row-values (nth sample-hiccup-table 3)) ))))
