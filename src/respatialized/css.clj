(ns respatialized.css
  "Compositional css generated with garden"
  (:require [garden.core :as garden]
            [garden.stylesheet :refer [at-media at-import]]
            [garden.selectors :refer [attr-starts-with attr before after]]
            [garden.units :as u]
            [garden.color]))


;; reset
(def remedy
  ^{:doc "Garden translation of CSS remedy"
    :source "https://github.com/jensimmons/cssremedy"
    :license {:name "Mozilla Public License"
              :version 2.0
              :url "https://www.mozilla.org/en-US/MPL/2.0/"}}
  [#_[:*
    (before) (after)
    {:box-sizing "border-box"}]         ; unclear if I got the selectors right here
   [:html {:line-sizing "normal"}]
   [:body {:margin "0"}]
   [(attr "hidden") {:display "none"}]
   [:h1 {:font-size "2rem"}]
   [:h2 {:font-size "1.5rem"}]
   [:h3 {:font-size "1.17rem"}]
   [:h4 {:font-size "1.00rem"}]
   [:h5 {:font-size "0.83rem"}]
   [:h6 {:font-size "0.67rem"}]
   [:h1 {:margin "0.67em 0"}]
   [:pre {:white-space "pre-wrap"}]
   [:hr {:border-style "solid", :border-width "1px 0 0",
         :color "inherit", :height "0", :overflow "visible"}]
   [:img :svg :video :canvas :audio :iframe :embed :object
    {:display "block", :vertical-align "middle", :max-width "100%"}]
     #_ [:audio ":not([controls])" {:display "none"}]
   [:picture {:display "contents"}]
   [:source {:display "none"}]
   [:img :svg :video :canvas {:height "auto"}]
   [:audio {:width "100%"}]
   [:img {:border-style "none"}]
   [:svg {:overflow "hidden"}]
   [:article :aside :figcaption :figure :footer :header
    :hgroup :main :nav :section {:display "block"}]
   [(attr "type" "=" "'checkbox'")
    (attr "type" "=" "'radio'")
    {:box-sizing "border-box", :padding "0"}]])



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

;; SYNTAX HIGHLIGHTING



(def prism-rules
  ^{:doc "Garden translation of syntax highlighting rules from prism.js"
    :url "https://prismjs.com/download.html#themes=prism&languages=markup+css+clojure"}
  [[:code (attr "class" "*=" "\"language-\"") :pre
    (attr "class" "*=" "\"language-\"")
    {:color "black", :text-align "left", :tab-size "4", :-o-tab-size "4", :white-space "pre", :font-size "1em", :-ms-hyphens "none", :hyphens "none", :word-wrap "normal", :background "none", :word-spacing "normal", :word-break "normal", :-webkit-hyphens "none", :-moz-tab-size "4", :-moz-hyphens "none"}]
   [:pre (attr "class" "*=" "\"language-\"")
    ;; _:::-moz-selection
    :pre (attr "class" "*=" "\"language-\"") " "
    ;; :::-moz-selection
    :code (attr "class" "*=" "\"language-\"")
    ;; :::-moz-selection
    :code (attr "class" "*=" "\"language-\"") " "
    ;; :::-moz-selection
    {:text-shadow "none", :background "#b3d4fc"}]
   [:pre (attr "class" "*=" "\"language-\"")
    ;; :::selection
    :pre (attr "class" "*=" "\"language-\"") " "
    ;; :::selection
    :code (attr "class" "*=" "\"language-\"")
    ;; :::selection
    :code (attr "class" "*=" "\"language-\"") " "
    ;; :::selection
    {:text-shadow "none", :background "#b3d4fc"}]
   (at-media {:print true}
             [:code (attr "class" "*=" "\"language-\"")
              :pre (attr "class" "*=" "\"language-\"")
              {:text-shadow "none"}])
   [:pre (attr "class" "*=" "\"language-\"")
    {:padding "1em", :margin ".5em 0", :overflow "auto"}]
   [":not(pre)" ">" :code (attr "class" "*=" "\"language-\"")
    :pre (attr "class" "*=" "\"language-\"")
    {:background "#f5f2f0"}]
   [":not(pre)" ">" :code
    (attr "class" "*=" "\"language-\"")
    {:padding ".1em", :border-radius ".3em",
     :white-space "normal"}]
   [:.token :.comment :.token :.prolog :.token :.doctype :.token :.cdata {:color "slategray"}]
   [:.token :.punctuation {:color "#999"}]
   [:.token :.namespace {:opacity ".7"}]
   [:.token :.property :.token :.tag :.token :.boolean :.token :.number :.token :.constant :.token :.symbol :.token :.deleted {:color "#905"}]
   [:.token :.selector :.token :.attr-name :.token :.string :.token :.char :.token :.builtin :.token :.inserted {:color "#690"}]
   [:.token :.operator :.token :.entity :.token :.url :.language-css " " :.token :.string :.style " " :.token :.string {:color "#9a6e3a", :background "hsla(0,0%,100%,.5)"}]
   [:.token :.atrule :.token :.attr-value :.token :.keyword {:color "#07a"}]
   [:.token :.function :.token :.class-name {:color "#DD4A68"}]
   [:.token :.regex :.token :.important :.token :.variable {:color "#e90"}]
   [:.token :.important :.token :.bold {:font-weight "bold"}]
   [:.token :.italic {:font-style "italic"}]])

(def highlight-rules
  (list
   [:.token.comment
    :.token.prolog
    :.token.doctype
    {:color (get wal-colors "color5")}]
   [:.token.symbol (get wal-colors "color6")]))

(defn -main
  "main fn"
  []
  (->> [remedy
        prism-rules
        ;; [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        ]
       garden/css
       (spit "public/css/main.css")))

(comment
  (-main)

  )
