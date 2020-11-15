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
            [clojure.test.check.generators :as gen]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.test.check.properties :as prop]
            [minimallist.core :refer [valid?]]
            ))

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
      (detect-paragraphs sample-code-form #"\n\n")))

    ))

(t/deftest models
  (t/testing "model primitives"
    (t/is (valid? attr-map {:class "a"
                            :href "http://google.com"}))
    (t/is (valid? attr-map {:title "some page"
                            :href "/relative-page.html"}))

    (t/is (valid? respatialized.document/image
                  [:img {:src "/pic.jpg" :width 500}])))

  (t/testing "structural forms"
    (t/is (valid? grid [:r-grid {:columns 8}]))
    (t/is (valid? grid [:r-grid {:columns 8} [:r-cell {:span "row"} [:p "some text"]]]))))

(def renders-correctly
  (prop/for-all
   [g (spec/gen :respatialized.document/grid)]
   (string? (html g))))

(binding [spec/*recursion-limit* 1]
    (st/instrument 'respatialized.document/process-text)
    (st/check 'respatialized.document/process-text)

    (tc/quick-check 5 renders-correctly))

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
                 (gen/vector gen/string-alphanumeric 1 10))
      ))

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
   20)

  )
