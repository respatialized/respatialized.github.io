(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup.core :refer html]
            [hiccup.element :as elem]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [vivid.art :as art]
            [respatialized.styles :as styles])
  ;; (:gen-class :name respatialized.render)
  )

(defn doc-header
  "Returns a default header."
  [title]
  (html
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    (hp/include-css "css/fonts.css")
    (hp/include-css "css/tachyons.min.css")]))

(defn header
  ([title level]
   (html [:div [level title]]))
  ([title] (header title :h1)))

(defn link [url text]
  (html (elem/link-to url text)))

(defn code [text] (html [:pre [:code text]]))
(defn in-code [text] (html [:code text]))

;; <p><img src="media/thinking-about-things.jpg" alt="thinking about things" /></p>
(defn img ([dir alt] [:p [:img {:src dir :alt alt}]])
  ([dir] [:p [:img {:src dir}]]))

(defn hiccup
  "Converts a hiccup file to HTML."
  [content]
  (hp/html5
   [:article
    {:lang "en"}
    [:body styles/page
     [:div styles/copy content]]
    [:footer
     {:class "mb7"}
     [:div [:a {:href "/"} "Home"]]]]))

(defn index
  "Generates the index from the list of posts."
  []
  (hp/html5 {:lang "en"}
            (header "Respatialized")
            [:body styles/page
             [:div {:class "f1 b"} "Respatialized"]
             [:div {:class "f3"} "recent writings"]
             [:ul {:class "f4"}
              [:li
               [:p [:a {:href "/not-a-tree.html"} "This Website Is Not A Tree"]]
               [:p "a metatextual introduction to this site as it is and as it could be."]]
              [:li [:p [:a {:href "/against-metadata.html"} "Against Metadata"]]
               [:p "rants against the apparent fact that metadata is treated as an afterthought in program design and configuration management."]]
              [:li [:p [:a {:href "/information-cocoon.html"} "Reifying the Filter Bubble, part 1"]]
               [:p "new digital infrastructure makes a metaphor less of one."]]
              [:li [:p [:a {:href "/reifying-filter-bubble-2.html"} "Reifying the Filter Bubble, part 2"]]
               [:p "notes against the 'decentralization' of an impoverished internet."]]
              [:li
               [:p [:a {:href "/working-definition.html"} "A Working Definition"]]
               [:p "a working definition of my own ideology."]]]]))

(defn entry-header [text date]
  (html [:span {:class "f2 b"} text] [:span {:class "f4"} date]))

(defn em [text] (html [:em text]))



