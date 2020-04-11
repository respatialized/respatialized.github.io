(ns respatialized.render
  (:require [hiccup.page :as hp]
            [comb.template :as template]
            [hiccup.core :refer [html]]
            [hiccup.element :as elem]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [respatialized.styles :as styles])
  (:gen-class)
  )

(defn doc-header
  "Returns a default header."
  [title]
  (html
   [:head
    [:title (str "Respatialized | " title)]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    (hp/include-css "css/fonts.css")
    (hp/include-css "css/tachyons.min.css")]))

(defn header
  ([title level class] (html [:div {:class class} [level title]]))
  ([title level] (header title level styles/header-default))
  ([title] (header title :h1)))

(defn entry-header
  ([text class]
   (html [:div {:class class} text]))
  ([text] (entry-header text styles/entry-header)))

(defn entry-date
  ([date class] (html [:div {:class class} date]))
  ([date] (entry-date date styles/entry-date)))

(defn em [& texts] (html (into [:em ] texts)))
(defn strong [& texts] (html (into [:strong] texts)))

(defn link [url text]
  (html (elem/link-to url text)))

(defn image
  ([path annotation class]
   (html [:img {:src path :alt annotation :class class}]))
  ([path annotation] (image path annotation styles/img-default))
  ([path] (image path "")))

(defn code
  ([text class] (html [:pre [:code {:class class} text]]))
  ([text] (code text styles/code)))
(defn in-code [text] (html [:code text]))

(defn blockquote
  ([content author
    {:keys [:outer-class
            :content-class
            :author-class]}]
   (html
    [:blockquote {:class outer-class}
     [:p {:class content-class} content]
     [:span {:class author-class} author]]))
  ([content author]
   (blockquote content author
               {:outer-class styles/blockquote-outer
                :content-class styles/blockquote-content
                :author-class styles/blockquote-author})))

(defn img ([dir alt] [:p [:img {:src dir :alt alt}]])
  ([dir] [:p [:img {:src dir}]]))

(defn ul [& items]
  (html (into [:ul] (map (fn [i] [:li i]) items))))
(defn ol [& items]
  (html (into [:ol] (map (fn [i] [:li i]) items))))

(defn sorted-map-vec->table
  "Converts a vector of maps to a HTML table."
  ([sorted-map-vec header-class row-class]
   (let [ks (keys (first sorted-map-vec))
         vs (map vals sorted-map-vec)
         get-header (fn [k] [:th k])
         get-row (fn [rv] (into [:tr {:class row-class}]
                                (map (fn [v] [:td v]) rv)))]
     (html
      (into
       [:table
        [:tr {:class header-class} (map get-header ks)]]
       (map get-row vs)))))
  ([sorted-map-vec]
   (sorted-map-vec->table sorted-map-vec
                          styles/table-header
                          styles/table-row)))

(defn sorted-map->table
  "Converts a sorted map (array of structs) to a html table."
  [smap header-class row-class]
  (html
   (into
    [:table
     [:tr {:class header-class} (map (fn [k] [:th k]) (keys smap))]]
    (map (fn row [r] [:tr {:class row-class}
                      (map (fn [i] [:td i]) r)]) (vals smap)))))

(defn vec->table
  "Converts a vector of vectors to a html table. Interprets the first vector as the header row."
  [[header & rows] header-class row-class]
  (html
   (into
    [:table
     [:tr {class header-class} (map (fn [i] [:th i] header))]
     (map (fn row [r] [:tr {:class row-class}
                       (map (fn [i] [:td i]) r)]) rows)])))

(defn script [content attr-map]
  (html [:script attr-map content]))

(defn page
  "Converts a comb/hiccup file to HTML."
  [t]
  (let [content (template/eval t)
        page-meta (eval 'metadata)]
    (hp/html5
     (doc-header (:title page-meta ""))
     [:article
      {:lang "en"}
      [:body {:class (:page-class page-meta styles/page)}
       [:div {:class (:copy-class page-meta styles/copy)} content]]
      [:footer
       {:class "mb7"}
       [:div [:a {:href "/"} "Home"]]]])))



