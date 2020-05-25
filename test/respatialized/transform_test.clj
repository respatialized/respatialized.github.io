(ns respatialized.transform-test
  (:require [respatialized.transform :refer :all]
            [clojure.test :as t]))

(t/deftest transforms
  (t/testing "splitter fns"
    (t/is (= (split-into-forms (first sample-form) :p {} #"\n\n")
             '([:p "first paragraph"] [:p "second paragraph"]))
          "splitter should tokenize text appropriately"))

  (t/testing "transforms"
    (t/is (= (rewrite-form-2 sample-form)
             '(([:r-cell {:span "row"} "first paragraph"]
                [:r-cell {:span "row"} "second paragraph"])
               [:r-grid
                [:r-cell ([:p "first cell line"] [:p "second cell line"])]
                [:r-cell ([:p "another cell"])]]
               ([:r-cell {:span "row"} "third paragraph"])))
          "prototype transformers should yield appropriate results")

    (t/is (= (hiccup.core/html (rewrite-form-2 sample-form))
             "<r-cell span=\"row\">first paragraph</r-cell><r-cell span=\"row\">second paragraph</r-cell><r-grid><r-cell><p>first cell line</p><p>second cell line</p></r-cell><r-cell><p>another cell</p></r-cell></r-grid><r-cell span=\"row\">third paragraph</r-cell>")
          "transformed text should be valid hiccup input")))
