(ns respatialized.render-test
  (:require [clojure.test :as t]
            [respatialized.render :refer :all]))


(def sample-header "<head><title>Respatialized | about</title><meta charset=\"utf-8\" /><meta content=\"IE=edge,chrome=1\" http-equiv=\"X-UA-Compatible\" /><meta content=\"width=device-width, initial-scale=1.0, user-scalable=no\" name=\"viewport\" /><link href=\"css/fonts.css\" rel=\"stylesheet\" type=\"text/css\" /><link href=\"css/tachyons.min.css\" rel=\"stylesheet\" type=\"text/css\" /></head>")


(t/deftest rendering
  (t/testing "html helper fns"

    (t/is (= "<em>help</em>" (em "help"))
          "emphasis should be added")
    (t/is (= "<em>help</em>" (em "help"))
          "emphasis should be added")
    (t/is (= (doc-header "about")
             sample-header)
          "header text should be output correctly.")

    (t/is
     (= (blockquote "This is a quotation" "anon" {:outer-class  "f8"})
        "<blockquote class=\"f8\"><p>This is a quotation</p><span>anon</span></blockquote>"))
    ))
