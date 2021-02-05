(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup.core :refer [html]]
            [hiccup.element :as elem]
            [hiccup.util :as util]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [respatialized.styles :as styles]
            [respatialized.document :refer [sectionize-contents]]
            [respatialized.parse :refer [parse parse-eval]])
  (:gen-class))

(defn doc-header
  "Returns a default header from a post's metadata def."
  [{:keys [title css-files page-style]}]
  (let [page-header
        [:head
         [:title (str "Respatialized | " title)]
         [:meta {:charset "utf-8"}]
         [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
         (apply hp/include-css "css/fonts.css" "css/main.css" css-files)]]
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
     (if class {:class class}) h d)))

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

(defn script [attr-map & contents]
  (apply conj [:script attr-map] contents))

(def default-grid 8)

(defn template->hiccup
  "Converts a template file to a hiccup data structure for the page."
  [t]
  (let [content (parse-eval t [:contents])
        page-meta (eval 'metadata)
        body-content (into [:article {:lang "en"}]
                           sectionize-contents
                           (rest content))]
    (list
    (doc-header page-meta)
     [:body
      body-content
      [:footer
       {:class "mb7"}
       [:div [:a {:href "/"} "Home"]]]])))

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


(defn include-source [file-path]
  (->> file-path
       slurp
       (conj [:pre])))
