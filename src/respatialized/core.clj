(ns respatialized.core
  (:require [hiccup.page :as hp]
            [clojure.string :as str]))

(defn page [data]
  (hp/html5 [:div {:style "max-width 900px; margin 40px auto:"}
             [:a {:href "/"} "Home"]
             (get-in data [:entry :content])]))


(defn render-post
  "Converts a post to HTML."
  [{global-meta :meta posts :entries post :entry}]
  (hp/html5 {:lang "en"}
         [:head
          [:title (str (:site-title global-meta) "|" (:title post))]
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]]
         [:body
          [:h1 (:title post)]
          [:div (:content post)]]))


(defn render-tags [{global-meta :meta posts :entries entry :entry}]
  (hp/html5 {:lang "en"}
         [:head
          [:title (str (:site-title global-meta) "|" (:topic entry))]
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]]
         [:body
          [:h1 (:title entry)]
          [:ul
           (for [post posts]
             [:li (:title post)])]]))

(defn render-assortment [{global-meta :meta posts :entries entry :entry}]
  (hp/html5 {:lang "en"}
         [:head
          [:title (str (:site-title global-meta) "|" (:keyword entry))]
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]]
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
         [:head
          [:title (:site-title global-meta)]
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]]
         [:body
          [:ul
           [:li [:a {:href "/about.html"} "About Page"]]
           [:li [:a {:href "/feed.rss"} "RSS"]]
           [:li [:a {:href "/atom.xml"} "Atom Feed"]]]
          [:ul
           (for [post posts]
             [:li [:a {:href (:permalink post)} (:title post)]])]]))
