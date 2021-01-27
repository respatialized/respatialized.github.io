(ns respatialized.document-test
  (:require [respatialized.document :refer :all]
            [respatialized.parse :refer [parse parse-eval md5]]
            [respatialized.build :refer [get-template-files]]
            [hiccup.core :refer [html]]
            [clojure.test :as t]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.gfredericks.test.chuck.properties :as prop']
            [minimallist.core :refer [valid?]]
            [minimallist.generator :as mg]
            [minimallist.helper :as h]
            [minimallist.minimap :refer [minimap-model]])
  (:import [java.lang Exception]))

(defmethod t/assert-expr 'valid-model [msg form]
  `(let [model# ~(nth form 1)
         data# ~(nth form 2)
         result# (valid? model# data#)
         data-rep# (get-in data# [:body :key]
                           (if (and (contains? data# :bindings)
                                    (= :let (:type data#)))
                             :model-data
                             data#))
         model-name# (if (= :ref (get-in model# [:body :type]))
                       (keyword (get-in model# [:body :key]))
                       "[NULL]")]
     (t/do-report
      {:type (if result# :pass :fail)
       :message ~msg
       :expected (str (with-out-str (clojure.pprint/pprint data-rep#))
                      " conforms to model for "
                      model-name#)
       :actual result#})
     result#))

(t/deftest transforms

  (t/testing "Paragraph detection"
    (t/is (= [:div [:p "some"] [:p "text"]]
             (detect-paragraphs [:div "some\n\ntext"] #"\n\n")))

    (t/is (=
           [:div [:p "some"] [:p "text" [:em "with emphasis"]]]
           (detect-paragraphs [:div "some\n\ntext" [:em "with emphasis"]]
                              #"\n\n")))

    (t/is (= [:div] (detect-paragraphs [:div " "] #"\n\n"))
          "Whitespace-only text should not be tokenized into paragraphs")

    (t/is
     (=
      [:div
       {:class "1col"}
       [:p "Linked in the comments on Truyers' post was "
        [:code {:class "ws-normal navy"} "noms"]
        ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
        [:code {:class "ws-normal navy"} "noms"]
        " DB alongside the code that is affected by that configuration in a way that reliably links the two."]]
      (detect-paragraphs
       [:div
        {:class "1col"}
        "\n\nLinked in the comments on Truyers' post was "
        [:code {:class "ws-normal navy"} "noms"]
        ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
        [:code {:class "ws-normal navy"} "noms"]
        " DB alongside the code that is affected by that configuration in a way that reliably links the two."]
       #"\n\n")))


    (t/is
     (=
      [:div
       {:class "row"}
       [:p "orphan text" [:em "with emphasis added"] "and"]
       [:p "linebreak"]]
      (detect-paragraphs
       [:div
        {:class "row"}
        "orphan text"
        [:em "with emphasis added"]
        "and\n\nlinebreak"] #"\n\n"))))


  (t/testing "Sectionizer"
    (t/is (=
           [:article
            [:section
             [:p "Some text"]
             [:p "Some more text"]]]
           (into [:article]
                 sectionize-contents
                 [[:p "Some text"]
                  [:p "Some more text"]])))
    (t/is (= [:article
              [:section
               [:p "text"
                [:q "a quote"]]]
              [:section
               [:p "more text"]]]
             (into [:article]
                   sectionize-contents
                   [[:section]
                    [:p "text"]
                    [:q "a quote"]
                    [:section]
                    [:p "more text"]])))

    (t/is (= [:article
              [:section
               [:p "text"
                [:q "a quote"]]]
              [:section
               [:p "more text"]]]
             (into [:article]
                   sectionize-contents
                   [[:p "text"]
                    [:q "a quote"]
                    [:section]
                    [:p "more text"]])))

    (t/is (= [:article
              [:section
               [:p "text"
                [:q "a quote"]]]
              [:section
               [:div {:class "subsection"}
                [:h3 "Subsection 1"]
                [:p "subsection 1 text"]]
               [:div {:class "subsection"}
                [:h3 "Subsection 2"]
                [:p "subsection 2 text"]]]]
             (into [:article]
                   sectionize-contents
                   [[:p "text"]
                    [:q "a quote"]
                    [:section]
                    :next
                    [:div {:class "subsection"}]
                    [:h3 "Subsection 1"]
                    [:p "subsection 1 text"]
                    :next
                    [:div {:class "subsection"}]
                    [:h3 "Subsection 2"]
                    [:p "subsection 2 text"]]))))

  (t/testing "Next Processing"
    (t/is
     (=
      [:div [:p "test" [:a "something"]] [:div "something else"]]
      (process-nexts [:div :next [:p] "test" [:a "something"] :next [:div] "something else"])))
    (t/is
     (=
      [:div [:p "test" [:a "something"]] [:div "something else"]]
      (process-nexts [:div [:p "test" [:a "something"]] [:div "something else"]])))
    (t/is
     (=
      [:div [:p "test"]]
      (process-nexts [:div [:p "test"]])))
    (t/is
     (=
      [:div]
      (process-nexts [:div])))))

(defn finalize-elem-model
  "Return a constrained version of the given element's model such that it has no child elements"
  [elem]
  (h/in-vector
   (h/cat
    [:tag (h/val elem)]
    [:attributes (h/? global-attributes)]
    [:contents (h/repeat 1 10 (h/not-inlined atomic-element))])))

(defn one-level-deep
  "Return a version of the given elements that can have a single level of nesting."
  [elem child-elems]
  (h/in-vector
   (h/cat
    [:tag (h/val elem)]
    [:attributes (h/? global-attributes)]
    [:contents
     (h/repeat 1 15
               (h/not-inlined
                (apply h/alt
                       atomic-element
                       (map finalize-elem-model
                            child-elems))))])))



(def example-forms
  "Some forms used to test the validity of the HTML models"
  {:a [:a {:href "http://www.archive.org"} "a link"]
   :data [:data {:value "0311ab"} "A sample post"]
   :del [:del "some deleted text"]
   :dl [:dl {:id "definitions"}
        [:dt ":dl - Definition List"]
        [:dd "A HTML element with a list of definitions"]]
   :figure [:figure [:figcaption "a picture"]
            [:img {:src "/some-picture.png"}]]
   :ul [:ul [:li "some text"] [:li "more text"]]
   :bdo [:bdo {:dir "rtl"} "right to left text"]
   :time [:time {:datetime "2020-12-31"}]
   :img [:img {:src "/sample.jpg"}]
   :span [:span [:img {:src "/sample.jpg"}]]
   :q [:q {:cite "Anonymous"} "If you can't convince, confuse!"]
   :script [:script {:src "/resources/klipse.js"} ""]
   :wbr [:wbr]
   :hr [:hr]
   :br [:br]
   #_ #_
   :table [:table
           [:caption "an example table"]
           [:colgroup [:col]]
           [:tr [:td "a cell"]]]
   :article [:article [:section "something"]]})

(t/deftest models
  (t/testing "model constructors"

    (t/is (valid-model
           minimap-model
           (->hiccup-model :p global-attributes
                           (h/* atomic-element))))

    (t/is (valid-model
           minimap-model
           (->hiccup-model :p global-attributes
                           (h/* (apply h/alt [:atomic-element atomic-element]
                                       [])))))

    (t/is (valid-model minimap-model
                       (->hiccup-model
                        :em [])))

    (t/is (valid-model minimap-model
                       (h/let ['em (->hiccup-model :em [])]
                         (->hiccup-model
                          :h2
                          [[:em (h/ref 'em)]]))))

    (t/is (valid-model minimap-model
                       (h/let ['em (->hiccup-model :em [])]
                         (->hiccup-model
                          :h2
                          (map elem-ref #{:em})))))

    (t/is (valid-model minimap-model
                       (h/let ['em (->hiccup-model :em [])
                               'h2 (->hiccup-model
                                    :h2
                                    (map elem-ref #{:em}))]
                         (h/alt [:em (h/ref 'em)]
                                [:h2 (h/ref 'h2)]))))

    (t/is
     (valid-model (->hiccup-model :col global-attributes :empty)
                  [:col])))



  (t/testing "content models"

    (t/is
     (valid-model
      minimap-model
      (h/in-vector (h/cat
                    [:tag (h/val :img)]
                    [:attributes
                     (-> global-attributes
                         (h/with-entries [:src url])
                         (h/with-optional-entries
                           [:alt string-gen]
                           [:sizes string-gen]
                           [:width (->constrained-model #(< 0 % 8192) gen/small-integer)]
                           [:height (->constrained-model #(< 0 % 8192) gen/small-integer)]
                           [:loading (h/enum #{"eager" "lazy"})]
                           [:decoding (h/enum #{"sync" "async" "auto"})]
                           [:crossorigin (h/enum #{"anonymous" "use-credentials"})]))]))))

    (t/is
     (valid-model
      minimap-model
      (h/in-vector (h/cat [:tag (h/val :hr)] [:attributes (h/? global-attributes)]))))

    (t/is (valid-model minimap-model elements))

    (t/is (valid-model
           (->hiccup-model :p global-attributes
                           (h/* atomic-element))
           [:p {:id "something"} "text in a paragraph"]))

    (t/is (valid-model (->element-model :p)  [:p "something" [:a {:href "link"} "text"]])
          "Phrasing subtags should be respected.")

    (t/is (valid-model (->element-model :a) [:a {:href "something"} [:ins "something" [:del "something" [:em "something else"]]]])
          "Phrasing subtags should be respected.")

    (t/is (valid-model (->element-model :ins) [:ins [:ins [:ins [:em "text"]]]])
          "Phrasing subtags should be respected")

    (t/is (valid-model (->element-model :ins) [:ins [:ins "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-model (->element-model :del) [:del [:em "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-model (->element-model :em) [:em [:ins [:ins [:em "text"]]]])
          "Phrasing subtags should be respected")

    (t/is (valid-model (->element-model :em) [:em [:a {:href "link"}] "something"])
          "Phrasing subtags should be respected")

    (t/is (not (valid? (->element-model :ins) [:ins [:ins [:ins [:p "text"]]]])))

    (t/is (valid-model (->element-model :a)
                       [:a {:href "something"} "link" [:em "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-model (->element-model :p)
                       [:p "text" [:img {:src "/picture.jpg"}]]))

    (doseq [elem (filter #(not (= % (symbol :phrasing-content)))
                         (-> elements :bindings keys))]
      (t/testing (str "model for element: <" elem ">")
        (t/is (valid-model minimap-model (->element-model (keyword elem))))
        (let [data (get example-forms (keyword elem) [(keyword elem) "sample string"])]
          (t/is (valid-model (->element-model (keyword elem)) data)))))

    (t/is (palpable? [:p "text"]))
    (t/is (not (palpable? [:p])))

    (t/is (valid-model flow-content [:div [:div [:div [:p "text"]]]]))
    (t/is (valid-model phrasing-content-m [:em "something"]))
    ;; h/with-condition isn't working on this?
    ;; (t/is (valid-model (->element-model :phrasing-content) [:em]))
    )


  (t/testing "full structure"
    (doseq [[k v] example-forms]
      (t/testing (str "model for element: <" (symbol k) ">")
                 (t/is (valid-model elements v))))

    (comment
      (map (fn [[k v]] [k (valid-model elements v)]) example-forms)

      ))

  (t/testing "atomic elements"

    (t/is (valid-model minimap-model atomic-element))

    (t/is (valid-model global-attributes {:class "a"
                                          :href "http://google.com"}))
    (t/is (valid-model global-attributes {:title "some page"
                                          :href "/relative-page.html"}))

    (t/is (valid-model (->element-model :img)
                       [:img {:src "/pic.jpg" :width 500}]))))

(comment
  (-> sample-multi-form-input
      (parse-eval [:r-grid {:columns 8}])
      process-text

      )

  )

;; (def simple-grid-cell-model
;;   "A version of grid cells that has a limit on how deeply nested the forms can be."
;;   (h/in-vector
;;    (h/cat
;;     [:tag (h/val :r-cell)]
;;     [:attributes (h/? (h/with-optional-entries global-attributes [:span raster-span]))]
;;     [:contents
;;      (h/repeat
;;       1 25
;;       (h/not-inlined
;;        (apply h/alt
;;               (map
;;                (fn [e] [e (one-level-deep e)]) (keys doc-tree)))))])))

;; (def simple-grid-model
;;   "A version of the grid without mutually recursive child refs, for testing by induction."
;;   (h/in-vector
;;    (h/cat
;;     [:tag (h/val :r-grid)]
;;     [:attributes
;;      (h/with-entries global-attributes
;;        [:columns (->constrained-model #(< 0 % 33) gen/small-integer 250)])]
;;     [:contents
;;      (h/repeat 1 25
;;                (h/not-inlined simple-grid-cell-model))])))

(defspec atomic-forms 250
  (prop/for-all
   [p (mg/gen atomic-element)]
   (valid? atomic-element p)))

(defspec elem-attributes 250
  (prop/for-all
   [a (mg/gen global-attributes)]
   (valid? global-attributes a)))

;; (defspec grid-cell-forms 25
;;   (prop/for-all
;;    [c (mg/gen simple-grid-cell-model)]
;;    (valid? simple-grid-cell-model c)))

;; (defspec grid-forms 25
;;   (prop/for-all
;;    [g (mg/gen simple-grid-model)]
;;    (valid-model simple-grid-model g)))


(comment

  (gen/sample (mg/gen raster-span) 20)

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell {:span "row"} [:p "some text"]]])

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell [:p "some text"]]])
  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell [:p "some text"]]])

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell]])
  (gen/sample (mg/gen global-attributes) 15)

  (gen/sample (mg/gen simple-grid-model) 25)

  (valid? simple-grid-model [:r-grid {:columns 1} [:r-cell {:span "5+4"} [:h6 1.0]]])

  )


;; (defspec renders-correctly
;;   25
;;   (prop/for-all
;;    [g (mg/gen article)]
;;    (and (valid? article g) (string? (html g)))))

(def sentences (-> "./resources/building-with-earth-excerpt.txt"
                   slurp
                   (clojure.string/split #"\.")))

;; (defspec parse-rewrite-render
;;   25
;;   (prop'/for-all
;;    [t (gen/elements sentences)
;;     basic-render-str (gen/fmap #(str "<%=(" % " \"" t "\")%>")
;;                                (gen/elements #{"ul" "ol" "em"}))]
;;    (do (load-deps)
;;        (and (valid? grid  (-> t
;;                               (parse-eval [:r-grid {:columns 8}])
;;                               process-text))
;;             (valid? grid (-> basic-render-str
;;                              (parse-eval [:r-grid {:columns 8}])
;;                              process-text))))))




(t/deftest existing-docs
  (t/testing "parsing and rendering existing pages"
    (let [pages (get-template-files "./content" ".ct")
          parsed-pages
          (into {}
                (map
                 (fn [p]
                   (try
                     [p (-> p
                            slurp
                            (parse-eval [:article]
                                        (md5 p)
                                        '[[respatialized.render :refer :all]])
                            (#(into [:article] sectionize-contents (rest %))))]
                     (catch Exception e
                       [p {:error (str "exception: " (.getMessage e))}])))
                 pages))]
      (doseq [[page contents] parsed-pages]
        (t/is (valid-model (->element-model :article) contents)
              (str "page " page " did not conform to document spec"))))))

(comment
  (def pages (get-template-files "./content" ".ct"))

  (def post-contents
    (into {}
                (map
                 (fn [p]
                   (try
                     [p (-> p
                            slurp
                            (parse-eval [:article]
                                        (md5 p)
                                        '[[respatialized.render :refer :all]])
                            (#(into [:article] sectionize-contents (rest %))))]
                     (catch Exception e
                       [p {:error (str "exception: " (.getMessage e))}])))
                 pages)))

  (def post-meta
    (into {} (map (fn [[p c]] [p {:valid? (valid? grid c)
                                  :size (count c)}]) post-contents))))
