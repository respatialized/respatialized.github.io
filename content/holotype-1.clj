(ns respatialized.public
  (:require [respatialized.core :refer :all]
            [respatialized.holotype :as holotype]))


(def holotype1
  [:article
   [:div {:class "f1 b"} "HOLOTYPE 1"]])

{:title "HOLOTYPE // 1" :content holotype1}
