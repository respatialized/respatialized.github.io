(ns respatialized.transform-test
  (:require [respatialized.transform :refer :all]
            [hiccup.core]
            [clojure.test :as t]))

(def sample-multi-form-input
  "first paragraph\n\nsecond paragraph with <%=[:em \"emphasis\"]%> text")

(def orphan-trees
   '([:r-grid
      "orphan text"
      [:r-cell "non-orphan text"]]
     ))

(def orphan-zip
  (zip/zipper not-inline? identity (fn [_ c] c) (first orphan-trees)))

(t/deftest transforms
  (t/testing "splitter fns"

    (t/is (= (tokenize-seq [1 2 3 "a\nb" 4 "c\nd" 5] #"\n" [:p])
             [1 2 3 [:p "a"] [:p "b"] 4 [:p "c"] [:p "d"] 5])
          "tokenizer rewrite should tokenize text appropriately")


    (t/is (= [[:p "a"] [:p "b"]] ((tokenizer #"\n" [:p]) ["a\nb"]) )
          "tokenizer strategy should tokenize text appropriately")

    (t/is (= "abc" ((tokenizer #"\n" [:p]) "abc") )
          "tokenizer strategy should not tokenize strings into character sequences.")

    (t/is (= [[:p "abc"]] ((tokenizer #"\n" [:p]) ["abc"]) )
          "tokenizer strategy should not tokenize strings into character sequences.")

    (t/is (= '([:p "abc"]) ((tokenizer #"\n" [:p]) '("abc")) )
          "tokenizer strategy should not tokenize strings into character sequences.")

    (t/is (= [:p "abc"] ((tokenizer #"\n" [:p]) [:p "abc"]))
          "tokenizer strategy should guard against already-tokenized sequences.")

    (t/is (=
           [1 2 3
              [:r-cell {:span "row"} "a"]
              [:r-cell {:span "row"} "b"]
              4
              [:r-cell {:span "row"} "c"]
              [:r-cell {:span "row"} "d"]
              5]
           ((tokenizer  #"\n" [:r-cell {:span "row"}]) [1 2 3 "a\nb" 4 "c\nd" 5])
             )
          "tokenizer strategy should tokenize text appropriately")



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


    (t/is (= [:r-cell [:p "a"] [:p "b"] [:p "c"] :d :e]
             (tokenize-elem [:r-cell "a\nb\nc" :d :e] #"\n"))

          )
    
    (t/is (= [:r-cell [:p "a" [:em "b"]] :c :d]
             (tokenize-elem [:r-cell "a" [:em "b"] :c :d] #"\n")))

    (t/is (= [:r-grid [:r-cell [:p "some text with" [:em "emphasis"] "added"]]]
             (rewrite-form-3 [:r-grid [:r-cell "some text with" [:em "emphasis"] "added"]])
             "tokenization should group inline elements appropriately"))

    (t/is (= [:r-grid [:r-cell [:p "some text with"] [:p "newline and" [:em "emphasis"] "added"]]]
             (rewrite-form-3 [:r-grid [:r-cell "some text with\n\nnewline and" [:em "emphasis"] "added"]])
             "tokenization should group inline elements appropriately"))

    (t/is (= '([:r-cell {:span "row"} "first paragraph"]
               [:r-cell {:span "row"} "second paragraph with " [:em "emphasis"] " text"])
             (rewrite-form-3 (respatialized.parse/parse sample-multi-form-input )))
          "non-grid elements should be left as is")



    (t/is (=
            [:r-grid
             [:r-cell {:span "row"} "orphan text"
               [:em "with emphasis added"]]
              [:r-cell "non-orphan text"]]
             (-> orphan-zip get-orphans zip/node)))
    
    ))
