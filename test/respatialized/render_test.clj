(ns respatialized.render-test
  (:require [clojure.test :as t]
            [hiccup.core :refer [html]]
            [respatialized.render :refer :all]
            [respatialized.parse :refer [parse]]))

(t/deftest rendering
  (t/testing "html helper fns"

    (t/is (= "<em>help</em>" (html (em "help")))
          "emphasis should be added")
    (t/is (= "<em>help</em>" (html (em "help")))
          "emphasis should be added")




    (t/is
     (= (html (sorted-map-vec->table
               [(sorted-map :a 1 :b 2) (sorted-map :a 3 :b 4)]))
        "<table><thead><tr class=\"\"><th>a</th><th>b</th></tr></thead><tbody><tr class=\"\"><td>1</td><td>2</td></tr><tr class=\"\"><td>3</td><td>4</td></tr></tbody></table>"))

    (t/is (= (html (sorted-map->table (sorted-map :a [1 2 3] :b [4 5 6])))
             "<table><tr class=\"\"><th>a</th><th>b</th></tr><tr class=\"\"><td>1</td><td>2</td><td>3</td></tr><tr class=\"\"><td>4</td><td>5</td><td>6</td></tr></table>"
             ))
    (t/is (= (html [:r-cell {:span 3} "ab"])
             "<r-cell span=\"3\">ab</r-cell>"))
    (t/is (= (html [:r-grid {:columns 6} [:r-cell {:span 3} "ab"]])
             "<r-grid columns=\"6\"><r-cell span=\"3\">ab</r-cell></r-grid>"))

    )

  (t/testing "source code fns"
    (t/is (= (include-def {:render-fn str} 'delimiters "./src/respatialized/parse.clj")
             [:code "(def delimiters [\"<%\" \"%>\"])"]))
    (t/is (= [:code
              "(def url (h/alt [:external (regex->model external-link-pattern)] [:internal (regex->model internal-link-pattern)]))"]
             (include-def {:render-fn str} 'url "./src/respatialized/document.clj")))))
