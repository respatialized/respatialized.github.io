(ns respatialized.parse
  "Parsing utilities for embedded clojure forms."
  {:license {:source "https://github.com/weavejester/comb"
             :type "Eclipse Public License, v1.0"}}
  (:require [clojure.edn :as edn]
            [clojure.tools.reader :as r]
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
    (try
      (eval (r/read-string (subs expr 1)))
      (catch Exception e
        (do (println "caught an exception while reading:" (.getMessage e))
            ::parse-error)))
    (let [res (try (eval (r/read-string expr))
                   (catch Exception e
                     (do (println "caught an exception while reading:" (.getMessage e))
                         ::parse-error)))]
      (if (= res ::parse-error)
        res
        nil))))


(defn yield-expr [expr]
  (if (.startsWith expr "=")
    (edn/read-string (subs expr 1))
    `(do ~(edn/read-string expr) nil)))

(defn nil-or-empty? [v]
  (if (seqable? v) (empty? v)
      (nil? v)))

(defn conj-non-nil [s & args]
  (reduce conj s (filter #(not (nil-or-empty? %)) args)))

(defn md5 [^String s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

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

(defn eval-expr-ns
  "Evaluates the given EDN string expr in the given ns with the given deps."
  [expr nmspc deps]
  (let [yield?  (.startsWith expr "=")
        exp (if yield?
              (subs expr 1)
              expr)
        current-ns *ns*]
    (binding [*ns* (create-ns (symbol nmspc))]
      (do
        (refer-clojure)
        (if deps (apply require deps))
        (if yield?
          (eval (r/read-string exp))
          (do (eval (r/read-string exp)) nil))))))

(defn parse-eval
  ([src start-seq nmspc deps]
   (let [evaluator
         (if nmspc #(eval-expr-ns % nmspc deps)
             eval-expr)]
     (loop [src src form start-seq]
       (let [[_ before expr after] (re-matches parser-regex src)]
         (if expr
           (recur
            after
            (conj-non-nil form before
                          (let [res (evaluator expr)]
                            (if (= res ::parse-error)
                              (do (throw (Exception. "parse error detected")))
                              res))))
           (conj-non-nil form after))))))
  ([src start-seq nmspc] (parse-eval src start-seq nmspc nil))
  ([src start-seq] (parse-eval src start-seq nil))
  ([src] (parse-eval src [:div])))
