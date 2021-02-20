(ns respatialized.build
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [hiccup.page :as hp]
   [respatialized.render :as render :refer :all]
   [respatialized.parse :as parse]
   [respatialized.holotype :as holotype]
   [juxt.dirwatch :refer [watch-dir close-watcher]]
   [clojure.string :as str]))

(def template-suffix ".ct")
(def template-suffix-regex #"#*[.]ct$")

(defn template-str->hiccup
  "Attempts to parse the given string"
  ([content-str {:keys [page-fn path]
                 :or {page-fn parse/parse-eval
                      path "[no file path given]"}}]
   (try
     (page-fn content-str)
     (catch Exception e
       (do (println (format "Caught an exception for %s: \n\n %s"
                            path (.getMessage e)))
           ::parse-error))))
  ([content-str] (template-str->hiccup content-str {})))

(def pages (atom {}))

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
         hp/html5
         (#(spit out-file %))
         )))
  ([path page-fn] (render-template-file path page-fn "public"))
  ([path]
   (render-template-file path render/template->hiccup "public")))


(defn get-template-files [dir suffix]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %)
                     (not (.isDirectory %))
                     (.endsWith (.toString %) suffix)))
       (map #(.toString %))))

(defn render-template-files
  "Renders the given files. Renders all in the content dir when called without args."
  ([template-files page-fn out-dir]
   (doseq [f template-files]
     (render-template-file f page-fn out-dir)))
  ([template-files page-fn] (render-template-files template-files page-fn "public"))
  ([template-files] (render-template-files template-files render/template->hiccup "public"))
  ([] (render-template-files (get-template-files "content" template-suffix))))

(defn load-deps []
  (do (require '[respatialized.render :refer :all]
               '[respatialized.holotype :as holotype]
               '[respatialized.structure.fractals :as fractals]
               '[hiccup.page :as hp]
               '[hiccup.core :refer [html]])))

(defn rerender [{:keys [file count action]}]
  (if (#{:create :modify} action)
    (do
      (println "re-rendering" (.toString file))
      (render-template-file (.toString file))
      (println "rendered"))))


(comment
  ;; store the post data and the html and only update it if
  ;; it renders without errors

  ;; the simplest implementation: just print the error and rerender with
  ;; the last known state
  ;; from there, we can begin to think about how to store exceptions
  ;; as values representing state
  (def example-post-map
    {"./content/holotype1.html.ct"
     {:data [#_ "post data"]
      :html "<doctype html>..."  ; not a requirement but may improve performance
      }})

  ;; after the initial implementation, other ideas are possible
  ;; 1. print exceptions in the rendered page by
  ;; 2. split up the successfullly eval'd fns from the errors
  ;; 3. begin saving a succession of successful renders in a database with commits as "checkpoints"
  )

(defn -main
  ([]
   (do

     (load-deps)
     (render-template-files)
     (println "establishing file watch")
     (let [fw (watch-dir rerender (io/file "./content/"))]
       (.addShutdownHook (java.lang.Runtime/getRuntime)
                         (Thread. (fn []
                                    (do (println "shutting down")
                                        (close-watcher fw)
                                        (shutdown-agents)))))
       (loop [watch fw]
         (await fw)
         (recur [fw])))))
  ([& files]
   (load-deps)
   (if (and (= 1 (count files))
            (.isDirectory (io/file (first files))))
     (do
       (render-template-files
        (get-template-files (first files)
                            template-suffix)))
     (do (render-template-files files)))))

(comment (-main))
