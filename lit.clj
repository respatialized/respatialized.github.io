#!/usr/bin/env -S bb -i -o

(require '[clojure.test :refer [is]])

(def default-code-open "+begin_src")
(def default-code-close "+end_src")
(def default-comment ";")

(defn comment-lines
  "Comments out everything not between code fences, then removes the code fences to yield an executable script."
  {:test (fn []
           (is (= (comment-lines ["plaintext to comment"] ";" "//*" "*//")
                  [";plaintext to comment"]))
           (is (= (comment-lines ["//*" "(code \"to execute\")" "*//"]
                                 ";" "//*" "*//")
                  ["(code \"to execute\")"]))
           (is (= (comment-lines ["test" "+begin_src" "(+ 1 1)" "+end_src" "test3"])
                  [";test" "(+ 1 1)" ";test3"]))
           )}
  ([input-lines comment-char open-block close-block]
   (loop [lines input-lines output-lines [] block? false]
     (cond
       (empty? lines) ; base case - the input is complete
       output-lines
       (empty? (first lines)) ; ignore blank lines
       (recur (rest lines) (conj output-lines (first lines)) block?)
       (= open-block (first lines))
       (recur (rest lines) output-lines true)
       (and block? (= (first lines) close-block))
       (recur (rest lines) output-lines false)
       block?
       (recur (rest lines) (conj output-lines (first lines)) block?)
       :else ; treate everything else as a comment
       (recur (rest lines) (conj output-lines (str comment-char (first lines))) block?))))
  ([input-lines] (comment-lines input-lines
                                default-comment
                                default-code-open
                                default-code-close)))

;; (clojure.test/run-tests)

(comment-lines *input*)
