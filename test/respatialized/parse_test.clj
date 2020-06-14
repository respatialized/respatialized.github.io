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
             '(:foo " bar " :baz)))
    (t/is (= (parse-eval "some text") '("some text"))
          "Plaintext should be passed as-is")
    (t/is (= (parse-eval "<%=[1 2 3]%>") '([1 2 3])))
    (t/is (= (parse-eval "<%=[\"a\" \"b\"]%>") '(["a" "b"]))
          "Escaped quotes in forms should be preserved.")
    (t/is (= (parse-eval "<%(def var 3)%> foo <%=var%>") '(" foo " 3))
          "In-form defs should be evaluated successfully.")

    (t/is (= (parse-eval "<%=(respatialized.render/em 3)%>")
            '("<em>3</em>"))
          "Namespace scoping should be preserved")
    (t/is (= (parse-eval "<%=(em 3)%>")
             '("<em>3</em>"))
          "Namespace scoping should be preserved")

    (t/is (= (hiccup.core/html (parse-eval "<%=[:em\"text\"]%>, with a comma following"))
             "<em>text</em>, with a comma following"))))
