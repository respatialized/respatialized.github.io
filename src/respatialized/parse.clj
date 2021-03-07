(ns respatialized.parse
  "Parsing utilities for embedded clojure forms."
  {:license {:source "https://github.com/weavejester/comb"
             :type "Eclipse Public License, v1.0"}}
  (:require [clojure.edn :as edn]
            [clojure.tools.reader :as r]
            [minimallist.core :as m]
            [minimallist.helper :as h]
            [clojure.string :as string]))

(def delimiters ["<%" "%>"])

(def parser-regex
  (re-pattern
   (str "(?s)\\A"
        "(?:" "(.*?)"
        (first delimiters) "(.*?)" (last delimiters)
        ")?"
        "(.*)\\z")))

(def expr-model
  (h/fn #(and (string? %) (re-matches parser-regex %))))

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
  (let [attempt
        (try {:success (if (.startsWith expr "=")
                         (r/read-string (subs expr 1))
                         `(do ~(r/read-string expr) nil))}
             (catch Exception e
               {:error {:type (.getClass e)
                        :message (.getMessage e)}}))]
    {:expr (:success attempt)
     :src (str (first delimiters) expr (last delimiters))
     :err (:error attempt)
     :result nil}))

(defn nil-or-empty? [v]
  (if (seqable? v) (empty? v)
      (nil? v)))

(defn conj-non-nil [s & args]
  (reduce conj s (filter #(not (nil-or-empty? %)) args)))

(defn md5 [^String s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(def parsed-expr-model
  (h/map
   [:src expr-model]
   [:expr (h/alt [:nil (h/val nil)]
                 ; let's not define a model for arbitrary Clojure exprs
                 [:val (h/fn some?)])]
   [:err (h/alt [:nil (h/val nil)]
                [:error (h/map [:type (h/fn class?)]
                               [:message (h/fn string?)])])]
   [:result (h/alt [:nil (h/val nil)]
                   [:val (h/fn some?)])]))

(defn parse
  ([src start-seq]
   (loop [src src form start-seq]
     (let [[_ before expr after] (re-matches parser-regex src)]
       (if expr
         (recur
          after
          (conj-non-nil form before (yield-expr expr)))
         (conj-non-nil form after)))))
  ([src] (parse src [])))



(defn eval-parsed-expr
  ([{:keys [:src :expr :err :result]
     :as expr-map} simplify?]
   (cond err expr-map
         result result
         :else
         (let [res (try
                     {:result (eval expr)}
                     (catch Exception e
                       {:error {:type (.getClass e)
                                :message (.getMessage e)}}))]
           (cond
             (and simplify? (:result res)) ; nil is overloaded here
             (:result res)
             (and (nil? (:result res)) (nil? (:err res)))
             nil
             :else (merge expr-map res)))))
  ([expr] (eval-parsed-expr expr false)))

(comment
  (def nested-parsed-expr
    [:div [:p (first (parse "<%=(+ 3 9)%>"))]])

  (clojure.walk/postwalk
   #(if (m/valid? parsed-expr-model %)
      (eval-parsed-expr % true)
      %)
   nested-parsed-expr)
  )




(defn yank-ns
  "Pulls the namespace out of the first expression in the parse tree."
  [expr-tree]
  (let [first-expr (->> expr-tree
                        (tree-seq vector? identity)
                        (filter #(m/valid? parsed-model %))
                        first
                        :expr)]
    (if (and (seq? first-expr)
             (seq? (second first-expr))
             (= (symbol 'ns) (first (second first-expr))))
      (second (second first-expr))
      nil)))

(comment
  (yank-ns (parse "<%(ns test-ns)%>"))

  )

(defn eval-all
  ([parsed-form simplify?]
   (let [form-nmspc (yank-ns parsed-form)
         nmspc (if form-nmspc (create-ns form-nmspc) *ns*)]
     (binding [*ns* nmspc]
       (refer-clojure)
       (clojure.walk/postwalk
        (fn [i] (if (m/valid? parsed-expr-model i)
                  (eval-parsed-expr i simplify?)
                  i))
        parsed-form))))
  ([parsed-form] (eval-all parsed-form true)))

(defn eval-expr-ns
  "Evaluates the given EDN string expr in the given ns with the given deps."
  [expr nmspc deps]
  (let [yield? (.startsWith expr "=")
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
