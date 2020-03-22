(ns build
  (:require [vivid.art :as art]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [respatialized.render :as render]
            [clojure.string :as str]
            [clojure.java.classpath :as cp])
  (:gen-class))

(def art-config
  {:dependencies
   {'hiccup {:mvn/version "2.0.0-alpha2"}
    'org.clojure/clojure {:mvn/version "1.10.0"}
    'respatialized {:mvn/version "SNAPSHOT"}
    }
   })

(defn check-art-form
  ([form pre config] (art/render (str pre form) config))
  ([form pre] (check-art-form form pre art-config))
  ([form] (check-art-form form "<% (require '[hiccup.core :refer [html]] '[respatialized.render :refer :all])")))

(defn render-file-contents [content]
  (render/page (art/render content art-config)))

(defn render-all [in-dir out-dir]
  (let [art-files
        (->> in-dir
             io/file
             file-seq
             (filter #(and (.isFile %)
                           (.endsWith (.toString %) art/art-filename-suffix))))]
    (doseq [f art-files]
      (let [out-file (-> f
                         (.getName)
                         (.toString)
                         (str/split art/art-filename-suffix-regex)
                         first
                         (#(str out-dir "/" %)))]
        (println "Rendering" (.toString out-file) "from path:" in-dir)
        (-> f
            slurp
            render-file-contents
            (#(spit out-file %)))))))

(defn -main []
  (render-all "content" "public")
  )