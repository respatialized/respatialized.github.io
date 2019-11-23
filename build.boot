(set-env!
 :source-paths #{"src"}
 :resource-paths #{"content" "resources"}
 :dependencies '[[perun "0.4.3-SNAPSHOT" :scope "test"]
                 [hiccup "2.0.0-alpha2" :exclusions [org.clojure/clojure]]
                 [pandeiro/boot-http "0.8.3" :exclusions [org.clojure/clojure]]])

(def project 'respatialized)

(require '[io.perun :as perun]
         '[pandeiro.boot-http :refer [serve]])

(deftask build
  "Build the blog from its constituent pieces."
  []
  (comp
   (perun/global-metadata :filename "metadata.edn")
   (perun/draft)
   (perun/markdown)
   (perun/static :renderer 'respatialized.core/render-static)
   (perun/print-meta)
   (perun/build-date)
   (perun/render :renderer 'respatialized.core/render-markdown)
   (perun/collection :renderer 'respatialized.core/render-index
                     :page "index.html")
   (perun/tags :renderer 'respatialized.core/render-tags)
   (perun/assortment :renderer 'respatialized.core/render-assortment
                     :grouper 'respatialized.core/assort)
   ;; (perun/sitemap)
   (perun/rss :description "respatialized")
   (perun/atom-feed :filterer :original)
   (perun/print-meta)
   (target)
   (notify)))

(deftask dev
  "Build and serve the blog locally."
  []
  (comp (watch)
        (build)
        (serve :resource-root "public")))
