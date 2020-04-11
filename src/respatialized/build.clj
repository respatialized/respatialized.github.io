(ns respatialized.build
  (:require
   [comb.template :as template]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [hiccup.page :as hp]
   [respatialized.render :refer :all]
   [respatialized.postprocess :as postprocess]
   [respatialized.holotype :as holotype]
   [clojure.string :as str]))

(def template-suffix ".ct")
(def template-suffix-regex #"#*[.]ct$")

(defn render-template-file
  ([path page-fn out-dir]
   (let [out-file (-> path
                      io/file
                      (.getName)
                      (.toString)
                      (str/split template-suffix-regex)
                      first
                      (#(str out-dir "/" %)))]
     (println "Rendering" (.toString out-file))
   (-> path
       slurp
       page-fn
       postprocess/detect-paragraphs
       (#(spit out-file %))
       )))
  ([path page-fn] (render-template-file path page-fn "public"))
  ([path]
   (render-template-file path page "public")))

(defn get-template-files [dir suffix]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %)
                     (.endsWith (.toString %) suffix)))
       (map #(.toString %))))

(defn render-template-files
  "Renders the given files. Renders all in the content dir when called without args."
  ([template-files page-fn out-dir]
   (doseq [f template-files]
     (render-template-file f page-fn out-dir)))
  ([template-files page-fn] (render-template-files template-files page-fn "public"))
  ([template-files] (render-template-files template-files page "public"))
  ([] (render-template-files (get-template-files "content" template-suffix))))

(defn load-deps []
  (do (require '[respatialized.render :refer :all]
               '[respatialized.holotype :as holotype]
               '[hiccup.page :as hp]
               '[hiccup.core :refer [html]])))

(defn -main
  ([]
   (load-deps)
   (render-template-files))
  ([& files]
   (load-deps)
   (render-template-files files)))
