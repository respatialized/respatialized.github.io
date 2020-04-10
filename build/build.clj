(ns build
  (:require [vivid.art :as art]
            [vivid.art.parse :refer [parse]]
            [comb.template :as template]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [respatialized.render :as render]
            [respatialized.postprocess :as postprocess]
            [respatialized.holotype :as holotype]
            [clojure.string :as str]
            [clojure.java.classpath :as cp])
  (:gen-class))

(def art-config
  {:dependencies
   {'hiccup {:mvn/version "2.0.0-alpha2"}
    'org.clojure/clojure {:mvn/version "1.10.0"}
    'respatialized {:mvn/version "SNAPSHOT"}
    'clojure2d {:mvn/version "1.2.0-SNAPSHOT"}
    'generateme/fastmath {:mvn/version "1.4.0-SNAPSHOT"}
    }
   })

(defn check-art-form
  ([form pre config] (art/render (str pre form) config))
  ([form pre] (check-art-form form pre art-config))
  ([form] (check-art-form form "<% (require '[hiccup.core :refer [html]] '[respatialized.render :refer :all])%>")))

(defn check-art-file
  ([path pre config] (check-art-form (slurp path) pre config))
  ([path pre] (check-art-form (slurp path) pre art-config))
  ([path] (check-art-form (slurp path) "")))

(defn render-file-contents [content]
  (render/page (art/render content art-config)))

(defn render-files
  ([art-files out-dir]
   (doseq [f art-files]
     (let [out-file (-> f
                        io/file
                        (.getName)
                        (.toString)
                        (str/split art/art-filename-suffix-regex)
                        first
                        (#(str out-dir "/" %)))]
       (println "Rendering" (.toString out-file))
       (-> f
           slurp
           render-file-contents
           postprocess/detect-paragraphs
           (#(spit out-file %))))))
  ([art-files] (render-files art-files "public")))

(defn get-art-files [dir]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %)
                     (.endsWith (.toString %) art/art-filename-suffix)))
       (map #(.toString %))))

(defn render-all [in-dir out-dir]
  (let [art-files (get-art-files in-dir)]
    (render-files art-files out-dir)))

(defn -main
  ([]
   (render-all "content" "public"))
  ([& files] (render-files files "public")))
