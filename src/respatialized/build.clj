(ns respatialized.build
  (:require
   [vivid.art :as art]
   [vivid.art.parse :refer [parse]]
   [comb.template :as template]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [respatialized.render :refer :all]
   [respatialized.postprocess :as postprocess]
   [respatialized.holotype :as holotype]
   [clojure.string :as str]))

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

(defn render-art-file [content]
  (page (art/render content art-config)))

(defn render-art-files
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
           render-art-file
           postprocess/detect-paragraphs
           (#(spit out-file %))))))
  ([art-files] (render-art-files art-files "public")))

(def template-suffix-regex #"#*[.]cc$")

(defn render-comb-file
  ([path page-fn out-dir]
   (let [out-file (-> path
                      io/file
                      (.getName)
                      (.toString)
                      (str/split template-suffix-regex)
                      first
                      (#(str out-dir "/" %)))]
   (-> path
       slurp
       page-fn
       postprocess/detect-paragraphs
       (#(spit out-file %))
       )))
  ([path page-fn] (render-comb-file path page-fn "public"))
  ([path]
   (render-comb-file path page "public")))

(defn get-art-files [dir]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %)
                     (.endsWith (.toString %) art/art-filename-suffix)))
       (map #(.toString %))))

(defn render-all-art [in-dir out-dir]
  (let [art-files (get-art-files in-dir)]
    (render-art-files art-files out-dir)))

(defn -main
  ([]
   (render-all-art "content" "public"))
  ([& files] (render-art-files files "public")))
