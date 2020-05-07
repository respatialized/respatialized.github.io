(ns respatialized.parse-test
  (:require  [clojure.test :as t]
             [respatialized.render :refer [em]]
             [respatialized.parse :refer :all]))

(t/deftest parser
  (t/testing "string parsing"
    (t/is (= (yield-expr "=:foo") :foo)
          "Forms should be returned.")
    (t/is (= (yield-expr ":foo") nil)
          "Non-forms should be evaluated and not returned.")

    (t/is (= (parse "<%=:foo%> bar <%=:baz%>")
             '(:foo "bar" :baz)))
    (t/is (= (parse "some text") '("some text"))
          "Plaintext should be passed as-is")
    (t/is (= (parse "<%=[1 2 3]%>") '([1 2 3])))
    (t/is (= (parse "<%=[\"a\" \"b\"]%>") '(["a" "b"]))
          "Escaped quotes in forms should be preserved.")
    (t/is (= (parse "<%(def var 3)%> foo <%=var%>") '("foo" 3))
          "In-form defs should be evaluated successfully.")

    (t/is (= (parse "<%=(respatialized.render/em 3)%>")
             [[:em 3]])
          "Namespace scoping should be preserved")
    (t/is (= (parse "<%=(em 3)%>")
             [[:em 3]])
          "Namespace scoping should be preserved")))
