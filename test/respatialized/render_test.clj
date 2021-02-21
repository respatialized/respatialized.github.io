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
    (t/is (= [:table
              [:thead [:tr [:th "name"] [:th "desc"] [:th "id"]]]
              [:tbody [:tr [:th {:scope "row"} "Entry 1"]
                       [:td "First item"] [:td 245]]
               [:tr [:th {:scope "row"} "Entry 2"]
                [:td "Second item"] [:td 249]]]]
             (map->table {"Entry 1" {:desc "First item" :id 245}
                          "Entry 2" {:desc "Second item" :id 249}})))
    (t/is (= [:table
              [:thead [:tr [:th "name"] [:th "desc"] [:th "id"]]]
              [:tbody
               [:tr [:th {:colspan 2} "I"]]
               [:tr [:th {:scope "row"} "Entry 1"]
                [:td "First item"] [:td 245]]
               [:tr [:th {:scope "row"} "Entry 2"]
                [:td "Second item"] [:td 249]]]
              [:tbody
               [:tr [:th {:colspan 2} "J"]]
               [:tr [:th {:scope "row"} "Entry 3"]
                [:td "Third item"] [:td 252]]]]
            (map->table {"Entry 1" {:desc "First item" :id 245 :group "I"}
                         "Entry 2" {:desc "Second item" :id 249 :group "I"}
                         "Entry 3" {:desc "Third item" :id 252 :group "J"}}
                        :group)))

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
             (include-def {:render-fn str} 'url "./src/respatialized/document.clj")))

    (t/is (= [:code "(defn hiccup-form? [f] (and (vector? f) (keyword? (first f))))"]
             (include-def {:render-fn str} 'hiccup-form? "./src/respatialized/document.clj")))

    ))
