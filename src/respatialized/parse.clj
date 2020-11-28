(ns respatialized.parse
  "Parsing utilities for embedded clojure forms."
  {:license {:source "https://github.com/weavejester/comb"
             :type "Eclipse Public License, v1.0"}}
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))

(def delimiters ["<%" "%>"])

(def parser-regex
  (re-pattern
   (str "(?s)\\A"
        "(?:" "(.*?)"
        (first delimiters) "(.*?)" (last delimiters)
        ")?"
        "(.*)\\z")))

(defn eval-expr [expr]
  (if (.startsWith expr "=")
    (eval (edn/read-string (subs expr 1)))
    (do (eval (edn/read-string expr))
        nil)))

(defn yield-expr [expr]
  (if (.startsWith expr "=")
    (edn/read-string (subs expr 1))
    `(do ~(edn/read-string expr) nil)))

(defn nil-or-empty? [v]
  (if (seqable? v) (empty? v)
      (nil? v)))

(defn conj-non-nil [s & args]
  (reduce conj s (filter #(not (nil-or-empty? %)) args)))

(defn parse
  ([src start-seq]
   (loop [src src form start-seq]
    (let [[_ before expr after] (re-matches parser-regex src)]
      (if expr
        (recur
         after
         (conj-non-nil form before (yield-expr expr)))
        (conj-non-nil form after)))))
  ([src] (parse src [:div])))

(defn parse-eval
  ([src start-seq]
   (loop [src src form start-seq]
           (let [[_ before expr after] (re-matches parser-regex src)]
             (if expr
               (recur
                after
                (conj-non-nil form before (eval-expr expr)))
               (conj-non-nil form after)))))
  ([src] (parse-eval src [:div])))
