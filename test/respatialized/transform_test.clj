(ns respatialized.transform-test
  (:require [respatialized.transform :refer :all]
            [clojure.test :as t]))

(def sample-multi-form-input
  "first paragraph\n\nsecond paragraph with <%=[:em \"emphasis\"]%> text")

(t/deftest transforms
  (t/testing "splitter fns"
    (t/is (= (split-into-forms (first sample-form) :p {} #"\n\n")
             '([:p "first paragraph"] [:p "second paragraph"]))
          "splitter should tokenize text appropriately"))

  (t/testing "transforms"
    (t/is (=  '(([:r-cell {:span "row"} "first paragraph"]
                [:r-cell {:span "row"} "second paragraph"])
               [:r-grid
                [:r-cell ([:p "first cell line"] [:p "second cell line"])]
                [:r-cell ([:p "another cell"])]]
                ([:r-cell {:span "row"} "third paragraph"]))
            (rewrite-form-2 sample-form))
          "prototype transformers should yield appropriate results")

    (t/is (= "<r-cell span=\"row\">first paragraph</r-cell><r-cell span=\"row\">second paragraph</r-cell><r-grid><r-cell><p>first cell line</p><p>second cell line</p></r-cell><r-cell><p>another cell</p></r-cell></r-grid><r-cell span=\"row\">third paragraph</r-cell>"
             (hiccup.core/html (rewrite-form-2 sample-form)))
          "transformed text should be valid hiccup input")

    (t/is (= '(([:r-cell {:span "row"} "first paragraph"]
                [:r-cell {:span "row"} "second paragraph with " [:em "emphasis"] " text"]))
             (rewrite-form-2 (respatialized.parse/parse sample-multi-form-input)))
          "non-grid elements should be left as is")))