(ns respatialized.io-test
  (:require
   [clojure.test :as t]
   [respatialized.io :refer :all]))

(def test-txt (slurp "resources/building-with-earth-excerpt.txt"))

(t/deftest text-parsers
  (t/testing "basic parsing"
    (t/is (= 3 (count (txt->edn test-txt))))
    )

  (t/testing "identical output for regular and malformed inputs"))
