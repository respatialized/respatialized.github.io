(ns respatialized.render-test
  (:require [clojure.test :as t]
            [hiccup.core :refer [html]]
            [respatialized.render :refer :all]
            [respatialized.parse :refer [parse]]))


(def sample-header
 "<head><title>Respatialized | about</title><meta charset=\"utf-8\" /><meta content=\"IE=edge,chrome=1\" http-equiv=\"X-UA-Compatible\" /><meta content=\"width=device-width, initial-scale=1.0, user-scalable=no\" name=\"viewport\" /><link href=\"css/raster.css\" rel=\"stylesheet\" type=\"text/css\" /><link href=\"css/fonts.css\" rel=\"stylesheet\" type=\"text/css\" /><link href=\"css/main.css\" rel=\"stylesheet\" type=\"text/css\" /></head>")


(t/deftest rendering
  (t/testing "html helper fns"

    (t/is (= "<em>help</em>" (html (em "help")))
          "emphasis should be added")
    (t/is (= "<em>help</em>" (html (em "help")))
          "emphasis should be added")
    (t/is (= (html (doc-header "about"))
             sample-header)
          "header text should be output correctly.")

    (t/is
     (= (html (blockquote "This is a quotation" "anon" {:outer-class  "f8"}))
        "<blockquote class=\"f8\"><p>This is a quotation</p><span>anon</span></blockquote>"))

    (t/is
     (= (html (sorted-map-vec->table
         [(sorted-map :a 1 :b 2) (sorted-map :a 3 :b 4)]))
        "<table><tr class=\"\"><th>a</th><th>b</th></tr><tr class=\"\"><td>1</td><td>2</td></tr><tr class=\"\"><td>3</td><td>4</td></tr></table>"))

     (= (html (sorted-map->table (sorted-map :a [1 2 3] :b [4 5 6])))
        "<table><tr class=\"\"><th>a</th><th>b</th></tr><tr class=\"\"><td>1</td><td>2</td><td>3</td></tr><tr class=\"\"><td>4</td><td>5</td><td>6</td></tr></table>")))
