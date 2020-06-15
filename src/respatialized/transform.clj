(ns respatialized.transform
  (:require
   [clojure.string :as str]
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

;; general form of the transformation
;; [:r-cell "a\nb"] => [:r-cell "a" "b"]
;; [:r-cell ?elem-to-split] => [:r-cell ?s1 ?s2 ... ?sn]
;; does meander's term rewriting allow you to express this kind of
;; transformation?
;;
;; if not,

(comment

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

  ; the idea we're trying to express in meander - contextual splitting
  ; of text into items.
  ;
  ; if the text isn't in a grid or cell already,
  ; split the text into separate cells.
  ;
  ; if it is, split the text into paragraphs.
  ;
  ; in either case the fundamental operation is the same, it just
  ; gets passed different data depending on the nesting level
  ; of the string that's going to be split.
  ; but it always operates on a string.
  ;

  ; start with the simpler case - splitting any string into paragraphs
  ; while preserving its position


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
