(ns respatialized.parse-test
  (:require  [clojure.test :as t]
             [respatialized.render :refer [em]]
             [respatialized.parse :refer :all]))

(defn ns-refer [f]
  (require '[respatialized.render :refer [em]])
  (f))

(t/use-fixtures :once ns-refer)

(t/deftest parser
  (t/testing "string parsing"
    (t/is (= (eval-expr "=:foo") :foo)
          "Forms should be returned.")
    (t/is (= (eval-expr ":foo") nil)
          "Non-forms should be evaluated and not returned.")

    (t/is (= (parse-eval "<%=:foo%> bar <%=:baz%>")
             [:div :foo " bar " :baz]))
    (t/is (= (parse-eval "some text") [:div "some text"])
          "Plaintext should be passed as-is")
    (t/is (= (parse-eval "some text" [:r-cell {:span "row"}])
             [:r-cell {:span "row"} "some text"])
          "Containing forms should be passed in correctly")
    (t/is (= (parse-eval "some text" [:r-grid {:columns 10}])
             [:r-grid {:columns 10} "some text"])
          "Containing forms should be passed in correctly")
    (t/is (= (parse-eval "<%=[1 2 3]%>") [:div [1 2 3]]))
    (t/is (= (parse-eval "<%=[\"a\" \"b\"]%>")  [:div ["a" "b"]])
          "Escaped quotes in forms should be preserved.")
    (t/is (= (parse-eval "<%(def var 3)%> foo <%=var%>") [:div " foo " 3])
          "In-form defs should be evaluated successfully.")

    (t/is (= (parse-eval "<%=(respatialized.render/em 3)%>")
             [:div [:em 3]])
          "Namespace scoping should be preserved")
    (t/is (= (parse-eval "<%=(em 3)%>")
             [:div [:em 3]])
          "Namespace scoping should be preserved")

    (t/is (= (hiccup.core/html (parse-eval "<%=[:em\"text\"]%>, with a comma following"))
             "<div><em>text</em>, with a comma following</div>"))))
