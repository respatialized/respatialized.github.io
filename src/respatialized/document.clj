(ns respatialized.document
  "Namespace for document processing."
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.zip :as zip]
   [clojure.data.finger-tree :as ftree :refer
    [counted-double-list ft-split-at ft-concat]]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [malli.core :as m]
   [malli.generator :as mg]
   [malli.registry :as mr]
   [malli.dot :as md]
   [malli.error :as me]
   [malli.util :as mu]))

(def block-level-tags
  "MDN list of block-level HTML element tags"
  #{:address :article :aside :blockquote
    :details :dialog :dd :div :dl :dt
    :fieldset :figcaption :figure :footer
    :form :h1 :h2 :h3 :h4 :h5 :h6 :header
    :hr :li :main :nav :ol :p :pre :section
    :table :ul})

(def inline-tags
  "MDN list of inline HTML element tags"
  #{:a :abbr :b :bdi :bdo :br #_:button
    :canvas :cite :code :data #_:datalist
    :del :dfn :em #_:embed :i #_:iframe :img
    #_:input :ins :kbd #_:label :mark #_:meter
    #_:noscript #_:object #_:output #_:picture
    #_:progress :q #_:ruby :s :samp :script
    #_:select #_:slot :small :span :strong
    :sub :sup #_:svg #_:template :time :u
    :tt :var #_:video :wbr})

(def metadata-tags
  "MDN list of metadata content element tags"
  #{:base :link :meta #_:noscript :script
    :style :title})

(def flow-tags
  "MDN list of flow content element tags"
  #{:a :abbr :aside :address :article #_:audio :b :bdo :bdi
    :blockquote :br #_:button #_:canvas :cite
    :code :data #_:datalist :del :details :dfn
    :div :dl :em #_:embed #_:fieldset :figure
    :footer #_:form :h1 :h2 :h3 :h4 :h5 :h6
    :header :hr :i #_:iframe :img #_:input :ins
    :kbd #_:label #_ :link :main #_:map :mark #_:math #_:menu
    #_:meter :nav #_:noscript #_:object :ol #_:output
    :p #_:picture :pre #_:progress :q #_:ruby :s
    :samp :script :section #_:select :small
    :span :strong :sub :sup #_:svg #_ :table
    #_:template #_:textarea :time :ul :var #_:video
    #_:wbr})

(def sectioning-tags
  "MDN list of sectioning content element tags"
  #{:article :aside :nav :section})

(def heading-tags
  "MDN list of heading content element tags"
  #{:h1 :h2 :h3 :h4 :h5 :h6 :hgroup})

(def phrasing-tags
  "MDN list of phrasing content element tags"
  #{:abbr #_:audio :b #_ :bdi :bdo :br #_:button #_:canvas :cite
    :code :data #_:datalist :dfn #_ :del :em #_:embed :i #_:iframe :img
    #_:input :kbd #_:label :mark #_:math #_:meter #_:noscript
    #_:object #_:output #_:picture #_:progress :q #_:ruby :s :samp
    :script #_:select :small :span :strong :sub :sup #_:svg
    #_:textarea :time :var #_:video #_ :wbr})

(def phrasing-subtags
  "MDN list of tags that are phrasing content when they contain only phrasing content."
  #{:a #_ :area :del :ins :link :map #_ :meta})

(def embedded-tags
  "MDN list of embedded content element tags"
  #{:audio :canvas #_:embed :iframe :img #_:math
    :object #_:picture :svg :video})

(def interactive-tags
  "MDN list of interactive content element tags"
  #{:a #_:button :details #_:embed #_:iframe #_:label
    #_:select #_:textarea})

(def transparent-tags
  "MDN list of transparent content tags"
  #{:ins :del :object})

(def external-link-pattern (re-pattern "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))
(def internal-link-pattern (re-pattern "/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))

#_(defn regex->model [re]
  (-> (h/fn #(and (string? %) (re-matches re %)))
      (h/with-test-check-gen
        (gen'/string-from-regex re))))

(def url
  [:orn
   [:external [:re external-link-pattern]]
   [:internal [:re internal-link-pattern]]])

#_(def string-gen (-> (h/fn string?) (h/with-test-check-gen gen/string)))
#_(def boolean-gen (-> (h/fn boolean?) (h/with-test-check-gen gen/boolean)))
#_(def int-gen (-> (h/fn int?) (h/with-test-check-gen gen/small-integer)))

(def global-attributes
  "MDN list of global HTML attributes as malli schema"
  (mu/optional-keys
   [:map
    [:class :string]
    [:contenteditable [:enum "true" "false" ""]]
    [:dir [:enum "ltr" "rtl" "auto"]]
    [:hidden :boolean]
    [:id :string]
    [:itemid :string]
    [:itemprop :string]
    [:itemref :string]
    [:itemscope :boolean]
    [:itemtype [:re external-link-pattern]]
    [:lang [:enum "en"]]
    [:tabindex :int]
    [:title :string]
    [:part :string]]))

(def atomic-element
 [:orn
  [:bool :boolean]
  [:decimal :double]
  [:integer :int]
  [:text :string]])


(defn has-reqd?
  "Checks to see if at least one entry in the given map schema has required keys"
  [schema]
  (let [[s-type & entries] (m/form schema)]
       (and
        (= :map s-type)
        (some (fn [e] (not (and (map? (second e))
                                (get (second e) :optional))))
              entries))))

(defn ->hiccup-schema [tag attr-model content-model]
  (let [head
        [:catn
         [:tag [:= tag]]
         [:attrs (if (has-reqd? attr-model)
                   attr-model
                   [:? attr-model])]]]
    (if (nil? content-model)
      [:and vector? head]
           (conj head [:contents content-model]))))

(defn ns-kw
  ([ns kw] (keyword (str ns) (str (name kw))))
  ([kw] (ns-kw *ns* kw)))

(defn ref-item [i] [:schema [:ref i]])

(comment

  (def sample-phrasing
    [:schema {:registry
              {"a-phrasing"
               [:catn
                [:tag [:= :a]]
                [:attributes [:? [:map-of keyword? any?]]]
                [:contents [:altn
                            [:atomic atomic-element]
                            [:node [:schema [:ref "phrasing-content"]]]]]]
               "phrasing-content"
               [:orn
                [:atomic-element atomic-element]
                [:node [:orn
                        [:a [:schema [:ref "a-phrasing"]]]
                        [:child [:schema [:ref "phrasing-content"]]]]]]
               "a"
               [:catn
                [:tag [:= :a]]
                [:attributes [:? [:map-of keyword? any?]]]
                [:contents [:* [:orn
                                [:phrasing [:schema [:ref "phrasing-content"]]]
                                [:flow [:schema [:ref "flow-content"]]]]]]]
               "flow-content"
               [:orn
                [:atomic-element atomic-element]
                [:node [:orn
                        [:a [:schema [:ref "a"]]]
                        [:child [:schema [:ref "flow-content"]]]]]]}}
     "flow-content"])

  ;; it works
  (m/validate sample-phrasing [:a {:href "https://google.com"} "text"])

  (mg/generate sample-phrasing-2 {:size 4})

  (def sample-phrasing-2
    [:schema
     {:registry
      {"a-phrasing" (->hiccup-schema
                     :a
                     (mu/merge global-attributes
                               [:map [:href url]])
                     [:* [:schema [:ref ::phrasing-content]]])
       ::em (->hiccup-schema
             :em
             global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::phrasing-content
       [:orn [:atomic-element atomic-element]
        [:node
         (apply
          conj
          [:orn
           [:a (ref-item "a-phrasing")]]
          (map (fn [t] [t (ref-item (ns-kw t))])
               #{:em}))]]}}
     ::phrasing-content])

  (m/validate sample-phrasing-2 [:a {:href "https://google.com"} "text"])

  (mg/generate sample-phrasing-2 {:size 4})

  )


(def element
    [:schema
     {:registry
      {"a-phrasing"
       (->hiccup-schema
        :a
        (mu/merge
         global-attributes
         [:map
          [:href url]
          [:download {:optional true} :string]
          [:rel {:optional true} :string]
          [:target {:optional true}
           [:enum "_self" "_blank" "_parent" "_top"]]])
        [:* [:schema [:ref ::phrasing-content]]])
       "del-phrasing"
       (->hiccup-schema
        :del
        (mu/merge
         global-attributes
         [:map [:cite {:optional true} :string]
          [:datetime {:optional true} :string]])
        [:* [:schema [:ref ::phrasing-content]]])
       "ins-phrasing"
       (->hiccup-schema
        :ins
        (mu/merge
         global-attributes
         [:map [:cite {:optional true} :string]
          [:datetime {:optional true} :string]])
        [:* [:schema [:ref ::phrasing-content]]])
       "link-phrasing"
       (->hiccup-schema
        :ins
        [:altn
         [:main
          (mu/merge
           global-attributes
           [:map
            [:itemprop :string]
            [:crossorigin {:optional true}
             [:enum "anonymous" "use-credentials"]]
            [:href {:optional true} url]
            [:media {:optional true} :string]
            [:rel {:optional true} :string]])]
         [:pre
          (mu/merge
           global-attributes
           [:map
            [:itemprop :string]
            [:crossorigin {:optional true}
             [:enum "anonymous" "use-credentials"]]
            [:href {:optional true} url]
            [:media {:optional true} :string]
            [:rel [:enum "preload" "prefetch"]]
            [:as [:enum "audio" "document" "embed"
                  "fetch" "font" "image" "object"
                  "script" "style" "track" "video" "worker"]]])]]
        nil)
       ::abbr (->hiccup-schema
               :abbr
               (mu/merge global-attributes
                         [:map [:title :string]])
               [:* [:schema [:ref ::phrasing-content]]])
       #_ ::area
       #_ ::audio
       ::b (->hiccup-schema
            :b
            global-attributes
            [:* [:schema [:ref ::phrasing-content]]])
       ::bdo (->hiccup-schema
              :bdo
              (mu/merge global-attributes
                        [:map [:dir [:enum "ltr" "rtl"]]])
              [:* [:schema [:ref ::phrasing-content]]])
       ::br (->hiccup-schema :br global-attributes nil)
       #_ ::button
       #_ ::canvas
       ::cite (->hiccup-schema
               :cite
               global-attributes
               [:* [:schema [:ref ::phrasing-content]]])
       ::code (->hiccup-schema
               :code
               global-attributes
               [:* [:schema [:ref ::phrasing-content]]])
       ::data (->hiccup-schema
               :data
               (mu/merge global-attributes
                         [:map [:value :string]])
               [:* [:schema [:ref ::phrasing-content]]])
       #_ ::datalist
       ::dfn (->hiccup-schema
              :dfn
              global-attributes
              [:* [:orn [:atomic-element atomic-element]
                   [:node
                    (apply
                     conj
                     [:orn
                      [:a [:schema [:ref "a-phrasing"]]]
                      [:del [:schema [:ref "del-phrasing"]]]
                      [:ins [:schema [:ref "ins-phrasing"]]]
                      [:link [:schema [:ref "link-phrasing"]]]]
                     (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                          (disj phrasing-tags :dfn)))]]])
       ::em (->hiccup-schema
             :em
             global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       #_ ::embed
       ::i (->hiccup-schema
            :i
            global-attributes
            [:* [:schema [:ref ::phrasing-content]]])
       #_ ::iframe
       ::img (->hiccup-schema
              :img
              (mu/merge
               global-attributes
               [:map
                [:src url]
                [:alt {:optional true} :string]
                [:sizes {:optional true} :string]
                [:width {:optional true} [:and [:> 0] [:<= 8192]]]
                [:height {:optional true} [:and [:> 0] [:<= 8192]]]
                [:loading {:optional true} [:enum "eager" "lazy"]]
                [:decoding {:optional true} [:enum "sync" "async" "auto"]]
                [:crossorigin {:optional true} [:enum "anonymous" "use-credentials"]]])
              nil)
       #_ ::input
       ::kbd (->hiccup-schema
              :kbd global-attributes
              [:* [:schema [:ref ::phrasing-content]]])
       #_ ::label
       #_ ::map
       ::mark (->hiccup-schema
               :mark global-attributes
               [:* [:schema [:ref ::phrasing-content]]])
       ::meta (->hiccup-schema
               :meta (mu/merge global-attributes
                               [:map [:itemprop :string]])
               nil)
       #_ ::meter
       #_ ::noscript
       #_ ::object
       #_ ::output
       #_ ::picture
       #_ ::progress
       ::q (->hiccup-schema
            :q (mu/merge global-attributes
                         [:map [:cite {:optional true} :string]])
            [:* [:schema [:ref ::phrasing-content]]])
       #_ ::ruby
       ::s (->hiccup-schema :s global-attributes
                            [:* [:schema [:ref ::phrasing-content]]])
       ::samp (->hiccup-schema
               :samp global-attributes
               [:* [:schema [:ref ::phrasing-content]]])
       ::script (->hiccup-schema
                 :script
                 (mu/merge
                  global-attributes
                  [:map
                   [:async {:optional true} [:enum true "async"]]
                   [:crossorigin {:optional true} :string]
                   [:defer {:optional true} [:= true]]
                   [:integrity {:optional true} :string]
                   [:nomodule {:optional true} :string]
                   [:referrerpolicy {:optional true}
                    [:enum "no-referrer" "no-referrer-when-downgrade"
                     "origin" "origin-when-cross-origin" "same-origin"
                     "strict-origin" "strict-origin-when-cross-origin" ""]]
                   [:src url]
                   [:type :string]])
                 [:? :string])
       ::small (->hiccup-schema
                :small global-attributes
                [:* [:schema [:ref ::phrasing-content]]])
       ::span (->hiccup-schema
               :span global-attributes
               [:* [:schema [:ref ::phrasing-content]]])
       ::strong (->hiccup-schema
                 :strong global-attributes
                 [:* [:schema [:ref ::phrasing-content]]])
       ::sub (->hiccup-schema
              :sub global-attributes
              [:* [:schema [:ref ::phrasing-content]]])
       ::sup (->hiccup-schema
              :sup global-attributes
              [:* [:schema [:ref ::phrasing-content]]])
       #_ ::svg
       #_ ::textarea
       ::time (->hiccup-schema
               :time (mu/merge global-attributes
                               [:map [:datetime :string]])
               [:* [:schema [:ref ::phrasing-content]]])
       ::var (->hiccup-schema
              :var global-attributes
              [:* [:schema [:ref ::phrasing-content]]])
       #_ ::video
       ::phrasing-content
       [:orn [:atomic-element atomic-element]
        [:node
         (apply
          conj
          [:orn
           [:a  [:schema [:ref "a-phrasing"]]]
           [:del [:schema [:ref "del-phrasing"]]]
           [:ins [:schema [:ref "ins-phrasing"]]]
           [:link [:schema [:ref "link-phrasing"]]]]
          (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
               phrasing-tags))]]
       ::hgroup
       (->hiccup-schema
        :hgroup
        global-attributes
        [:+
         [:orn
          [:h1 [:schema [:ref ::h1]]]
          [:h2 [:schema [:ref ::h2]]]
          [:h3 [:schema [:ref ::h3]]]
          [:h4 [:schema [:ref ::h4]]]
          [:h5 [:schema [:ref ::h5]]]
          [:h6 [:schema [:ref ::h6]]]]])
       ::h1 (->hiccup-schema
             :h1 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::h2 (->hiccup-schema
             :h2 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::h3 (->hiccup-schema
             :h3 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::h4 (->hiccup-schema
             :h4 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::h5 (->hiccup-schema
             :h5 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::h6 (->hiccup-schema
             :h6 global-attributes
             [:* [:schema [:ref ::phrasing-content]]])
       ::heading-content
       [:orn
        [:hgroup [:schema [:ref ::hgroup]]]
        [:h1 [:schema [:ref ::h1]]]
        [:h2 [:schema [:ref ::h2]]]
        [:h3 [:schema [:ref ::h3]]]
        [:h4 [:schema [:ref ::h4]]]
        [:h5 [:schema [:ref ::h5]]]
        [:h6 [:schema [:ref ::h6]]]]
       ::a
       [:orn
        [:phrasing [:schema [:ref "a-phrasing"]]]
        [:flow
         (->hiccup-schema
          :a
          (mu/merge
           global-attributes
           [:map
            [:href [:orn [:link url]
                    [:fragment :string]]]
            [:download {:optional true} :string]
            [:rel {:optional true} :string]
            [:target {:optional true} [:enum "_self" "_blank" "_parent" "_top"]]])
          [:* [:schema [:ref ::flow-content]]])]]
       ::address (->hiccup-schema
                  :address global-attributes
                  [:* (apply
                       conj [:orn
                             [:atomic-element atomic-element]
                             [:phrasing-content [:schema [:ref ::phrasing-content]]]]
                       (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                            (set/difference
                             flow-tags
                             heading-tags
                             phrasing-tags
                             sectioning-tags
                             #{:header :footer :address})))])
       ::article (->hiccup-schema
                  :article global-attributes
                  [:* [:schema [:ref ::flow-content]]])
       ::aside (->hiccup-schema
                :aside global-attributes
                [:* [:schema [:ref ::flow-content]]])
       ::bdi (->hiccup-schema
              :bdi global-attributes [:* [:schema [:ref ::phrasing-content]]])
       ::blockquote (->hiccup-schema
                     :blockquote
                     (mu/merge global-attributes
                               [:map [:cite {:optional true} :string]])
                     [:* [:schema [:ref ::flow-content]]])
       ::del
       [:orn
        [:phrasing [:schema [:ref "del-phrasing"]]]
        [:flow (->hiccup-schema
                :del global-attributes
                [:* [:schema [:ref ::flow-content]]])]]
       ::details (->hiccup-schema
                  :details
                  global-attributes
                  [:catn
                   [:sum
                    (->hiccup-schema
                     :summary
                     global-attributes
                     [:orn
                      [:flow [:* [:schema [:ref ::flow-content]]]]
                      [:heading [:schema [:ref ::heading-content]]]])]
                   [:contents [:* [:schema [:ref ::flow-content]]]]])
       ::div (->hiccup-schema
              :div global-attributes
              [:* [:schema [:ref ::flow-content]]])
       ::dl
       (->hiccup-schema
        :dl
        global-attributes
        [:*
         [:catn
          [:term
           [:+
            (->hiccup-schema
             :dt
             global-attributes
             (apply conj
                    [:orn
                     [:atomic-element atomic-element]
                     [:phrasing-content [:schema [:ref ::phrasing-content]]]
                     [:heading-content [:schema [:ref ::heading-content]]]]
                    (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                         (set/difference flow-tags phrasing-tags heading-tags sectioning-tags))))]]
          [:details
           [:+ (->hiccup-schema
                :dd global-attributes
                [:* [:schema [:ref ::flow-content]]])]]]])
       ::figure (->hiccup-schema
                 :figure global-attributes
                 [:altn
                  [:caption-first
                   [:catn
                    [:figcaption (->hiccup-schema
                                  :figcaption
                                  global-attributes
                                  [:* [:schema [:ref ::flow-content]]])]
                    [:rest [:* [:schema [:ref ::flow-content]]]]]]
                  [:caption-last
                   [:catn
                    [:rest [:* [:schema [:ref ::flow-content]]]]
                    [:figcaption (->hiccup-schema
                                  :figcaption
                                  global-attributes
                                  [:* [:schema [:ref ::flow-content]]])]]]
                  [:no-caption
                   [:* [:schema [:ref ::flow-content]]]]])
       ::footer
       (->hiccup-schema
        :footer global-attributes
        [:* (apply
             conj
             [:orn [:atomic-element atomic-element]
              [:phrasing-content [:schema [:ref ::phrasing-content]]]
              [:heading-content [:schema [:ref ::heading-content]]]]
             (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                  (set/difference flow-tags phrasing-tags heading-tags #{:header :footer})))])
       ::header
       (->hiccup-schema
        :header global-attributes
        [:* (apply
             conj
             [:orn [:atomic-element atomic-element]
              [:phrasing-content [:schema [:ref ::phrasing-content]]]
              [:heading-content [:schema [:ref ::heading-content]]]]
             (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                  (set/difference flow-tags phrasing-tags heading-tags #{:header :footer})))])
       ::hr (->hiccup-schema :hr global-attributes nil)
       ::ins
       [:orn
        [:phrasing [:schema [:ref "ins-phrasing"]]]
        [:flow (->hiccup-schema
                :ins global-attributes
                [:* [:schema [:ref ::flow-content]]])]]
       ::main (->hiccup-schema
               :main global-attributes
               [:* [:schema [:ref ::flow-content]]])
       ::nav (->hiccup-schema :nav global-attributes
                              [:* [:schema [:ref ::flow-content]]])
       ::ol (->hiccup-schema
             :ol
             (mu/merge
              global-attributes
              [:map
               [:reversed {:optional true} :boolean]
               [:start {:optional true} [:and [:> 0] [:< 65536]]]
               [:type {:optional true} [:enum "a" "A" "i" "I" "1"]]])
             [:*
              [:orn
               [:li (->hiccup-schema
                     :li
                     (mu/merge
                      global-attributes
                      [:map [:value {:optional true} :int]])
                     [:* [:schema [:ref ::flow-content]]])]
               [:script [:schema [:ref ::script]]]]])
       ::p (->hiccup-schema
            :p global-attributes
            [:* [:schema [:ref ::phrasing-content]]])
       ::pre (->hiccup-schema :pre global-attributes
                              [:* [:schema [:ref ::phrasing-content]]])
       ::section (->hiccup-schema :section global-attributes
                                  [:* [:schema [:ref ::flow-content]]])
       ::table [:schema
                {:registry
                 {::th (->hiccup-schema
                        :th
                        (mu/merge global-attributes
                                  [:map
                                   [:abbr {:optional true} :string]
                                   [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
                                   [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
                                   [:headers {:optional true} :string]
                                   [:scope {:optional true} [:enum "row" "col" "rowgroup"
                                                             "colgroup" "auto"]]])
                        [:*
                         (apply conj
                                [:orn [:atomic-element atomic-element]
                                 [:phrasing-content [:schema [:ref ::phrasing-content]]]
                                 [:heading-content [:schema [:ref ::heading-content]]]]
                                (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                                     (set/difference flow-tags
                                                     phrasing-tags
                                                     sectioning-tags
                                                     heading-tags #{:table :footer :header})))])
                  ::td (->hiccup-schema
                        :td
                        (mu/merge
                         global-attributes
                         [:map
                          [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
                          [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
                          [:headers {:optional true} :string]])
                        [:* [:schema [:ref ::flow-content]]])
                  ::tr (->hiccup-schema
                        :tr global-attributes
                        [:orn
                         [:all-header [:* [:schema [:ref ::th]]]]
                         [:rowdata [:catn [:header [:? [:schema [:ref ::th]]]]
                                    [:data [:* [:schema [:ref ::td]]]]]]])
                  ::table
                  (->hiccup-schema
                   :table
                   global-attributes
                   [:catn
                    [:caption [:? (->hiccup-schema
                                   :caption
                                   global-attributes
                                   [:* [:schema [:ref ::flow-content]]])]]
                    [:colgroups
                     [:*
                      [:orn
                       [:empty-span
                        (->hiccup-schema
                         :colgroup
                         (mu/merge global-attributes
                                   [:map [:span [:>= 1]]])
                         nil)]
                       [:cols
                        (->hiccup-schema
                         :colgroup
                         global-attributes
                         [:* (->hiccup-schema
                              :col
                              (mu/merge global-attributes
                                        [:map [:span [:>= 1]]])
                              nil)])]]]]
                    [:header [:? (->hiccup-schema
                                  :thead
                                  global-attributes
                                  [:* [:schema [:ref ::tr]]])]]
                    [:contents
                     [:altn
                      [:body (->hiccup-schema :tbody
                                              global-attributes
                                              [:* [:schema [:ref ::tr]]])]
                      [:rows [:+ [:schema [:ref ::tr]]]]]]
                    [:footer [:? (->hiccup-schema :tfoot
                                                  global-attributes
                                                  [:* [:schema [:ref ::tr]]])]]])}}
                ::table]
       ::ul (->hiccup-schema
             :ul global-attributes
             [:* [:orn
                  [:li (->hiccup-schema
                        :li global-attributes
                        [:* [:schema [:ref ::flow-content]]])]
                  [:script [:schema [:ref ::script]]]]])
       ::flow-content
       (apply conj
              [:orn
               [:atomic-element atomic-element]
               [:phrasing [:schema [:ref ::phrasing-content]]]
               [:heading  [:schema [:ref ::heading-content]]]]
              (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                   (set/difference
                    flow-tags phrasing-tags heading-tags)))
       ::element
       [:orn
        [:flow [:schema [:ref ::flow-content]]]
        [:heading [:schema [:ref ::heading-content]]]
        [:phrasing [:schema [:ref ::phrasing-content]]]]
       }}
     ::element])

(defn subschema [[_ meta-map orig-ref] new-ref]
  [:schema meta-map new-ref])

(comment
  (m/validate element [:em [:a {:href "http://google.com"} "link"] "more text"])

  (m/validate (subschema element ::em)
              [:em [:a {:href "http://google.com"} "link"] "more text"])

  (def em-gen
    (mg/generator
     (subschema element ::em)
     {:size 5}))

  (m/validate (subschema phrasing-content "a-phrasing")
              [:a {:href "http://google.com"} "link"])

  (m/validate element [:h1 [:b "emphasized header"]])

  (m/parse element [:h1 [:em "emphasized header"]])

  ((m/validator element) [:h1 [:em "emphasized header"]])

  (m/validate (subschema element ::p) [:p "text"])

  (m/validate (subschema element ::section)
              [:section [:h1 "header text"] "section text"]))


(def element? (m/validator element))

(def element-validators
  (let [kws (filter keyword? (keys (get (second element) :registry)))]
    (into {:atomic-element (m/validator atomic-element)}
          (map (fn [t] [t (m/validator (subschema element (ns-kw t)))])
               kws))))

(def phrasing? (m/validator (subschema element ::phrasing-content)))

;; "content is palpable when it's neither empty or hidden;
;; it is content that is rendered and is substantive.
;; Elements whose model is flow content or phrasing content
;; should have at least one node which is palpable."
(defn palpable? [c]
    (some? (some
            #(m/validate atomic-element %)
            (tree-seq #(and (vector? %) (keyword? (first %))) rest c))))




(comment
                                        ; example before

  (def sample-form '("first paragraph\n\nsecond paragraph"
                     [:section
                      {:class "grid"}
                      [:div {:class "1col"} "first coll line\n\nsecond col line"]
                      [:div {:class "1col"} "another cell"]]
                     "third paragraph"))

                                        ; example after
  (def sample-parsed-form
    '([:section
       [:p "first paragraph"]
       [:p "second paragraph"]]
      [:section {:class "grid"}
       [:div {:class "1col"} [:p "first cell line"] [:p "second cell line"]]
       [:div {:class "1col"} [:p "another cell"]]
       "third paragraph"])))

(defn not-in-form? [e] (and (vector? e)
                            (not (contains? phrasing-tags (first e)))))

;; already-tokenized is just the paragraph model
;; but it may be difficult to use it here
(defn already-tokenized? [e]
  (or
   (map? e)
   (and (vector? e) (= (first e) :p))))


;; group the orphans, then split the string


(defn para? [i] (and (vector? i) (= :p (first i))))
(defn in-para? [i] (or (m/validate atomic-element i)
                       (and (vector? i) (phrasing-tags (first i)))))

(defn detect-paragraphs
  "For each string in the element split it by the given regex, and insert the result into the original element. Leaves sub-elements as is and inserts them into the preceding paragraph."
  ([seq re]
   (let [v? (vector? seq)
         r (loop [s (apply ftree/counted-double-list seq)
                  final (ftree/counted-double-list)]
             (if (empty? s) final       ; base case
                 (let [h (first s) t (rest s)
                       current-elem (last final)]
                   (cond
                     (and (string? h)
                          (some? (re-find re h)))
                     (let [[hh & tt] (str/split h re)
                           rest (map (fn [i] [:p i]) tt)]
                       (cond
                         (or (empty? hh) (re-matches (re-pattern "\\s+") hh)) (recur (concat rest t) final)
                         ((get element-validators ::p) current-elem)
                         (recur (concat rest t)
                                (conj (first (ft-split-at final (- (count final) 1)))
                                      (conj current-elem hh)))
                         :else (recur (concat rest t) (conj final [:p hh]))))
                     (phrasing? h)
                     (if ((get element-validators ::p) current-elem)
                       (recur t
                              (conj (first (ft-split-at final (- (count final) 1)))
                                    (conj current-elem h)))
                       (recur t (conj final [:p h])))
                     :else
                     (recur t (conj final h))))))]
     (if v? (apply vector r)  r)))
  ([re] (fn [seq] (detect-paragraphs seq re)))
  ([] (detect-paragraphs (re-pattern "\n\n"))))

(comment
  (detect-paragraphs [:section "some\n\ntext" [:em "with emphasis"]]
                     #"\n\n"))

(defn split-strings
  "Split any strings in the sequence by the regex and insert them into the sequence. Ignore any non-strings."
  ([vec re]
   (apply
    vector
    (reduce
     (fn [acc next]
       (if (string? next)
         (concat acc (clojure.string/split next re))
         (concat acc (list next))))
     []
     (apply list vec))))
  ([re] (fn [v] (split-strings v re))))

(defn next? [elem]
  (= :next (first elem)))

;; seems like this may be a problem if I want to nest further
;; but the actual structure implied by the format I've chosen
;; is pretty flat - at most three levels deep:
;; [:article [:section #_ :next [:div "text"]]]

(defn process-nexts [nexts]
  (loop [[h n & rest] nexts
         res []]
    (if (empty? rest) ; base case
      (condp = [(nil? h) (nil? n)]
        [true true] res
        [false true] (conj res h)
        [false false] (conj res h n))
      (cond
        (= :next n)
        (recur (apply conj [:next] rest)
               (conj res h))
        (= :next h)
        (recur
         (drop-while #(not= % :next) rest)
         (conj res (apply conj n (take-while #(not= % :next) rest))))
        :else
        (recur
         (drop-while #(not= % :next) rest)
         (apply conj res h n (take-while #(not= % :next) rest)))))))

;; (def process-contents
;;   (comp
;;    (partition-by next?))
;;   (cond
;;     (next? (first contents))
;;     (let [[_ into-elem & rest] contents]
;;       (apply conj into-elem rest))
;;     :else contents))

(defn front-matter? [_] false)

(defn section? [i]
  (and (vector? i) (= :section (first i))))

(defn process-chunk [chunk]
  (cond
    (front-matter? chunk) chunk         ; front matter gets left as-is
    (section? (first (first chunk)))
    (let [[[s] content] chunk]
      (apply conj s
             (detect-paragraphs (process-nexts content) #"\n\n")))
    :else
    (let [[content] chunk]
      (apply conj
             [:section]
             (detect-paragraphs (process-nexts content) #"\n\n")))))

(def sectionize-contents
  (comp
   (partition-by section?)
   (partition-all 2)
   (map process-chunk)))


 (defn- make
    "Return the node, skipping attribute maps if present."
    {:license {:source "https://github.com/davidsantiago/hickory"
               :type "Eclipse Public License, v1.0"}}
    [node children]
    (if (vector? node)
      (if (map? (second node))
        (into (subvec node 0 2) children)
        (apply vector (first node) children))
      children))

  (defn- children
    "Return the node's children, skipping attribute maps if present."
    {:license {:source "https://github.com/davidsantiago/hickory"
               :type "Eclipse Public License, v1.0"}}
    [node]
    (if (vector? node)
      (if (map? (second node))
        (subvec node 2)
        (subvec node 1))
      node))

  (defn form-zipper [f]
    (zip/zipper not-in-form? children make f))

(defn hiccup-form? [f] (and (vector? f) (keyword? (first f))))

(defn valid-form?
  "Checks whether the given data conforms to its element model based on its keyword"
  [e]
  (let [tag (first e)
        valid? (get element-validators tag)]
    (valid? e)))

(defn invalid-form? [e]
  (and (hiccup-form? e) (not (valid-form? e))))

(defn find-invalid-forms [elem]
  (loop [loc (form-zipper elem)
         res {}]
    (let [e (zip/node loc)]
      (if (zip/end? loc) res
          (recur (zip/next loc)
                 (if (hiccup-form? e)
                   (if (not (valid-form? e))
                     (assoc res e (valid-form? e))
                     res)
                   res))))))

(comment
  (find-invalid-forms [:div [:p "text"] [:p [:p "text"]]])

  )

(comment
  (def unsectioned
    [:article
     [:p "Some text"]
     [:p "Some more text"]])

  (into [:article] sectionize (rest unsectioned))

  (def sectioned
    [:article
     [:section]
     [:p "text"]
     [:q "a quote"]
     [:section]
     [:p "more text"]])
  (into [:article] sectionize  (rest sectioned))

  (def partially-sectioned
    [:article
     [:p "text"]
     [:q "a quote"]
     [:section]
     [:p "more text"]])

  (into [:article] sectionize (rest partially-sectioned))

  (def actual-doc (-> "./content/information-cocoon.html.ct" slurp (respatialized.parse/parse-eval [:article])))

  (respatialized.build/load-deps)

  (def actual-doc-2 (-> "./content/reifying-filter-bubble-2.html.ct"
                        slurp
                        (respatialized.parse/parse-eval [:article])))

  (into [:article] sectionize-contents (rest actual-doc))

  (into [] sectionize-contents actual-doc-2))
