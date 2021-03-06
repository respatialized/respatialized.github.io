(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup2.core :as hiccup]
            [hiccup.element :as elem]
            [hiccup.util :as util]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.tools.reader :as r]
            [flatland.ordered.set :refer [ordered-set]]
            [flatland.ordered.map :refer [ordered-map]]
            [respatialized.styles :as styles]
            [respatialized.document :as doc :refer [sectionize-contents]]
            [respatialized.parse :refer [parse parse-eval]
             :as parse])
  (:gen-class))

(defn doc-header
  "Returns a default header from a post's metadata def."
  [{:keys [title page-style scripts]}]
  (let [page-header
        (apply conj
               [:head
                [:title (str "Respatialized | " title)]
                [:meta {:charset "utf-8"}]
                [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
                [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
                [:script {:type "text/javascript" :src "js/prism.js" :async "async"}]
                [:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"}]
                [:link {:type "text/css" :href "css/fonts.css" :rel "stylesheet"}]
                [:link {:type "text/css" :href "css/main.css" :rel "stylesheet"}]]
               scripts)]
    (if page-style (conj page-header [:style page-style]) page-header)))

(defn header
  "Create a structured header given the option map and child elements."
  [{:keys [date level class]
    :or {level :h1}
    :as opts} & contents]
  (let [c (if (not (map? opts)) (conj contents opts) contents)
        h (apply conj [level] c)
        d (if date [:time {:datetime date} date])]
    (respatialized.parse/conj-non-nil
     [:header]
     (if class {:class class} nil) h d)))

(defn em [& contents]  (apply conj [:em] contents))
(defn strong [& contents]  (apply conj [:strong] contents))

(defn link
  ([url
    {:keys [frag]
     :or {frag nil}
     :as opts} & contents]
   (let [c (if (not (map? opts)) (conj contents opts) contents)]
     (apply conj [:a {:href url}] c))))

(defn image
  ([path annotation class]
    [:img {:src path :alt annotation :class class}])
  ([path annotation] (image path annotation styles/img-default))
  ([path] (image path "")))

(defn code ([& contents] [:pre (apply conj [:code] contents)]))
(defn in-code ([& contents] (apply conj [:code] contents)))
(defn aside [& contents] (apply conj [:aside] contents))

;; (defn )

(defn blockquote
  [{:keys [caption url author source]
    :or {caption nil
         author ""
         url ""}
    :as opts} & contents]
  (let [c (if (not (map? opts)) (conj contents opts) contents)
        s (if source [:figcaption author ", " [:cite source]]
              [:figcaption author])]
    [:figure
     (apply conj [:blockquote {:cite url}] c) s]))

;; (defn blockquote
;;   ([content author
;;     {:keys [:outer-class
;;             :content-class
;;             :author-class]}]

;;    [:blockquote {:class outer-class}
;;     [:div {:class content-class} content]
;;     [:span {:class author-class} author]])
;;   ([content author]
;;    (blockquote content author
;;                {:outer-class styles/blockquote-outer
;;                 :content-class styles/blockquote-content
;;                 :author-class styles/blockquote-author})))

(defn quote [{:keys [cite]
              :or {cite ""}
              :as opts} & contents]
  (let [c (if (not (map? opts)) (conj contents opts) contents)]
    (apply conj [:q {:cite cite}] c)))

(defn ul [& contents]
  (apply conj [:ul] (map (fn [i] [:li i]) contents)))
(defn ol [& contents]
   (apply conj [:ol] (map (fn [i] [:li i]) contents)))

(defn sorted-map-vec->table
  "Converts a vector of maps to a hiccup table."
  ([sorted-map-vec header-class row-class]
   (let [ks (keys (first sorted-map-vec))
         vs (map vals sorted-map-vec)
         get-header (fn [k] [:th k])
         get-row (fn [rv] (apply conj [:tr {:class row-class}]
                                (map (fn [v] [:td v]) rv)))]

     [:table
      [:thead (apply conj [:tr {:class header-class}] (map get-header ks))]
      (into [:tbody] (map get-row vs))]))
  ([sorted-map-vec]
   (sorted-map-vec->table sorted-map-vec
                          styles/table-header
                          styles/table-row)))

(defn sorted-map->table
  "Converts a sorted map (array of structs) to a hiccup table."
  ([smap header-class row-class]
   (into
    [:table
     [:tr {:class header-class} (map (fn [k] [:th k]) (keys smap))]]
    (map (fn row [r] [:tr {:class row-class}
                      (map (fn [i] [:td i]) r)]) (vals smap))))
  ([smap]
   (sorted-map->table smap styles/table-header styles/table-row)))

(defn vec->table
  "Converts a vector of vectors to a hiccup table. Interprets the first vector as the header row."
  [[header & rows] header-class row-class]
   (into
    [:table
     [:tr {class header-class} (map (fn [i] [:th i] header))]
     (map (fn row [r] [:tr {:class row-class}
                       (map (fn [i] [:td i]) r)]) rows)]))

(defn- ->named-row [row-name vals-map col-names]
  (let [all-vals (merge (into (ordered-map)
                              (map (fn [c] [c ""]) col-names))
                        vals-map)]
    (apply conj [:tr [:th {:scope "row"} row-name]]
           (map (fn [[k v]] [:td v]) all-vals))))

(defn- map->tbody
  ([m cols]
   (apply conj [:tbody]
          (map (fn [[k v]] (->named-row k v cols)) m)))
  ([m cols group-name]
   (apply conj [:tbody [:tr [:th {:colspan (inc (count cols))} group-name]]]
          (map (fn [[k v]] (->named-row k v cols)) m))))

(defn- ->header
  ([cols]
   [:thead
    (apply conj [:tr]
           (map (fn [i] [:th (str (name i))]) cols))]))

(defn map->table
  "Converts the map to a table. Assumes keys are row headers and values
  are maps of row entries (key:column/val:val).
  Optionally breaks up the table into multiple <tbody> elements by an
  additional attribute."
  ([m subtable-attr]
   (let [body-keys (->> m
                        vals
                        (map keys)
                        flatten
                        (into (ordered-set))
                        ((fn [i] (disj i subtable-attr))))
         header
         (->header (concat ["name"] body-keys))

         grouped-entries
         (->> m
              (group-by (fn [[e vs]] (get vs subtable-attr)))
              (map (fn [[grp ms]]
                     [grp (into {} (map (fn [[entry vs]] [entry (dissoc vs subtable-attr)]) ms))])))]
     (apply conj [:table header]
            (map (fn [[sub-val vm]]
                   (map->tbody vm
                               body-keys
                               sub-val))
                 grouped-entries))))
  ([m]
   (let [all-keys (->> m
                       vals
                       (map keys)
                       flatten
                       (into (ordered-set)))
         header (->header (concat ["name"] all-keys))]
     [:table header
      (map->tbody m all-keys)])))

(defn script [attr-map & contents]
  (apply conj [:script attr-map] contents))

(def default-grid 8)

(defn template->hiccup
  "Converts a template file to a hiccup data structure for the page."
  [t]
  (let [parsed (parse/parse t)
        form-ns (parse/yank-ns parsed)
        tmpl-ns (if form-ns form-ns
                    (symbol (str "tmp-ns." (Math/abs (hash parsed)))))
        evaluated  (parse/eval-with-errors
                    parsed tmpl-ns doc/validate-element)
        page-meta (parse/eval-in-ns 'metadata tmpl-ns)
        body-content
        (into [:article {:lang "en"}]
              sectionize-contents
              evaluated)]
    [:html
     (doc-header page-meta)
     [:body
      body-content
      [:footer
       {:class "mb7"}
       [:div [:a {:href "/"} "Home"]]]]]))

;; (defn page
;;   "Converts a comb/hiccup file to HTML."
;;   [t]
;;   (hp/html5 (template->hiccup t)))



(def lit-open "//CODE{")
(def lit-close "}//")
(def comb-open "<%=(code \"")
(def comb-close "\")%>")

(defn fence-code [in-text]
  (-> in-text
      (string/replace lit-open comb-open)
      (string/replace lit-close comb-close)))

(defn include-file [file-path]
  (-> file-path
      slurp
      code))

(defn include-template-file [file-path]
  (-> file-path
      slurp
      fence-code
      parse))


(defn include-source
  ([{:keys [details]
     :or {details nil}
     :as opts} file-path]
   (let [source-code (slurp file-path)]
     (if details (conj [:details [:summary details]]
                       (code source-code))
         (code source-code))))
  ([file-path] (include-source {} file-path)))


(defn include-def
  "Excerpts the source code of the given symbol in the given file."
  ([{:keys [render-fn def-syms]
     :or {render-fn #(util/escape-html (with-out-str (pprint %)))
          def-syms #{'def 'defn}}} sym f]
   (with-open [r (clojure.java.io/reader f)]
     (loop [source (java.io.PushbackReader. r)]
       (if (not (.ready source)) :not-found
           (let [e (try (r/read source)
                        (catch Exception e nil))]
             (if (and (list? e)
                      (def-syms (first e))
                      (= sym (symbol (second e))))
               [:pre [:code {:class "language-clojure"} (render-fn e)]]
               (recur source)))))))
  ([sym f] (include-def {} sym f)))
