(ns respatialized.holotype
  "Beyond markdown with Perun."
  (:require [hiccup.page :as hp]
            [respatialized.render :as render]
            [respatialized.structure.fractals :as fractals]
            [clojure2d.core :as clj2d]
            [clojure.java.io :as io]
            [fastmath.core :as m]))


(defn svg-pt [[^Double x ^Double y]]
  [:circle
   {:cx (m/* 500 (m/+ 0.5 (m// x 4.0)))
    :cy (m/* 700 (m/+ 0.5 (m// y 5.0)))
    :style {:opacity 0.2}
   :r 1}])

(def pts (into [:svg {:id "clifford" :width 500 :height 700}]
               (map svg-pt
                    (take 20000
                          (iterate
                           (fractals/clifford 1.7 1.7 0.6 1.2)
                           [0.7 0.7])))))

(def pts2 (into [:svg {:id "clifford" :width 500 :height 700}]
               (map svg-pt
                    (take 20000
                          (iterate
                           (fractals/clifford 1.7 2.7 0.9 1.2)
                           [0.7 0.7])))))

(defn one
  [{meta :meta entry :entry}]
  (hp/html5
   [:article
    {:lang "en"}
    (render/doc-header "HOLOTYPE1")
    [:body {:class "ml3 basier-mono bg-mid-gray"}
     [:div {:class "f1 b white"} "HOLOTYPE//1"]
     [:div [:p "an example of using clojure alone to render a Perun post:"]]
     [:div [:p "render svg at compile time using hiccup data structures"]]
     pts
     pts2
     [:div [:p "CAUTION RECOMMENDED // RESULTING PAGE WEIGHS 3.5MB"]]]
    [:footer {:class "white f3 mb7"}
     [:div [:a {:href "/"} "//RESPATIALIZED"]]]]))


(defn canvas->hiccup! [cnvs path desc]
  "Takes a clojure2d canvas and creates a hiccup img structure with its output.
   SIDE EFFECT: outputs an image at the path for the fn to refer to."
  (let [{x :w y :h} cnvs
        out-path (str "output/" path)
        ]
    (clj2d/save cnvs out-path)
    (into [:img {:src out-path :alt desc}])))

;; adding another layer of indirection to this function could allow it to be called only at build-time (not sure how yet)


(def holotype2-surface  (clj2d/canvas 500 700))
;; (def holotype2-display (clj2d/show-window holotype2-surface "RESPATIALIZED//HOLOTYPE2"))
(def dejong-pts
  (->> [-0.1 1.0]
       (iterate (fractals/de-jong 1.318 2.014 0.001 2.07))
       (map (fn [[x y]]
              [(m/* 500 (m/+ 0.5 (m// x 4.0)))
               (m/* 700 (m/+ 0.5 (m// y 4.0)))]))
       (take 10000)))

(defn draw-pts [canvas pts]
  (doseq [[x y] pts]
    (clj2d/rect canvas x y 1 1))
  canvas)

(defn two
  "holotype 2: aggregation of attractor points; blur of points based on relative density.
  larger points -> more blur."
  [{meta :meta entry :entry}]
  (let [img (clj2d/with-canvas-> holotype2-surface
              (clj2d/set-background 0.0 0.0 0.0 0.0)
              (clj2d/set-stroke 1)
              (clj2d/set-color 50 50 50 100)
              (draw-pts dejong-pts))]
    (hp/html5
     [:article
      {:lang "en"}
      (render/doc-header "HOLOTYPE-2")
      [:body {:class "ml3 basier-mono bg-mid-gray"}
       [:div {:class "f1 b white"} "HOLOTYPE//2"]
       [:div [:p "render png at compile time using clojure2d and boot"]]
       [:div (canvas->hiccup! img "holotype/2/01.png" "dejong" )]
       [:footer {:class "white f3 mb7"}
        [:div [:a {:href "/"} "//RESPATIALIZED"]]]]]))
  )


