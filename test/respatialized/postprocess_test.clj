(ns respatialized.postprocess-test
  (:require [respatialized.postprocess :refer :all]
            [clojure.test :as t]))


(t/deftest postprocessing
  (t/testing "postprocessing functions"
    (t/is
     (= [:r-cell [:p "a"] [:p "b"]]
        (cell-paragraphs [:r-cell "a\n\nb"]))
     (= [:r-cell {:span 3} [:p "a"] [:p "b"]]
        (paragraphs :r-cell {:span 3} "a\n\nb"))
     )

    (t/is
     (=
      '(([:r-cell {:span "row"} "first paragraph"]
         [:r-cell {:span "row"} "second paragraph"])
        [:r-grid [:r-cell [:p "first cell line"] [:p "second-cell-line"]]
         [:r-cell [:p "another cell" ]]])
      (tokenize '("first paragraph\n\nsecond paragraph"
        [:r-grid [:r-cell "first cell line\n\nsecond-cell-line"]
         [:r-cell "another cell"]]))))

    (t/is (= '([:r-grid [:r-cell [:p "ab"] [:p "cd"]]])
             (tokenize '([:r-grid [:r-cell "ab\n\ncd"]]))))
    (t/is (vector? (second (first (tokenize '([:r-grid [:r-cell "ab\n\ncd"]]))))))

    (t/is (= (cell-paragraphs [:r-cell {:span "row"} [:h1 "title"]])
             [:r-cell {:span "row"} [:h1 "title"]]))
    (t/is (= (tokenize '("\n\n"
 [:r-cell {:span "row", :class "b"} [:h3 "title"]]
 "\n"
 [:div {:class "f4"} "2020-05-16"]
                         "\n\nsample text\n\nline 2 of sample text"))
'(()
 [:r-cell {:span "row", :class "b"} [:h3 "title"]]
 ([:r-cell {:span "row"} "\n"])
 [:div {:class "f4"} "2020-05-16"]
 ([:r-cell {:span "row"} ""]
  [:r-cell {:span "row"} "sample text"]
  [:r-cell {:span "row"} "line 2 of sample text"]))))))
