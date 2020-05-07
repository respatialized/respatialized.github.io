(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup.core :refer [html]]
            [hiccup.element :as elem]
            [hiccup.util :as util]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [respatialized.styles :as styles]
            [respatialized.parse :refer [parse]])
  (:gen-class)
  )

(defn doc-header
  "Returns a default header."
  [title]
   [:head
    [:title (str "Respatialized | " title)]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    (hp/include-css "css/raster.css" "css/fonts.css" "css/main.css")
    ])

(defn header
  ([title level class]  [:div {:class class} [level title]])
  ([title level] (header title level styles/header-default))
  ([title] (header title :h1)))

(defn entry-header
  ([text class]
    [:div {:class class} text])
  ([text] (entry-header text styles/entry-header)))

(defn entry-date
  ([date class]  [:div {:class class} date])
  ([date] (entry-date date styles/entry-date)))

(defn em [& texts]  (into [:em ] texts))
(defn strong [& texts]  (into [:strong] texts))

(defn link [url text]
   (elem/link-to url text))

(defn image
  ([path annotation class]
    [:img {:src path :alt annotation :class class}])
  ([path annotation] (image path annotation styles/img-default))
  ([path] (image path "")))

(defn code
  ([text class]  [:pre {:class class} [:code text]])
  ([text] (code text styles/code)))

(defn in-code ([text] (in-code text styles/in-code))
  ([text class]  [:code {:class class} text]))

(defn blockquote
  ([content author
    {:keys [:outer-class
            :content-class
            :author-class]}]

    [:blockquote {:class outer-class}
     [:p {:class content-class} content]
     [:span {:class author-class} author]])
  ([content author]
   (blockquote content author
               {:outer-class styles/blockquote-outer
                :content-class styles/blockquote-content
                :author-class styles/blockquote-author})))

(defn img ([dir alt] [:p [:img {:src dir :alt alt}]])
  ([dir] [:p [:img {:src dir}]]))

(defn ul [& items]
   (into [:ul] (map (fn [i] [:li i] items))))
(defn ol [& items]
   (into [:ol] (map (fn [i] [:li i] items))))

(defn sorted-map-vec->table
  "Converts a vector of maps to a hiccup table."
  ([sorted-map-vec header-class row-class]
   (let [ks (keys (first sorted-map-vec))
         vs (map vals sorted-map-vec)
         get-header (fn [k] [:th k])
         get-row (fn [rv] (into [:tr {:class row-class}]
                                (map (fn [v] [:td v]) rv)))]

      (into
       [:table
        [:tr {:class header-class} (map get-header ks)]]
       (map get-row vs))))
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

(defn script [content attr-map]
  [:script attr-map content])

(defn template->hiccup
  "Converts a template file to hiccup data structures."
  [t]
  (let [content (parse t)
        page-meta (eval 'metadata)
        body (into [:body {:class (:page-class page-meta styles/page)}] content)
        ]
    (list (doc-header (:title page-meta ""))
     [:article
      {:lang "en"}
      body
      [:div {:class (:copy-class page-meta styles/copy)} content]]
      [:footer
       {:class "mb7"}
       [:div [:a {:href "/"} "Home"]]])))

(defn page
  "Converts a comb/hiccup file to HTML."
  [t]
  (hp/html5 (template->hiccup t)))




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
