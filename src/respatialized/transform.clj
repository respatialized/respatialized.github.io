(ns respatialized.transform
  (:require
   [clojure.string :as str]
   [clojure.zip :as zip]
   [meander.epsilon :as m]
   [meander.strategy.epsilon :as m*]))

; example before
(def sample-form '("first paragraph\n\nsecond paragraph"
                   [:r-grid [:r-cell "first cell line\n\nsecond cell line"]
                    [:r-cell "another cell"]]
                   "third paragraph"))

; example after
(def sample-parsed-form
  '([:r-cell {:span "row"} "first paragraph"]
    [:r-cell {:span "row"} "second paragraph"]
    [:r-grid
     [:r-cell [:p "first cell line"] [:p "second cell line"]]
     [:r-cell "another cell"]]
    [:r-cell {:span "row"} "third paragraph"]))

(defn split-into-forms
  ([text tag attr sep]
   (if (not (empty? attr))
     (m/rewrites (str/split text sep)
                 [_ ... ?s . _ ...]
                 [~tag ~attr ?s])
     (m/rewrites (str/split text sep)
                 [_ ... ?s . _ ...]
                 [~tag ?s])))
  ([tag attr sep] (fn [text] (split-into-forms text tag attr sep)))
  ([tag sep] (split-into-forms tag {} sep))
  ([tag] (split-into-forms tag #"\n\n"))
  ([] (split-into-forms :p)))

; warm up exercise: the vanilla clojure way to ignore
; the enclosing collection created by str/split
(defn split-and-insert
  "For each item in the sequence matching the predicate,
  split it by the given function and insert the result into the
  original sequence."
  [seq pred splitter]
  (let [v? (vector? seq)
        r (loop [s seq acc []]
            (if (empty? s) acc ; base case
                (let [h (first s) t (rest s)]
                  (if (pred h)
                    (recur t (concat acc (splitter h)))
                    (recur t (conj acc h))))))]
    (if v? (apply vector r) r)))

(defn apply-map
  "If i matches any of the predicate keys in the given map, apply the corresponding fn to it. Otherwise, return i."
  [fn-map i]
  (loop [kvs fn-map]
    (let [[pred func] (first kvs)]
      (cond
        (empty? kvs) i
        (pred i) (func i)
        :else (recur (rest kvs))))))

(defn grid-elem [i]
  (and (vector? i) (contains? #{:r-grid :r-cell} (first i))))

(defn r-cell? [i] (and (vector? i) (= (first i) :r-cell)))
; checks if the given item is in a a vector but not a grid vector
(defn not-grid-elem? [i]
  (and (vector? i) (not (contains? #{:r-grid :r-cell} (first i)))))
(defn split-cell-contents [c]
  (let [r (map #(if (string? %)
                  ((split-into-forms :p) %) %) c)]
    (if (vector? c) (into [] r) r)))

(def base-strategy
  (m*/pipe (m*/pred string?)
           (split-into-forms :r-cell {:span "row"} #"\n\n")))

(def elem-strategy
  (m*/pipe (m*/pred not-grid-elem?)
           identity))

(def cell-strategy
  (m*/pipe (m*/pred r-cell?) split-cell-contents))

(def rewrite-form-2
  (m*/some-td (m*/choice
               elem-strategy
               cell-strategy
               base-strategy)))

(def rewrite-form-2-trace (m*/trace rewrite-form-2))

;; general form of the transformation
;; [:r-cell "a\nb"] => [:r-cell "a" "b"]
;; [:r-cell ?elem-to-split] => [:r-cell ?s1 ?s2 ... ?sn]
;; does meander's term rewriting allow you to express this kind of
;; transformation? can you do "one to many" transformations that
;; put the returned values into the correct position in the sequence?
;;
;; if not, then rewrite-form-2 is the best that can be done right now.
;;
;; or perhaps it's just a question of piping together more than one
;; transformation - split in one go, then pattern match on the split
;; values in another
;;
;;
;; from the cookbook:
;; https://github.com/noprompt/meander/blob/epsilon/doc/cookbook.md#recursion-reduction-and-aggregation
;; "Patterns can call themselves with the m/cata operator. This is like recursion.
;; You can leverage self recursion to accumulate a result."
;;
;; it includes a very succinct example of the power of term rewriting
;; to express general computation - it's kinda like lambda calculus with
;; more rules of application that modify the terms under evaluation
;;
;;
;; actually it's a lot simpler to do this:
;;  (m/rewrites ["a\nb" "c\nd" 2 3 4 5 "e\nf"]
;;    (m/scan (m/pred string? !s))
;;    [:r-grid . [:r-cell !s] ...])
;;
;; the dot (.) and ellipsis (...) operators
;; do exactly this in the context of a containing sequence
;;
;; so the strategy looks something like this for the simplest case:
;; 1. collect the strings
;; 2. split the strings
;; 3. flatten the resulting sequence
;; 4. collect those strings (!split-strings)
;; 5. insert them like [:r-grid . [:r-cell !split-strings] ...]
;;
;; the advanced case is to ensure the strings get split differently
;; based on their surrounding context.

(defn tokenize-seq
  ([seq sep token]
   (m/rewrite seq
              (m/seqable (m/or
                          (m/and (m/pred string?)
                                 (m/app #(str/split % sep)
                                        [(m/app (fn [i] (into token [i])) !xs) ...]))
                          !xs) ...)
              [!xs ...]))
  ([sep token] (fn [seq] (tokenize-seq seq sep token)))
  ([token] (tokenize-seq #"\n\n" token))
  ([] (tokenize-seq [:r-cell {:span "row"}])))

(defn tokenizer
  ([sep token]
   (m*/guard
    #(and (sequential? %)
          (not (contains? #{:p :em :li :ul :ol} (first %))))
    (m*/rewrite
     (m/seqable
      (m/or
       (m/and (m/pred string?)
              (m/app #(str/split % sep)
                     [(m/app (fn [i] (into token [i])) !xs) ...]))
       !xs) ...)
     [!xs ...])))
  ([token] (tokenizer #"\n\n" token)))

;; the general idea: apply the rewriting to the sequence, then recurse into
;; the subsequences and do it again.

(def rewrite-strategy
  (m*/pipe
   (tokenizer [:r-cell {:span "row"}])
   (m*/breadth-first
    (tokenizer [:p]))))

(defn rewrite-form-3 [form]
  (let [l? (list? form)
        rw (rewrite-strategy form)]
    (if l? (apply list rw) rw)))

;; one challenge: pattern matching on the context to ensure that redundant rewrites
;; don't happen - guarding against things like [:p [:p [:p "text"]]]
;; these blow up certain strategies with stack overflow errors
;;
;; solving this challenge is actually the key to solving the issue
;; with inline elements not getting absorbed correctly

(def inline-elems #{:em :li :ol :ul :p}) ; elements that should be considered part of the same form
(defn inline? [e] (and (vector? e)
                       (contains? inline-elems (first e))))
(defn not-inline? [e] (and (vector? e)
                           (not (contains? inline-elems (first e)))))

(defn tokenize-elem
  "For each string in the element split it by the given regex,
  and insert the result into the original element. Leaves sub-elements as is."
  ([seq re]
   (let [v? (vector? seq)
         r (loop [s (apply list seq) final []]
             (if (empty? s) final       ; base case
                 (let [h (first s) t (rest s)
                       current-elem (last final)]
                   (cond
                     (and (string? h) (re-find re h))
                     (let [[hh & tt] (str/split h re)
                           rest (map (fn [i] [:p i]) tt)]
                       (if (and (vector? current-elem) (= (first current-elem) :p))
                         (recur (concat rest t)
                                (conj (apply vector (drop-last 1 final))
                                      (conj current-elem hh)))
                         (recur (concat rest t) (conj final [:p hh]))))
                     (or (string? h) (inline? h))
                     (if (and (vector? current-elem) (= (first current-elem) :p))
                       (recur t
                              (conj (apply vector (drop-last 1 final))
                                    (conj current-elem h)))
                       (recur t (conj final [:p h])))
                     :else
                     (recur t (conj final h))))))]
     (if v? (apply vector r)  r)))
  ([re] (fn [seq] (tokenize-elem seq re))))


(def rewrite-strategy-2
  (m*/pipe
   (m*/guard r-cell? (tokenize-elem #"\n\n"))
   (m*/guard string? #(tokenize-elem [:r-cell %] #"\n\n"))))


(comment
  (rewrite-strategy-2
   [:r-grid [:r-cell "some text with\n\nnewline and" [:em "emphasis"] "added"]])

  )

(comment

                                        ; tip from the library author via clojurians slack:
  (m/rewrite [1 2 "a" "b" "c"]  (m/seqable (m/or (m/pred string? !s) _) ...)
    [:ul . [:li !s] ...])

  (m/rewrite [1 2 "a" "b" "c"]  [(m/or (m/pred string? !s) _) ...]
    [:ul . [:li !s] ...])

                                        ; another from Jimmy Miller (another contributor)
  (m/rewrite ["ad,sf" 1 "asd,fa,sdf" 2 3]
    [(m/or
      (m/and (m/pred string?) (m/app #(clojure.string/split % #",") [!xs ...]))
      !xs) ...]
    [!xs ...])



  ;; using m/$ to match the context of an individual form


  (m/match [:r-cell "a\nb"]
    (m/$ ?context (m/pred string? ?s))
    (?context (str/split ?s #"\n")))

  (m/rewrite [() '(1 2 3)] ;; Initial state
    ;; Intermediate step with recursion
    [?current (?head & ?tail)]
    (m/cata [(?head & ?current) ?tail])

    ;; Done
    [?current ()] ?current)

  (m/rewrite [() '(1 2 3 :a :b)] ;; Initial state
    ;; Intermediate step with recursion
    [?current ((m/$ (m/pred keyword? ?k)) & ?tail)]
    (m/cata [(?k & ?current) ?tail])
    ;; Done
    [?current ()] ?current)

  (split-and-insert [1 2 3 "a|b|c"] string? #(str/split % #"\|"))

                                        ; find top-level strings
  (m/search sample-form
    (_ ... (m/pred string? ?s) . _ ...)
    ?s)

                                        ; find grid cells
  (m/search sample-form
    (_ ... (m/pred #(= (first %) :r-grid) ?g) . _ ...)
    ?g)

                                        ; split and insert into top-level list (attempt)
  (m/find
    (m/rewrites ["a\nb" "c"] [_ ... (m/app #(str/split % #"\n") ?s) . _ ...] ?s)
    [?i]
    ?i)

  (def p
    (m*/top-down
     (m*/match
       (m/pred string? ?s) (keyword ?s)
       (m/pred int? ?i) (inc ?i)
       ?x ?x)))

  (p [1 ["a" 2] "b" 3 "c"])

                                        ; sample on real form
                                        ;
  (def rewrite-form
    (m*/bottom-up
     (m*/match
       (m/pred string? ?s) (m/app (split-into-forms :r-cell {:span "row"} #"\n\n") ?s)
                                        ;(m/pred #(= (first %) :r-cell) ?c) (map (split-into-forms :p {} #"\n\n") ?c)
       ?x ?x)))

  (rewrite-form sample-form)

  (def hiccup
    [:div
     [:p {"foo" "bar"}
      [:strong "Foo"]
      [:em {"baz" "quux"} "Bar"
       [:u "Baz"]]]
     [:ul
      [:li "Beef"]
      [:li "Lamb"]
      [:li "Pork"]
      [:li "Chicken"]]])

  ;; meander.epsilon/find
  (m/find hiccup
    (m/with [%h1 [!tags {:as !attrs} . %hiccup ...]
             %h2 [!tags . %hiccup ...]
             %h3 !xs
             %hiccup (m/or %h1 %h2 %h3)]
      %hiccup)
    [!tags !attrs !xs])

  (m/rewrites
    ("first paragraph\n\nsecond paragraph"
     [:r-grid
      [:r-cell "first cell line\n\nsecond cell line"]
      [:r-cell "another cell"]]
     "third paragraph")

    (m/with [%s (m/pred string?)])

    (([:r-cell {:span "row"} "first paragraph"]
      [:r-cell {:span "row"} "second paragraph"])
     [:r-grid
      [:r-cell "first cell line\n\nsecond cell line"]
      [:r-cell "another cell"]]
     ([:r-cell {:span "row"} "third-paragraph"])))

  ;; the idea we're trying to express in meander - contextual splitting
  ;; of text into items.
  ;;
  ;; if the text isn't in a grid or cell already,
  ;; split the text into separate cells.
  ;;
  ;; if it is, split the text into paragraphs.
  ;;
  ;; in either case the fundamental operation is the same, it just
  ;; gets passed different data depending on the nesting level
  ;; of the string that's going to be split.
  ;; but it always operates on a string.
  ;;
  ;; start with the simpler case - splitting any string into paragraphs
  ;; while preserving its position

  (def rewrite-form
    (m*/bottom-up
     (m*/match
       (m/pred string? ?s)
       (m/rewrites (str/split ?s #"\n\n")
         [_ ... ?s . _ ...]
         [:p ?s])
       ?x ?x)))

  (rewrite-form sample-form)

  ;; a good starting point.
  ;;
  ;; next: add a bigger predicate pattern match before matching
  ;; against the string? predicate
  ;;
  ;;
  ;; that may not work - the replaced string would potentially get
  ;; matched again after the first predicate and thus wrapped twice
  ;;
  ;; come to think of it, was this split and wrap getting applied
  ;; recursively the cause of the stack overflow error?
  ;;
  ;;

  (rewrite-form-2 sample-form)

  ;; seems like the problem may have been the call to m*/attempt,
  ;; which repeatedly retried until it got the stack overflow.
  ;;
  ;; some-td produces the result I thought (m*/top-down (m*/attempt ...))
  ;; would achieve.
  )

;; trying something different: a zipper

(defn orphan? [e] (or (string? e) (inline? e)))

(defn already-tokenized? [e]
  (and (vector? e) (= (first e) :p)))

(defn group-orphans
    ([encloser s]
     (let [grouper
           (fn [s]
             (apply vector (mapcat
                            (fn [i] (if (orphan? (first i))
                                      (into encloser (apply vector i))
                                      i))
                            (partition-by #(or (orphan? %) (already-tokenized? %)) s))))]
       (if (keyword? (first s)) [(first s) (grouper (rest s))]
           (grouper s)
           )))
  ([encloser] (fn [s] (group-orphans encloser s))))

(comment
  (group-orphans [:p] ["orphan text"
                       [:em "with emphasis added"]])

  (group-orphans [:p] [:r-grid "orphan text"
                       [:em "with emphasis added"]])
 
  )


(defn get-orphans [loc]
  (println loc)
  (cond
    (zip/end? loc) loc
    (and (zip/branch? loc)
         (some orphan? (zip/children loc)) ; are there orphans?
         (not (every? orphan?
                      (zip/children loc))))
    (if (= :r-cell (first (zip/up loc)))
      (recur (zip/edit loc (group-orphans [:p])))
      (recur (zip/edit loc (group-orphans [:r-cell {:span "row"}])))
      )
    :else (recur (zip/next loc))))

(comment
  (def orphan-trees
    '([:r-grid
       "orphan text"
       [:em "with emphasis added"]
       [:r-cell "non-orphan text"]]
      ))

  (def orphan-grid-zipper
    (zip/zipper not-inline? identity (fn [_ c] c) (first orphan-trees)))

  (-> orphan-grid-zipper zip/next zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/next zip/node)
  ;; defining a custom predicate for a zipper lets you ignore inline elements and
  ;; only recurse into the subsequences that actually define child relationships

  (-> orphan-grid-zipper zip/next zip/next zip/up zip/node)
  (-> orphan-grid-zipper zip/next zip/next zip/next zip/next zip/next zip/up zip/node)
  ;; with a zipper you can always access the enclosing element
  ;; to perform contextual operations by matching on the parent element type


  (partition-by #(and (string? %) (some? (re-find #"\n\n" %))) [1 2 3 3 "a" "b" "c\n\nd" 4 3])
  ;; a standard library function that may be useful for paragraph splitting


  (get-orphans orphan-grid-zipper)
 
  ;; this doesn't work because it partitions the entire enclosing sequence
  ;; when actually we just want to partition the orphans
  )
