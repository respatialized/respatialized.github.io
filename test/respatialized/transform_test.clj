(ns respatialized.transform-test
  (:require [respatialized.transform :refer :all]
            [respatialized.parse :refer [parse parse-eval]]
            [respatialized.build :refer [load-deps]]
            [hiccup.core]
            [clojure.zip :as zip]
            [clojure.test :as t]))

(load-deps)

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

(def sample-code-form
  [:r-cell
   {:span "row"}
   "\n\nLinked in the comments on Truyers' post was "
   [:code {:class "ws-normal navy"} "noms"]
   ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
   [:code {:class "ws-normal navy"} "noms"]
   " DB alongside the code that is affected by that configuration in a way that reliably links the two."])

(def orphan-zip
  (zip/zipper not-in-form? identity (fn [_ c] c) (first orphan-trees)))

(def orphan-zip-2
  (zip/zipper not-in-form? identity (fn [_ c] c) (second orphan-trees)))

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

    (t/is (= '(:div
               ([:div
                 {:class "f3"}
                 [:a
                  {:href "https://github.com/attic-labs/noms"}
                  ("Noms: The Versioned, Forkable, Syncable Database")]])
               [:r-cell
                {:span "row"}
                [[:p "Linked in the comments on Truyers' post was "
                  [:code {:class "ws-normal navy"} "noms"]
                  ", a database directly inspired by Git's decentralized and immutable data model, but designed from the ground up to have a better query model and more flexible schema. Unfortunately, it seems to be unmaintained and not ready for prime time. Additionally, for the use case I'm describing, it's unclear how to effectively distribute the configuration data stored in a "
                  [:code {:class "ws-normal navy"} "noms"]
                  " DB alongside the code that is affected by that configuration in a way that reliably links the two."]]])
             (-> sample-text parse-eval process-text)))

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
