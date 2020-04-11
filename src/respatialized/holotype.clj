(ns respatialized.holotype
  "Beyond markdown with hiccup."
  (:require [hiccup.page :as hp]
            [respatialized.render :as render]
            [respatialized.structure.fractals :as fractals]
            [clojure2d.core :as clj2d]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [fastmath.core :as m]))


(defn svg-pt [[^Double x ^Double y]]
  [:circle
   {:cx (m/* 500 (m/+ 0.5 (m// x 4.0)))
    :cy (m/* 700 (m/+ 0.5 (m// y 5.0)))
    :style {:opacity 0.2}
   :r 1}])

(defn pts []
  (into [:svg {:id "clifford" :width 500 :height 700}]
        (map svg-pt
             (take 20000
                   (iterate
                    (fractals/clifford 1.7 1.7 0.6 1.2)
                    [0.7 0.7])))))

(defn pts2 []
  (into [:svg {:id "clifford" :width 500 :height 700}]
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
     (pts)
     (pts2)
     [:div [:p "CAUTION RECOMMENDED // RESULTING PAGE WEIGHS 3.5MB"]]]
    [:footer {:class "white f3 mb7"}
     [:div [:a {:href "/"} "//RESPATIALIZED"]]]]))




;; adding another layer of indirection to this function could allow it to be called only at build-time (not sure how yet)

(def h2-surface  (clj2d/canvas 900 1400))

(defn draw-pts [canvas pts]
  (doseq [[x y] pts]
    (clj2d/rect canvas x y 1 1))
  canvas)

(defn pull-colors
  ([path] (-> path
              slurp
              (#(json/read-str %))
              (#(select-keys % ["colors" "special"]))))
  ([] (pull-colors "/home/andrew/.cache/wal/colors.json")))

(def current-colors (pull-colors))

(defn h2 []
  (let
      [dejong-pts
       (->> [-0.1 1.0]
            (iterate (fractals/de-jong 1.318 2.014 0.001 2.07))
            (map (fn [[y x]]
                   [(m/* (:w h2-surface) (m/+ 0.7 (m// x 1.8)))
                    (m/* (:h h2-surface) (m/+ 0.5 (m// y 2.9)))]))
            (take 80000))]
    (clj2d/with-canvas-> h2-surface
      (clj2d/set-background (clojure2d.color/to-RGB (get-in current-colors ["colors" "color2"])))
      (clj2d/set-stroke 1)
      (clj2d/set-color (clojure2d.color/to-RGB (get-in current-colors ["special" "foreground"])))
      (draw-pts dejong-pts))
    ))

(defn canvas->hiccup! [cnvs path desc]
  "Takes a clojure2d canvas and creates a hiccup img structure with its output.
   SIDE EFFECT: outputs an image at the path for the fn to refer to."
  (do (with-out-str (clj2d/save cnvs (str "public/" path))))
  (into [:img {:src path :alt desc}]))

(comment
  ; only show the window when doing interactive development
  (clj2d/show-window h2-surface "RESPATIALIZED//HOLOTYPE2")
  )
