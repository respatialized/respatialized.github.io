(ns respatialized.core
  (:require [hiccup.page :as hp]
            [clojure.string :as str]))


(defn header
  "Returns a default header."
  [title]
  [:head
   [:title title]
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
   (hp/include-css "/css/fonts.css")
   (hp/include-css "/css/tachyons.min.css")]
  )


(defn render-post
  "Converts a post to HTML."
  [{global-meta :meta posts :entries post :entry}]
  (hp/html5
   [:article
    {:lang "en"}
    (header (str (:site-title global-meta) "|" (:title post)))
    [:body {:class "bg-moon-gray ml5 avenir"}
     [:div {:class "f2 b code"} (:title post)]
     [:div {:class "f4 lh-copy mw9"} (:content post)]]
    [:footer
     {:class "mb7"}
     [:div [:a {:href "/"} "Home"]]]]))


(defn render-tags [{global-meta :meta posts :entries entry :entry}]
  (hp/html5 {:lang "en"}
            (header (str (:site-title global-meta) "|" (:topic entry)))
         [:body
          [:h1 (:title entry)]
          [:ul
           (for [post posts]
             [:li (:title post)])]]))

(defn render-assortment [{global-meta :meta posts :entries entry :entry}]
  (hp/html5 {:lang "en"}
            (header (str (:site-title global-meta) "|" (:keyword entry)))
         [:body
          [:h1 (str "Page " (:page entry))]
          [:ul
           (for [post posts]
             [:li (:title post)])]]))

(defn assort [entries]
  (->> entries
       (mapcat (fn [entry]
                 (if-let [kws (:keywords entry)]
                   (map #(-> [% entry]) (str/split kws #"\s*,\s*"))
                   [])))
       (reduce (fn [result [kw entry]]
                 (let [path (str kw ".html")]
                   (-> result
                       (update-in [path :entries] conj entry)
                       (assoc-in [path :entry :keyword] kw))))
               {})))

(defn render-index
  "Generates the index from the list of posts."
  [{global-meta :meta posts :entries}]
  (hp/html5 {:lang "en"}
            (header (:site-title global-meta))
            [:body {:class "bg-moon-gray ml5 avenir"}
             [:div {:class "f1 b code"} "Respatialized"]
             [:ul {:class "list flex pr3"}
              [:li [:a {:href "/about.html"} "About Page"]]
              [:li [:a {:href "/feed.rss"} "RSS"]]
              [:li [:a {:href "/atom.xml"} "Atom Feed"]]]
             [:div {:class "f3"} "recent writings"]
             [:ul {:class "f4"}
              [:li
               [:p [:a {:href "/against-metadata.html"} "Against Metadata"]]
               [:p "a brief rant against the apparent fact that metadata is treated as an afterthought."]]
              [:li
               [:p [:a {:href "/working-definition.html"} "A Working Definition"]]
               [:p "my current definition of 'socialism.'"]]
              ]]))
