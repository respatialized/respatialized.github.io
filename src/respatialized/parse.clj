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

(defn yield-expr [expr]
  (if (.startsWith expr "=")
    (eval (edn/read-string (subs expr 1)))
    (do (eval (edn/read-string expr))
        nil)))

(defn nil-or-empty? [v]
  (if (seqable? v) (empty? v)
      (nil? v)))

(defn conj-non-nil [s & args]
  (seq (concat s (filter #(not (nil-or-empty? %)) args))))

(defn parse [src]
  (loop [src src form []]
    (let [[_ before expr after] (re-matches parser-regex src)]
      (if expr
        (recur
         after
         (conj-non-nil form before (yield-expr expr)))
        (conj-non-nil form after)))))
