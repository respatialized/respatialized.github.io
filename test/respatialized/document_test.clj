(ns respatialized.document-test
  (:require [respatialized.document :refer :all]
            [respatialized.parse :refer [parse parse-eval]]
            [respatialized.build :refer [load-deps]]
            [hiccup.core :refer [html]]
            [clojure.zip :as zip]
            [clojure.spec.alpha :as spec]
            [clojure.test :as t]
            [clojure.spec.test.alpha :as st]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.test.check.properties :as prop]
            [minimallist.core :refer [valid? explain]]
            [minimallist.generator :as mg]
            [minimallist.helper :as h]))

;; (load-deps)

(def sample-multi-form-input
  "first paragraph\n\nsecond paragraph with <%=[:em \"emphasis\"]%> text")

(def orphan-trees
  '([:r-grid
     "orphan text" [:em "with emphasis added"]
     [:r-cell "non-orphan text"]]
    [:r-grid
     "orphan text" [:em "with emphasis added"] "and\n\nlinebreak"
     [:r-cell "non-orphan text\n\nwith linebreak"]]))

(def sample-text "<%=[:div {:class \"f3\"} (link \"https://github.com/attic-labs/noms\" \"Noms: The Versioned, Forkable, Syncable Database\")]%>\n\nLinked in the comments on Truyers' post was <%=(in-code \"noms\")%>, a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a <%=(in-code \"noms\")%> DB alongside the code that is affected by that configuration in a way that reliably links the two.")

(def sample-front-matter [:div
                          "\n\n"
                          [:r-cell {:span "row", :class "b"} [:h1 "Against Metadata"]]
                          "\n\n"
                          [:r-cell {:span "row", :class "b"} [:h4 "Frustrations with YAML"]]
                          "\n"
                          [:div {:class "f4"} "2019-08-16"]])

(def sample-code-form
  [:r-cell
   {:span "row"}
   "\n\nLinked in the comments on Truyers' post was "
   [:code {:class "ws-normal navy"} "noms"]
   ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
   [:code {:class "ws-normal navy"} "noms"]
   " DB alongside the code that is affected by that configuration in a way that reliably links the two."])

(def orphan-zip
  (form-zipper (first orphan-trees)))

(def orphan-zip-2
  (form-zipper (second orphan-trees)))

(t/deftest transforms

  (t/testing "zippers"
    (t/is (=
           [:r-grid [:p "orphan text"
                     [:em "with emphasis added"]]]
           (group-orphans [:p]
                          already-tokenized?
                          [:r-grid "orphan text"
                           [:em "with emphasis added"]])))

    (t/is
     (=
      [:r-grid [:p "orphan text"
                [:em "with emphasis added"]]
       [:r-cell "non-orphan text"]]
      (group-orphans [:p]
                     #(and (vector? %) (contains? #{:p :r-cell} (first %)))
                     [:r-grid "orphan text"
                      [:em "with emphasis added"]
                      [:r-cell "non-orphan text"]])))

    (t/is (=
           [:r-grid
            [:r-cell {:span "row"} "orphan text"
             [:em "with emphasis added"]]
            [:r-cell "non-orphan text"]]
           (-> orphan-zip get-orphans zip/node)))

    (t/is (= [:a "b" "c" :d "e" "f"]
             (split-strings [:a "b\n\nc" :d "e" "f"] #"\n\n")))

    (t/is (=
           [:r-cell [:p "some"] [:p "text" [:em "with emphasis"]]]
           (detect-paragraphs [:r-cell "some\n\ntext" [:em "with emphasis"]]
                              #"\n\n")))

    (t/is
     (=
      [:r-cell
       {:span "row"}
       [:p "orphan text" [:em "with emphasis added"] "and"]
       [:p "linebreak"]]
      (detect-paragraphs [:r-cell
                          {:span "row"}
                          "orphan text"
                          [:em "with emphasis added"]
                          "and\n\nlinebreak"] #"\n\n")))

    (t/is (=
           [:r-grid
            [:r-cell {:span "row"}
             [:p "orphan text"
              [:em "with emphasis added"] "and"]
             [:p "linebreak"]]
            [:r-cell [:p "non-orphan text"]
             [:p "with linebreak"]]]
           (-> orphan-zip-2
               get-orphans
               zip/node
               form-zipper
               tokenize-paragraphs
               zip/node)))

    (t/is (vector?
           (-> sample-front-matter
               form-zipper
               tokenize-paragraphs
               zip/node)))

    (t/is (vector?
           (-> [:r-cell [:h1 "some text"]]
               form-zipper
               tokenize-paragraphs
               zip/node)))

    (t/is (vector?
           (-> [:div [:r-cell "some text"]]
               form-zipper
               tokenize-paragraphs
               zip/node)))

    (t/is (=
           [:r-grid
            [:r-cell {:span "row"}
             [:p "orphan text"
              [:em "with emphasis added"] "and"]
             [:p "linebreak"]]
            [:r-cell [:p "non-orphan text"]
             [:p "with linebreak"]]]
           (-> [:r-grid
                [:r-cell
                 {:span "row"}
                 "orphan text"
                 [:em "with emphasis added"]
                 "and\n\nlinebreak"]
                [:r-cell "non-orphan text\n\nwith linebreak"]]
               form-zipper
               tokenize-paragraphs
               zip/node)))

    ;; (t/is (= [:div
    ;;           '([:div
    ;;              {:class "f3"}
    ;;              [:a
    ;;               {:href "https://github.com/attic-labs/noms"}
    ;;               "Noms: The Versioned, Forkable, Syncable Database"]])
    ;;           [:r-cell
    ;;            {:span "row"}
    ;;            [:p "Linked in the comments on Truyers' post was "
    ;;             [:code {:class "ws-normal navy"} "noms"]
    ;;             ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
    ;;             [:code {:class "ws-normal navy"} "noms"]
    ;;             " DB alongside the code that is affected by that configuration in a way that reliably links the two."]]]
    ;;          (-> sample-text parse-eval process-text)))

    (t/is
     (=
      [:r-cell
       {:span "row"}
       [:p "Linked in the comments on Truyers' post was "
        [:code {:class "ws-normal navy"} "noms"]
        ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
        [:code {:class "ws-normal navy"} "noms"]
        " DB alongside the code that is affected by that configuration in a way that reliably links the two."]]
      (-> sample-code-form form-zipper tokenize-paragraphs zip/node)))

    (t/is
     (=
      [:r-cell
       {:span "row"}
       [:p "Linked in the comments on Truyers' post was "
        [:code {:class "ws-normal navy"} "noms"]
        ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
        [:code {:class "ws-normal navy"} "noms"]
        " DB alongside the code that is affected by that configuration in a way that reliably links the two."]]
      (detect-paragraphs sample-code-form #"\n\n")))))

(defn finalize-elem-model
  "Return a constrained version of the given element's model such that it has no child elements"
  [elem]
  (h/in-vector
   (h/cat
    [:tag (h/val elem)]
    [:attributes (h/? attr-map)]
    [:contents (h/repeat 1 10 (h/not-inlined atomic-element))])))

(defn one-level-deep
  "Return a version of the given elements that can have a single level of nesting."
  [elem]
  (h/in-vector
   (h/cat
    [:tag (h/val elem)]
    [:attributes (h/? attr-map)]
    [:contents
     (h/repeat 1 15
               (h/not-inlined
                (apply h/alt
                       atomic-element
                       (map finalize-elem-model
                            (get doc-tree elem)))))])))

(t/deftest models
  (t/testing "atomic elements"
    (t/is (valid? attr-map {:class "a"
                            :href "http://google.com"}))
    (t/is (valid? attr-map {:title "some page"
                            :href "/relative-page.html"}))

    (t/is (valid? respatialized.document/image
                  [:img {:src "/pic.jpg" :width 500}])))

  (t/testing "structural forms"
    (t/is (valid? grid [:r-grid {:columns 8}]))
    (t/is (valid? grid [:r-grid {:columns 8} [:r-cell {:span "row"} [:p {:id "i"} "some text"]]]))
    (t/is (valid? grid [:r-grid {:columns 8} [:r-cell [:p {:id "i"} "some text"]]]))
    (t/is (valid? grid [:r-grid {:columns 8} [:r-cell {:span "1-2"} [:p {:id "i"} "some text"]]]))))

(def simple-grid-cell-model
  "A version of grid cells that has a limit on how deeply nested the forms can be."
  (h/in-vector
   (h/cat
    [:tag (h/val :r-cell)]
    [:attributes (h/? (h/with-optional-entries attr-map [:span raster-span]))]
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
     (h/with-entries attr-map
       [:columns (->constrained-model #(< 0 % 33) gen/small-integer 250)])]
    [:contents
     (h/repeat 1 25
               (h/not-inlined simple-grid-cell-model))])))

(defspec atomic-forms 250
  (prop/for-all
   [p (mg/gen atomic-element)]
   (valid? atomic-element p)))

(defspec span-forms 250
  (prop/for-all
   [s (mg/gen raster-span)]
   (valid? raster-span s)))

(defspec grid-cell-forms 250
  (prop/for-all
   [c (mg/gen simple-grid-cell-model)]
   (valid? simple-grid-cell-model c)))

(defspec grid-forms 250
  (prop/for-all
   [g (mg/gen simple-grid-model)]
   (valid? simple-grid-model g)))


(comment

  (gen/sample (mg/gen raster-span) 20)

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell {:span "row"} [:p "some text"]]])

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell [:p "some text"]]])
  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell [:p "some text"]]])

  (valid? simple-grid-model [:r-grid {:columns 8} [:r-cell]])
  (gen/sample (mg/gen attr-map) 15)

  (gen/sample (mg/gen simple-grid-model) 25)

  (valid? simple-grid-model [:r-grid {:columns 1} [:r-cell {:span "5+4"} [:h6 1.0]]])

  )

(defspec renders-correctly
  250
  (prop/for-all
   [g (mg/gen simple-grid-model)]
   (string? (html g))))


(t/deftest specs
  (binding [spec/*recursion-limit* 1]
    (st/instrument 'respatialized.document/process-text)
    (st/check 'respatialized.document/process-text)))

(defn test-terminal-elem? [i]
  (or (not (sequential? i))
      (and (not-any? sequential? i)
           (< (count i) 10))))

(spec/def ::in-form-elem-test
  (spec/or
   :header
   (spec/with-gen
     (spec/+
      (spec/and
       vector?
       header-pattern
       (spec/every test-terminal-elem?)))
     #(gen/fmap
       vec
       (gen/vector
        (spec/gen
         (spec/and
          header-pattern
          (spec/every test-terminal-elem?))) 1 10)))))

(spec/def ::kw-or-s-vec
  (spec/with-gen
    (spec/+ (spec/or :s string? :k keyword?))
    #(gen/vector
      (spec/gen (spec/or :s string? :k keyword?))
      1 10)))

(comment
  (process-text [:r-grid "orphan text"] [:r-cell {:span "1-6"}])

  (spec/def ::li-test
    (spec/with-gen
      :respatialized.document/li
      #(gen/fmap (fn [coll] (into [:li] coll))
                 (gen/vector gen/string-alphanumeric 1 10))))

  (spec/def ::ul-test
    (spec/cat :type #{:ul}
              :attr-map (spec/? :respatialized.document/attr-map)
              :items (spec/* ::li-test)))


  ;; this didn't work because such-that must create the example
  ;; before checking it against the predicate, which blows up the stack


  (gen/sample
   (gen/such-that #(every? test-terminal-elem? %)
                  (spec/gen :respatialized.document/in-form-elem)) 5)

  (spec/def ::kw-or-s-vec
    (spec/with-gen
      (spec/+ (spec/or :s string? :k keyword?))
      #(gen/vector
        (spec/gen (spec/or :s string? :k keyword?))
        1 10)))

  (gen/sample
   (spec/gen ::kw-or-s-vec)
   20))
