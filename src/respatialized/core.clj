(ns respatialized.core
  (:require [hiccup.page :as hp]))

(defn page [data]
  (hp/html5 [:div {:style "max-width 900px; margin 40px auto:"}
             [:a {:href "/"} "Home"]
            (get-in data [:entry :content])]))
