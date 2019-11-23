(ns respatialized.holotype
  "Beyond markdown with Perun."
  (:require [hiccup.page :as hp]
            [respatialized.core :refer :all]
            [fastmath.core :as m]))

(defn clifford
  "Returns a Clifford attractor function.
   Use with iterate."
  [^Double a ^Double b ^Double c ^Double d]
  (fn [[^Double x ^Double y]]
    [(m/- (m/sin (m/* a y)) (m/* c (m/cos (m/* a x))))
     (m/- (m/sin (m/* b x)) (m/* d (m/cos (m/* b y))))]))

(defn svg-pt [[^Double x ^Double y]]
  [:circle
   {:cx (m/* 500 (m/+ 0.5 (m// x 4.0)))
    :cy (m/* 700 (m/+ 0.5 (m// y 4.0)))
   :r 1}])

(def pts (into [:svg {:id "clifford" :width 500 :height 700}]
               (map svg-pt
                    (take 20000
                          (iterate
                           (clifford 1.7 1.7 0.6 1.2)
                           [0.7 0.7])))))

(defn one
  [{meta :meta entry :entry}]
  (hp/html5
   [:article
    {:lang "en"}
    (header "HOLOTYPE1")
    [:body {:class "ml3 basier-mono bg-mid-gray"}
     [:div {:class "f1 b white"} "HOLOTYPE//1"]
     [:div [:p "an example of using Clojure alone to render a Perun post."]]
     [:div [:p "using Hiccup data structures, SVG can be generated using computational methods at compile time."]]
     [:svg {:id "holocanvas"
               :width 500
               :height 700
               :class "ba"}
      [:circle {:cx 50 :cy 50 :r "40"}]]
     [:div]
     pts]
    [:footer {:class "white mb7"}
     [:div [:a {:href "/"} "Home"]]]]))

