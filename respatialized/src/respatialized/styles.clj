(ns respatialized.styles
  (:require [hiccup.page :as hp]
            [clojure.string :as str]))

(def page {:class "bg-moon-gray ml3 basier"})

(def copy {:class "f4 lh-copy mw9"})

(defn blkquote [content author]
  [:blockquote {:class "bl bw2 b--green w-60"}
   [:p {:class "f3 pl2 bold"} content]
   [:span {:class "pl2"} author]])

