(ns respatialized.document-test
  (:require [respatialized.document :as doc :refer :all]
            [respatialized.parse :as parse :refer [parse parse-eval md5]]
            [respatialized.build :refer [get-template-files]]
            [respatialized.render :refer [template->hiccup]]
            [clojure.zip :as zip]
            [clojure.set :as set]
            [clojure.test :as t]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.gfredericks.test.chuck.properties :as prop']
            [malli.core :as m :refer [validate]]
            [malli.error :as me]
            [malli.generator :as mg])
  (:import [java.lang Exception]))

(defmethod t/assert-expr 'valid-schema? [msg form]
  `(let [model# ~(nth form 1)
         data# ~(nth form 2)
         result# (m/validate model# data#)
         model-name# (last model#)]
     (t/do-report
      {:type (if result# :pass :fail)
       :message ~msg
       :expected (str (with-out-str (clojure.pprint/pprint data#))
                      " conforms to schema for "
                      model-name#)
       :actual  (if (not result#)
                   (m/explain model# data#)
                  result#)})
     result#))

(t/deftest transforms

  (t/testing "Paragraph detection"
    (t/is (= [:div [:p "some"] [:p "text"]]
             (detect-paragraphs [:div "some\n\ntext"] #"\n\n")))

    (t/is (=
           [:div [:p "some"] [:p "text" [:em "with emphasis"]]]
           (detect-paragraphs [:div "some\n\ntext" [:em "with emphasis"]]
                              #"\n\n")))

    #_ (t/is (= [:div] (detect-paragraphs [:div " "] #"\n\n"))
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

    (t/is (=
           [:article
            [:section
             [:p "Some text"]
             [:p "Some more text"]]
            [:section
             [:p [:q "a quote"]]]]
           (into [:article]
                 sectionize-contents
                 [[:p "Some text"]
                  [:p "Some more text"]
                  [:section]
                  [:q "a quote"]])))

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
               [:header [:h1 "section header"]]
               [:p "more text"]]]
             (into [:article]
                   sectionize-contents
                   [[:section]
                    [:p "text"]
                    [:q "a quote"]
                    [:section
                     [:header [:h1 "section header"]]]
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
   :head [:head [:title "a page"] [:script {:type "text/javascript" :src "/intro.js"}] [:style "h1 {size: 3rem}"]]
   :span [:span [:img {:src "/sample.jpg"}]]
   #_ #_:script [:script {:type "text/javascript" :src "code.js"}]
   :q [:q {:cite "Anonymous"} "If you can't convince, confuse!"]
   :script [:script {:src "/resources/klipse.js" :type "text/javascript"} ""]
   #_ #_ :wbr [:wbr]
   :hr [:hr]
   :br [:br]
   :abbr [:abbr {:title "ACME Corporation"} "ACME"]
   :ol [:ol [:li "item"] [:li "item2"]]
   :hgroup [:hgroup [:h1 "big header"] [:h4 "small header"]]
   :link [:link {:rel "stylesheet" :href "/main.css"}]
   :details [:details [:summary [:span "summarized text"]] [:p "text"]]
   :table [:table
           [:caption "an example table"]
           [:colgroup [:col]]
           [:thead [:tr [:td "label"]]]
           [:tbody [:tr [:td "a cell"]]]]
   :article [:article [:section "something"]]})

(t/deftest models
  (t/testing "model constructors"

    #_(t/is (valid-model
             minimap-model
             (->hiccup-model :p global-attributes
                             (h/* atomic-element))))

    #_(t/is (valid-model
             minimap-model
             (->hiccup-model :p global-attributes
                             (h/* (apply h/alt [:atomic-element atomic-element]
                                         [])))))

    #_(t/is (valid-model minimap-model
                         (->hiccup-model
                          :em [])))

    #_(t/is (valid-model minimap-model
                         (h/let ['em (->hiccup-model :em [])]
                           (->hiccup-model
                            :h2
                            [[:em (h/ref 'em)]]))))

    #_(t/is (valid-model minimap-model
                         (h/let ['em (->hiccup-model :em [])]
                           (->hiccup-model
                            :h2
                            (map elem-ref #{:em})))))

    #_(t/is (valid-model minimap-model
                         (h/let ['em (->hiccup-model :em [])
                                 'h2 (->hiccup-model
                                      :h2
                                      (map elem-ref #{:em}))]
                           (h/alt [:em (h/ref 'em)]
                                  [:h2 (h/ref 'h2)]))))

    #_(t/is
       (valid-model (->hiccup-schema :col global-attributes nil)
                    [:col])))



  (t/testing "content models"

    #_(t/is
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

    #_(t/is
       (valid-model
        minimap-model
        (h/in-vector (h/cat [:tag (h/val :hr)] [:attributes (h/? global-attributes)]))))

    #_(t/is (valid-model minimap-model elements))

    (t/is (valid-schema?
           (->hiccup-schema :p global-attributes
                            [:* atomic-element])
           [:p {:id "something"} "text in a paragraph"]))

    (t/is (valid-schema? (subschema html :respatialized.document/p)
                         [:p "something" [:a {:href "https://link.com"} "text"]])
          "Phrasing subtags should be respected.")

    (t/is (valid-schema?
           (subschema html "a-phrasing")
           [:a {:href "https://something.com"} [:ins "something" [:del "something" [:em "something else"]]]])
          "Phrasing subtags should be respected.")

    (t/is (valid-schema?
           (subschema html "ins-phrasing")
           [:ins [:ins [:ins [:em "text"]]]])
          "Phrasing subtags should be respected")

    (t/is (valid-schema?
           (subschema html "ins-phrasing")
           [:ins [:ins "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-schema?
           (subschema html "del-phrasing")
           [:del [:em "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-schema?
           (subschema html :respatialized.document/em)
           [:em [:ins [:ins [:em "text"]]]])
          "Phrasing subtags should be respected")

    (t/is (valid-schema?
           (subschema html :respatialized.document/em)
           [:em [:a {:href "https://archive.org"}] "something"])
          "Phrasing subtags should be respected")

    (t/is (not (m/validate
                (subschema html "ins-phrasing")
                [:ins [:ins [:ins [:p "text"]]]])))

    (t/is (valid-schema?
           (subschema html "a-phrasing")
           [:a {:href "https://example.com"} "link" [:em "text"]])
          "Phrasing subtags should be respected")

    (t/is (valid-schema?
           (subschema html :respatialized.document/p)
           [:p "text" [:img {:src "/picture.jpg"}]]))

    (t/is (valid-schema?
           (subschema html :respatialized.document/em)
           [:em "text" [:br] "more text"]))

    ;; (t/is (valid-model (->element-model :element) '([:em [:br] "text"])))

    (doseq [elem (set/union flow-tags phrasing-tags heading-tags)]
      (t/testing (str "model for element: <" (name elem) ">")
        (let [data (get example-forms elem
                        [elem "sample string"])
              schema (subschema
                      html (ns-kw 'respatialized.document elem))]
          (t/is (valid-schema? schema data)))))

    (t/is (palpable? [:p "text"]))
    (t/is (not (palpable? [:p])))

    (t/is (valid-schema? (subschema html ::doc/element) [:div [:div [:div [:p "text"]]]]))
    ;; (t/is (valid-model element-m [:em "something"]))
    ;; h/with-condition isn't working on this?
    ;; (t/is (valid-model (->element-model :element) [:em]))
    )


  (t/testing "example forms"
    (doseq [[k v] example-forms]
      (let [schema (subschema html (ns-kw 'respatialized.document k))]
        (t/testing (str "model for element: <" (symbol k) ">")
          (t/is (valid-schema? schema v))))))

  (comment
      (map (fn [[k v]] [k (valid-schema? htmls v)]) example-forms)

      )

  (t/testing "atomic elements"


    (t/is (m/validate global-attributes {:class "a"
                                         :href "http://google.com"}))
    (t/is (m/validate global-attributes {:title "some page"
                                         :href "/relative-page.html"}))

    (t/is (valid-schema? (subschema
                          html
                          :respatialized.document/img)
                         [:img {:src "/pic.jpg" :width 500}]))))

(comment
  (-> sample-multi-form-input
      (parse-eval [:r-grid {:columns 8}])
      process-text

      )

  )


(defspec atomic-forms 250
  (prop/for-all
   [p (mg/generator atomic-element)]
   (validate atomic-element p)))

(defspec elem-attributes 250
  (prop/for-all
   [a (mg/generator global-attributes)]
   (validate global-attributes a)))

;; (defspec grid-cell-forms 25
;;   (prop/for-all
;;    [c (mg/gen simple-grid-cell-model)]
;;    (valid? simple-grid-cell-model c)))

;; (defspec grid-forms 25
;;   (prop/for-all
;;    [g (mg/gen simple-grid-model)]
;;    (valid-model simple-grid-model g)))


(comment


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

  (def simple-grid-cell-model
    "A version of grid cells that has a limit on how deeply nested the forms can be."
    (h/in-vector
     (h/cat
      [:tag (h/val :r-cell)]
      [:attributes (h/? (h/with-optional-entries global-attributes [:span raster-span]))]
      [:contents
       (h/repeat
        1 25
        (h/not-inlined
         (apply h/alt
                (map
                 (fn [e] [e (one-level-deep e)]) (keys doc-tree)))))])))

  (def simple-grid-model
    "A version of the grid without mutually recursive child refs, for testing by induction."
    (h/in-vector
     (h/cat
      [:tag (h/val :r-grid)]
      [:attributes
       (h/with-entries global-attributes
         [:columns (->constrained-model #(< 0 % 33) gen/small-integer 250)])]
      [:contents
       (h/repeat 1 25
                 (h/not-inlined simple-grid-cell-model))])))


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
          _ (respatialized.build/load-deps)
          parsed-pages
          (into {}
                (map
                 (fn [p]
                   (try
                     [p (-> p slurp template->hiccup)]
                     (catch Exception e
                       [p {:error (str "exception: " (.getMessage e))}])))
                 pages))]
      (doseq [[page content] parsed-pages]
        (t/is
         (m/validate html content)
         (str "page " page " did not conform to <html> spec"))))))

(comment
  (def pages (get-template-files "./content" ".ct"))

  (respatialized.build/load-deps)

  (def parsed-post-contents
    (into {} (map (fn [p] [p (-> p
                                 slurp
                                 parse/parse
                                 (parse/eval-with-errors
                                  (symbol (str "tmp-"
                                               (Math/abs (hash p))))
                                  validate-element))])
                  pages)))

  (defn find-invalid [contents]
    (filter #(false? (first %))
            (map (fn [e] [(element? e) e]) contents)))

  (def post-contents
    (into {}
          (map
           (fn [p]
             (try
               [p (-> p slurp template->hiccup)]
               (catch Exception e
                 [p {:error (str "exception: " (.getMessage e))}])))
           pages)))

  (def post-meta
    (into {} (map (fn [[p c]] [p {:valid? (valid? (->element-model :article) c)
                                  :size (count c)}]) post-contents)))



  (defn zip-walk [f z]
    (if (zip/end? z)
      (zip/root z)
      (recur f (zip/next (f z)))))

  (def forms
    (zip-walk (fn [e] {:elem e
                       :status
                       (and
                        (vector? e)
                        (keyword? (first e))
                        (valid? (->element-model (first e)) e))})
              (form-zipper (get post-contents "./content/against-metadata.html.ct"))))


  )
