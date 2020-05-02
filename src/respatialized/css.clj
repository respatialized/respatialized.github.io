(ns respatialized.css
  "Tachyons-like compositional css generated with garden"
  (:require [garden.core :as garden]
            [garden.units :as u]
            [garden.color]))

(def wal-colors
  {"color1" "#33484E",
   "color6" "#F6971B",
   "color7" "#e6cca4",
   "color11" "#9E5B2B",
   "color4" "#E66A13",
   "color3" "#9E5B2B",
   "color5" "#768176",
   "color2" "#57585A",
   "color13" "#768176",
   "color15" "#e6cca4",
   "color12" "#E66A13",
   "color14" "#F6971B",
   "color8" "#a18e72",
   "color10" "#57585A",
   "color9" "#33484E",
   "color0" "#011117",
   "background" "#011117",
   "foreground" "#e6cca4",
   "cursor" "#e6cca4"})

;(defn color-shades "Yields" [color-hex])

;; SIZES
;;



;; SPACING
;; utilities for dealing with spacing of elements: floats, line heights


(defn create-scale [])

(def base-font-size (u/em 1.5))
(def line-height (u/em 1.45))
(def baseline (u/em-div line-height 2))
(def column-gap (u/em* baseline 4))
(def row-gap (u/em* baseline 2))

(defn max-widths [n]
  (conj
   (map
    (fn [i] [(keyword (str ".mw" i)) {:max-width (u/em* i line-height)}])
    (range 1 (+ n 1)))
   [:.mw-full {:max-wdith "100%"}]))

(defn max-heights [n]
  (map
   (fn [i] [(keyword (str ".mh" i)) {:max-height (u/em* i line-height)}])
   (range 1 (+ n 1))))

;; COLOR
;; BORDERS
;; DISPLAY
;; FONTS


(def sans-serif "\"Basier Square\"")
(def monospace "\"Basier Square Mono\"")

(def html-rules
  [:html {:-webkit-font-smoothing "auto"
          :-moz-osx-font-smoothing "auto"
          :text-rendering "optimizeLegibility"
          :font-family sans-serif
          :background (get wal-colors "background")
          :color (garden.color/lighten (get wal-colors "foreground") 10)
          :line-height line-height
          :font-size base-font-size}])

(defn -main
  "main fn"
  []
  (->> [html-rules
        (max-widths 80)
        (max-heights 60)
        [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        [:a {:color (garden.color/lighten (get wal-colors "color1") 10)}]]
       garden/css
       (spit "public/css/main.css")))

(comment
  (-main))
