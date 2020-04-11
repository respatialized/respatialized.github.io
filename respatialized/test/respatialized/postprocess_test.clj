(ns respatialized.postprocess-test
  (:require [respatialized.postprocess :refer :all]
            [clojure.test :as t]))


(t/deftest postprocessing
  (t/testing "paragraph tokenization"
    (t/is (= "<p>First paragraph</p>\n<p>Second paragraph</p>"
             (detect-paragraphs "First paragraph\n\nSecond paragraph"))
          "Implied paragraphs should be delimited with <p> tags")

    (t/is (= "<div>First paragraph</div>\n<div>Second paragraph</div>"
             (detect-paragraphs "<div>First paragraph</div>\n\n<div>Second paragraph</div>"))
          "Block delimited paragraphs should be left alone."))

  (t/testing "line breaks")
  )
