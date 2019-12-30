(ns respatialized.archive-test
  (:require [clojure.test :as t]
            [respatialized.archive :refer :all]))

(t/deftest utils
  (t/testing "helper fns for specs")
  (t/is (same-size? [[1] [2] [3]]) "vectors should be correctly identified as equal in size")
  (t/is (not (same-size? [[1] [2] [3 4]])) "vectors should be correctly identified as equal in size")
  )
