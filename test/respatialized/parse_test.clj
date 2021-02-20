(ns respatialized.parse-test
  (:require  [clojure.test :as t]
             [respatialized.render :refer [em link code blockquote]]
             [respatialized.parse :refer :all]))

(defn ns-refer [f]
  (require '[respatialized.render :refer [em]])
  (f))

(t/use-fixtures :once ns-refer)

(t/deftest evals
  (t/testing "expression evaluation"
    (t/is (= (eval-expr "=:foo") :foo)
          "Forms should be returned.")

    (t/is (= (eval-expr ":foo") nil)
          "Non-forms should be evaluated and not returned.")

    (t/is (= (eval-expr "=((+ 3 4)") :respatialized.parse/parse-error)
          "Invalid exprs should return error values")
    (t/is (= (eval-expr "=(unknown-function 3 4)") :respatialized.parse/parse-error)
          "Invalid exprs should return error values")))

(t/deftest parser
  (t/testing "string parsing"


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
    (t/is (= (parse-eval "<%(def var 3)%> foo <%=var%>" [:div] "var-test-ns") [:div " foo " 3])
          "In-form defs should be evaluated successfully.")

    (t/is (= (parse-eval "<%=(respatialized.render/em 3)%>")
             [:div [:em 3]])
          "Namespace scoping should be preserved")
    (t/is (= (parse-eval "<%=(em 3)%>")
             [:div [:em 3]])
          "Namespace scoping should be preserved")

    (t/is (= (parse-eval "<%=(link \"here\" \"text\")%>" [:div] "link-ns" '[[respatialized.render :refer [link]]])
             [:div (link "here" "text")])
          "Links should parse + eval using external namespaces")

    (t/is (= (parse-eval "an inline <%=(link \"here\" \"link\")%>, followed by some <%=(em \"emphasis\").%>"
                         [:div]
                         "link-em-ns"
                         '[[respatialized.render :refer [link em]]])
             [:div
              "an inline "
              (link "here" "link")
              ", followed by some "
              (em "emphasis")])
          "All elements should end up in vector forms")


    (t/is (= (parse-eval "<%=[:em \"text\"]%>, with a comma following")
             [:div [:em "text"] ", with a comma following"]))

    (t/is (= (hiccup.core/html (parse-eval "<%=[:em\"text\"]%>, with a comma following"))
             "<div><em>text</em>, with a comma following</div>"))))
