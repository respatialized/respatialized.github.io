(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup.element :as elem]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [vivid.art :as art]
            [respatialized.styles :as styles])
  (:gen-class))


(defn header
  "Returns a default header."
  [title]
  (hp/html5
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    (hp/include-css "/css/fonts.css")
    (hp/include-css "/css/tachyons.min.css")]))

(defn link [url text]
  (hp/html5 (elem/link-to url text)))

(defn code [text] (hp/html5 [:pre [:code text]]))
(defn in-code [text] (hp/html5 [:code text]))

(defn hiccup
  "Converts a hiccup file to HTML."
  [content]
  (hp/html5
   [:article
    {:lang "en"}
    [:body styles/page
     [:div styles/copy content]]
    [:footer
     {:class "mb7"}
     [:div [:a {:href "/"} "Home"]]]]))

(defn index
  "Generates the index from the list of posts."
  []
  (hp/html5 {:lang "en"}
            (header "Respatialized")
            [:body styles/page
             [:div {:class "f1 b"} "Respatialized"]
             [:div {:class "f3"} "recent writings"]
             [:ul {:class "f4"}
              [:li
               [:p [:a {:href "/not-a-tree.html"} "This Website Is Not A Tree"]]
               [:p "a metatextual introduction to this site as it is and as it could be."]]
              [:li [:p [:a {:href "/against-metadata.html"} "Against Metadata"]]
               [:p "rants against the apparent fact that metadata is treated as an afterthought in program design and configuration management."]]
              [:li [:p [:a {:href "/information-cocoon.html"} "Reifying the Filter Bubble, part 1"]]
               [:p "new digital infrastructure makes a metaphor less of one."]]
              [:li [:p [:a {:href "/reifying-filter-bubble-2.html"} "Reifying the Filter Bubble, part 2"]]
               [:p "notes against the 'decentralization' of an impoverished internet."]]
              [:li
               [:p [:a {:href "/working-definition.html"} "A Working Definition"]]
               [:p "a working definition of my own ideology."]]]]))

(defn entry-header [text date]
  (hp/html5 [:span {:class "f2 b"} text] [:span {:class "f4"} date]))

(defn em [text] (hp/html5 [:em text]))


(def art-config
  {:dependencies
   {'hiccup {:mvn/version "2.0.0-alpha2"}
    'org.clojure/clojure {:mvn/version "1.10.0"}
    }
   :bindings '{header 'header
               link 'link
               code 'code
               entry-header 'entry-header
               em em}
   })

(defn render-all [in-dir out-dir]
  (let [art-files (->> in-dir
                       io/file
                       file-seq
                       (filter #(and (.isFile %)
                                     (.endsWith (.toString %) art/art-filename-suffix))))]
    (doseq [f art-files]
      (let [out-file (-> f
                         (.getName)
                         (.toString)
                         (clojure.string/split art/art-filename-suffix-regex)
                         first
                         (#(str out-dir "/" %)))]
        (println "Rendering" (.toString out-file) "from path:" in-dir)
        (-> f
            slurp
            (art/render art-config)
            hiccup
            (#(spit out-file %)))))))

(defn -main []
  (render-all "content" "target")
  )
