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
   [malli.util :as mu]
   #_[minimallist.minimap :refer [minimap-model]]))

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
    :wbr})

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
    #_:textarea :time :var #_:video :wbr})

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
    [:title :string]]))

(def atomic-element
 [:orn
  [:boolean :boolean]
  [:decimal :double]
  [:int :int]
  [:text :string]])


(comment

  #_(defn ->constrained-model
      ([pred generator max-tries-or-opts]
       (-> (h/fn pred)
           (h/with-test-check-gen
             (gen/such-that pred generator max-tries-or-opts))))
      ([pred generator]
       (-> (h/fn pred)
           (h/with-test-check-gen
             (gen/such-that pred generator)))))


  ;; example code from minimallist
  (def hiccup-model
    (h/let ['hiccup (h/alt [:node (h/in-vector (h/cat [:name (h/fn keyword?)]
                                                      [:props (h/? (h/map-of [(h/fn keyword?) (h/fn any?)]))]
                                                      [:children (h/* (h/not-inlined (h/ref 'hiccup)))]))]
                           [:element (h/alt [:nil (h/fn nil?)]
                                            [:boolean (h/fn boolean?)]
                                            [:number (h/fn number?)]
                                            [:text string-gen])])]
      (h/ref 'hiccup)))



  (defn quote-kw [kw] `~(symbol kw))
  (defn elem-ref [e] [e (h/ref (quote-kw e))])

  (defn has-required?
    "Checks to see if at least one entry in the given map model has required keys"
    [model]
    (and
     (= :map (:type model))
     (some #(nil? (get % :optional)) (:entries model))))

  (defn ->hiccup-model
    ([elem-tag attrs contents]
     (let [ms [[:tag (h/val elem-tag)]
               [:attributes (if (has-required? attrs) attrs (h/? attrs))]
               (cond (= :empty contents) nil
                     (valid? minimap-model contents)
                     [:contents contents]
                     (or (set? contents) (sequential? contents))
                     [:contents
                      (h/*
                       (h/not-inlined
                        (apply h/alt
                               [:atomic-element atomic-element]
                               contents)))])]]
       (h/not-inlined
        (h/in-vector
         (apply h/cat (filter some? ms))))))
    ([elem-tag contents] (->hiccup-model elem-tag global-attributes contents)))

                                        ; ruby isn't supported yet
  (let
      [ruby (h/in-vector
             (h/cat
              [:tag (h/val :ruby)]
              [:attributes (h/? global-attributes)]
              [:contents
               (h/+ (h/not-inlined
                     (h/cat
                      [:character string-gen]
                      [:pronunciation (h/in-vector
                                       (h/cat [:tag (h/val :rt)]
                                              [:text string-gen]))])))]))])

  (def ruby [:ruby "漢" [:rt "kan"] "字" [:rt "ji"]])

  (def datetime-str
    (h/alt
     [:year (->constrained-model #())])))

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
           head
           (conj head [:contents content-model]))))

(defn- dbl-colon [kw]
  (keyword (str kw)))

(defn ns-kw [kw] (keyword (str *ns*) (str (name kw))))

(defn ref-item [i] [:schema [:ref i]])

(comment

  (def sample-phrasing
    [:schema {:registry
              {"a-phrasing"
               [:catn
                [:tag [:= :a]]
                [:attributes [:? [:map-of keyword? any?]]]
                [:contents [:* [:schema [:ref "phrasing-content"]]]]]
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
             [:* [:schema [:ref "phrasing content"]]])
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

  )


(def phrasing-content
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
                      [:a (ref-item "a-phrasing")]
                      [:del (ref-item "del-phrasing")]
                      [:ins (ref-item "ins-phrasing")]
                      [:link (ref-item "link-phrasing")]]
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
               #{:em}))]]}}
     ::phrasing-content])

(defn subschema [[_ meta-map orig-ref] new-ref]
  [:schema meta-map new-ref])

(comment
  (m/validate phrasing-content [:em [:a {:href "http://google.com"} "link"] "more text"])

  (m/validate (subschema phrasing-content ::em)
              [:em [:a {:href "http://google.com"} "link"] "more text"])

  (m/validate (subschema phrasing-content "a-phrasing")
              [:a {:href "http://google.com"} "link"])

  )

(def heading-content
  [:schema
   {:registry
    {::hgroup
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
     ::h1 (->hiccup-schema :h1 global-attributes
                           [:* phrasing-content])
     ::h2 (->hiccup-schema :h2 global-attributes
                           [:* phrasing-content])
     ::h3 (->hiccup-schema :h3 global-attributes
                           [:* phrasing-content])
     ::h4 (->hiccup-schema :h4 global-attributes
                           [:* phrasing-content])
     ::h5 (->hiccup-schema :h5 global-attributes
                           [:* phrasing-content])
     ::h6 (->hiccup-schema :h6 global-attributes
                           [:* phrasing-content])
     ::heading-content
     [:orn
      [:hgroup [:schema [:ref ::hgroup]]]
      [:h1 [:schema [:ref ::h1]]]
      [:h2 [:schema [:ref ::h2]]]
      [:h3 [:schema [:ref ::h3]]]
      [:h4 [:schema [:ref ::h4]]]
      [:h5 [:schema [:ref ::h5]]]
      [:h6 [:schema [:ref ::h6]]]]}}
   ::heading-content])

(comment
  (m/validate heading-content [:h1 [:em "emphasized header"]])

  )

(def flow-content
  [:schema
   {:registry
    {::a
     [:orn
      [:phrasing (subschema phrasing-content "a-phrasing")]
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
        [:flow-content [:* [:schema [:ref ::flow-content]]]])]]
     ::address (->hiccup-schema
                :address global-attributes
                [:* (apply
                     conj [:orn
                           [:atomic-element atomic-element]]
                     (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                          (clojure.set/difference
                           flow-tags
                           heading-tags
                           sectioning-tags
                           #{:header :footer :address})))])
     ::article (->hiccup-schema
                :article global-attributes
                [:* [:schema [:ref ::flow-content]]])
     ::aside (->hiccup-schema
              :aside global-attributes
              [:* [:schema [:ref ::flow-content]]])
     ::bdi (->hiccup-schema
            :bdi global-attributes [:* phrasing-content])
     ::blockquote (->hiccup-schema
                   :blockquote
                   (mu/merge global-attributes
                             [:map [:cite {:optional true} :string]])
                   [:* [:schema [:ref ::flow-content]]])
     ::del
     [:orn
      [:phrasing (subschema phrasing-content "del-phrasing")]
      [:flow (->hiccup-schema
              :del global-attributes
              [:* [:schema [:ref ::flow-content]]])]]
     ::details (->hiccup-schema
                :details
                global-attributes
                [:catn
                 [:summary (->hiccup-schema
                            :summary
                            global-attributes
                            [:orn
                             [:flow [:* [:schema [:ref ::flow-content]]]]
                             [:heading heading-content]])]
                 [:contents [:schema [:ref ::flow-content]]]])
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
         (->hiccup-schema
          :dt
          global-attributes
          (apply conj
                 [:orn
                  [:atomic-element atomic-element]]
                 (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                      (set/difference flow-tags sectioning-tags))))]
        [:details
         (->hiccup-schema
          :dd global-attributes
          [:* [:schema [:ref ::flow-content]]])]]])
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
           [:orn [:atomic-element atomic-element]]
           (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                (set/difference flow-tags #{:header :footer})))])
     ::header
     (->hiccup-schema
      :header global-attributes
      [:* (apply
           conj
           [:orn [:atomic-element atomic-element]]
           (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                (set/difference flow-tags #{:header :footer})))])
     ::hr (->hiccup-schema :hr global-attributes nil)
     ::ins
     [:orn
      [:phrasing (subschema phrasing-content "ins-phrasing")]
      [:flow (->hiccup-schema
              :ins global-attributes
              [:* [:schema [:ref ::flow-content]]])]]
     ::main (->hiccup-schema
             :main global-attributes
             [:* [:schema [:ref ::flow-content]]])
     ::nav (->hiccup-schema :nav global-attributes
                            [:* [:schema [:ref ::flow-content]]])
     ::ol (->hiccup-schema
           :ol (mu/merge
                global-attributes
                [:map
                 [:reversed {:optional true} :boolean]
                 [:start {:optional true} [:and [:> 0] [:< 65536]]]
                 [:type {:optional true} [:enum "a" "A" "i" "I" "1"]]])
           [:*
            [:altn
             [:li (->hiccup-schema
                   :li
                   (mu/merge
                    global-attributes
                    [:map [:value {:optional true} :int]])
                   [:* [:schema [:ref ::flow-content]]])]
             [:script [:schema [:ref ::script]]]]])
     ::p (->hiccup-schema :p global-attributes
                          [:* phrasing-content])
     ::pre (->hiccup-schema :pre global-attributes
                            [:* phrasing-content])
     ::section (->hiccup-schema :section global-attributes
                                [:* [:schema [:ref ::flow-content]]])

     ;; ::table [:schema
     ;;          {:registry
     ;;           {::th (->hiccup-schema
     ;;                  :th
     ;;                  (mu/merge global-attributes
     ;;                            [:map
     ;;                             [:abbr {:optional true} :string]
     ;;                             [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
     ;;                             [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
     ;;                             [:headers {:optional true} :string]
     ;;                             [:scope {:optional true} [:enum "row" "col" "rowgroup"
     ;;                                                       "colgroup" "auto"]]])
     ;;                  [:*
     ;;                   (apply conj
     ;;                          [:altn [:atomic-element atomic-element]]
     ;;                          (map (fn [t] [t (ref-item t)])
     ;;                               (set/difference flow-tags sectioning-tags
     ;;                                               heading-tags #{:table :footer :header})))])
     ;;            ::td (->hiccup-schema
     ;;                  :td
     ;;                  (mu/merge
     ;;                   global-attributes
     ;;                   [:map
     ;;                    [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
     ;;                    [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
     ;;                    [:headers {:optional true} :string]])
     ;;                  [:schema [:ref ::flow-contents]])
     ;;            ::tr (->hiccup-schema
     ;;                  :tr global-attributes
     ;;                  [:altn
     ;;                   [:all-header [:* [:schema [:ref ::th]]]]
     ;;                   [:rowdata [:catn [:header [:? [:schema [:ref ::th]]]]
     ;;                              [:data [:* [:schema [:ref ::td]]]]]]])}}
     ;;          (->hiccup-schema
     ;;           :table
     ;;           global-attributes
     ;;           [:catn
     ;;            [:caption [:? (->hiccup-schema :caption
     ;;                                           global-attributes
     ;;                                           [:schema [:ref ::flow-contents]])]]
     ;;            [:colgroups
     ;;             [:*
     ;;              [:altn
     ;;               [:empty-span
     ;;                (->hiccup-schema
     ;;                 :colgroup
     ;;                 (mu/merge global-attributes
     ;;                           [:map [:span [:>= 1]]])
     ;;                 nil)]
     ;;               [:cols
     ;;                (->hiccup-schema
     ;;                 :colgroup
     ;;                 global-attributes
     ;;                 [:* (->hiccup-schema
     ;;                      :col
     ;;                      (mu/merge global-attributes
     ;;                                [:map [:span [:>= 1]]])
     ;;                      nil)])]]]]
     ;;            [:header [:? (->hiccup-schema :thead
     ;;                                          global-attributes
     ;;                                          [:* [:schema [:ref ::tr]]])]]
     ;;            [:contents
     ;;             [:altn
     ;;              [:body (->hiccup-schema :tbody
     ;;                                      global-attributes
     ;;                                      [:* [:schema [:ref ::tr]]])]
     ;;              [:rows [:+ [:schema [:ref ::tr]]]]]]
     ;;            [:footer [:? (->hiccup-schema :tfoot
     ;;                                          global-attributes
     ;;                                          [:* [:schema [:ref ::tr]]])]]])]

     ::ul (->hiccup-schema
           :ul global-attributes
           [:* [:altn
                [:li (->hiccup-schema
                      :li global-attributes
                      [:* [:schema [:ref ::flow-content]]])]
                [:script [:schema [:ref ::script]]]]])

     ::flow-content
     (apply conj
            [:orn
             [:atomic-element atomic-element]
             [:phrasing phrasing-content]
             [:heading heading-content]]
            (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                 (set/difference
                  flow-tags phrasing-tags heading-tags)))}}
   ::flow-content])



(comment
  (m/validate flow-content [:p "text"])

  )

(defn anti-subschema [[_ meta-map ref] excluded-item]
  [:schema
   (update meta-map :registry (fn [r] (dissoc )))])

(comment
  (def elements
    (h/let
        ;; inline text elements first
        ['phrasing-content
         (h/let ['a-phrasing
                 (->hiccup-model
                  :a
                  (-> global-attributes
                      (h/with-entries
                        [:href
                         (h/alt
                          [:link url]
                          [:fragment string-gen])])
                      (h/with-optional-entries
                        [:download string-gen]
                        [:rel string-gen]
                        [:target (h/enum #{"_self" "_blank" "_parent" "_top"})]))
                  (h/*
                   (apply h/alt
                          [:a (h/ref 'a-phrasing)]
                          [:ins (h/ref 'ins-phrasing)]
                          [:del (h/ref 'del-phrasing)]
                          [:atomic-element atomic-element]
                          (map elem-ref phrasing-tags))))
                 'del-phrasing
                 (->hiccup-model
                  :del
                  (h/with-optional-entries global-attributes
                    [:cite string-gen]
                    [:datetime string-gen])
                  (h/*
                   (apply h/alt
                          [:a (h/ref 'a-phrasing)]
                          [:ins (h/ref 'ins-phrasing)]
                          [:del (h/ref 'del-phrasing)]
                          [:atomic-element atomic-element]
                          (map elem-ref phrasing-tags))))
                 'ins-phrasing
                 (->hiccup-model
                  :ins
                  (h/with-optional-entries global-attributes
                    [:cite string-gen]
                    [:datetime string-gen])
                  (h/*
                   (apply h/alt
                          [:a (h/ref 'a-phrasing)]
                          [:ins (h/ref 'ins-phrasing)]
                          [:del (h/ref 'del-phrasing)]
                          [:atomic-element atomic-element]
                          (map elem-ref phrasing-tags))))
                 'link-phrasing
                 (->hiccup-model
                  :link
                  (h/let
                      ['link-attrs (-> global-attributes
                                       (h/with-entries [:itemprop string-gen])
                                       (h/with-optional-entries
                                         [:crossorigin (h/enum #{"anonymous" "use-credentials"})]
                                         [:href url]
                                         [:media string-gen]
                                         [:rel string-gen]))]
                      (h/alt
                       [:pre (-> (h/ref 'link-attrs)
                                 (h/with-entries
                                   [:rel (h/enum #{"preload" "prefetch"})])
                                 (h/with-optional-entries
                                   [:as (h/enum #{"audio" "document" "embed"
                                                  "fetch" "font" "image" "object"
                                                  "script" "style" "track" "video"
                                                  "worker"})]))]
                       [:main (h/ref 'link-attrs)]))
                  :empty)]
           (apply h/alt
                  [:a (h/ref 'a-phrasing)]
                  [:ins (h/ref 'ins-phrasing)]
                  [:del (h/ref 'del-phrasing)]
                  [:link (h/ref 'link-phrasing)]
                  [:atomic-element atomic-element]
                  (map elem-ref phrasing-tags)))
         'phrasing-contents (h/* (h/ref 'phrasing-content))
         'a (->hiccup-model :a
                            (-> global-attributes
                                (h/with-entries
                                  [:href
                                   (h/alt
                                    [:link url]
                                    [:fragment string-gen])])
                                (h/with-optional-entries
                                  [:download string-gen]
                                  [:rel string-gen]
                                  [:target (h/enum #{"_self" "_blank" "_parent" "_top"})]))
                            (h/ref 'phrasing-contents))
         'abbr (->hiccup-model :abbr (h/ref 'phrasing-contents))
         'b (->hiccup-model :b (h/ref 'phrasing-contents))
         'bdi (->hiccup-model :bdi (h/ref 'phrasing-contents))
         'bdo (->hiccup-model :bdo
                              (h/with-entries global-attributes
                                [:dir (h/enum #{"ltr" "rtl"})])
                              (h/ref 'phrasing-contents))
         'br (->hiccup-model :br :empty)
         'cite (->hiccup-model :cite (h/ref 'phrasing-contents))
         'code (->hiccup-model :code (h/ref 'phrasing-contents))
         'data (->hiccup-model :data
                               (h/with-entries global-attributes
                                 [:value string-gen])
                               (h/ref 'phrasing-contents))
         'del (->hiccup-model :del (h/ref 'phrasing-contents))
         'ins (->hiccup-model :ins (h/ref 'phrasing-contents))
         'dfn (->hiccup-model :dfn
                              (map elem-ref (disj phrasing-tags :dfn)))
         'em (->hiccup-model :em (h/ref 'phrasing-contents))
         'i (->hiccup-model :i (h/ref 'phrasing-contents))
         'kbd (->hiccup-model :kbd (h/ref 'phrasing-contents))
         'mark (->hiccup-model :mark (h/ref 'phrasing-contents))
         'q (->hiccup-model :q
                            (h/with-optional-entries global-attributes
                              [:cite url])
                            (h/ref 'phrasing-contents))
         's (->hiccup-model :s (h/ref 'phrasing-contents))
         'samp (->hiccup-model :samp (h/ref 'phrasing-contents))
         'img (->hiccup-model :img
                              (-> global-attributes
                                  (h/with-entries [:src url])
                                  (h/with-optional-entries
                                    [:alt string-gen]
                                    [:sizes string-gen]
                                    [:width (->constrained-model #(< 0 % 8192) gen/small-integer)]
                                    [:height (->constrained-model #(< 0 % 8192) gen/small-integer)]
                                    [:loading (h/enum #{"eager" "lazy"})]
                                    [:decoding (h/enum #{"sync" "async" "auto"})]
                                    [:crossorigin (h/enum #{"anonymous" "use-credentials"})]))
                              :empty)
         'small (->hiccup-model :small (h/ref 'phrasing-contents))
         'span (->hiccup-model :span (h/ref 'phrasing-contents))
         'strong (->hiccup-model :strong (h/ref 'phrasing-contents))
         'sub (->hiccup-model :sub (h/ref 'phrasing-contents))
         'sup (->hiccup-model :sup (h/ref 'phrasing-contents))
         'time (->hiccup-model :time
                               (h/with-entries
                                 global-attributes
                                 [:datetime string-gen])
                               (h/ref 'phrasing-contents))
         'u (->hiccup-model :u (h/ref 'phrasing-contents))
         'var (->hiccup-model :var (h/ref 'phrasing-contents))
         'wbr (h/val [:wbr])
         ;; link
         'link (->hiccup-model
                :link
                (h/let
                    ['link-attrs (-> global-attributes
                                     (h/with-optional-entries
                                       [:crossorigin (h/enum #{"anonymous" "use-credentials"})]
                                       [:href url]
                                       [:type (h/enum #{"text/html" "text/css"})]
                                       [:media string-gen]
                                       [:rel string-gen]))]
                    (h/alt
                     [:pre (-> (h/ref 'link-attrs)
                               (h/with-entries
                                 [:rel (h/enum #{"preload" "prefetch"})])
                               (h/with-optional-entries
                                 [:as (h/enum #{"audio" "document" "embed"
                                                "fetch" "font" "image" "object"
                                                "script" "style" "track" "video"
                                                "worker"})]))]
                     [:main (h/ref 'link-attrs)]))
                :empty)
         ;; then other text
         'blockquote (->hiccup-model :blockquote
                                     (h/with-optional-entries
                                       global-attributes
                                       [:cite string-gen])
                                     (map elem-ref flow-tags))
         'dl (h/in-vector
              (h/cat
               [:tag (h/val :dl)]
               [:attributes (h/? global-attributes)]
               [:contents
                (h/*
                 (h/cat [:term
                         (h/not-inlined
                          (->hiccup-model
                           :dt
                           (map elem-ref (set/difference flow-tags sectioning-tags))))]
                        [:defn-details
                         (h/not-inlined (->hiccup-model
                                         :dd
                                         (map elem-ref flow-tags)))]))]))
         'div (->hiccup-model :div (map elem-ref flow-tags))
         'figure
         (h/in-vector
          (h/cat
           [:tag (h/val :figure)]
           [:attributes (h/? global-attributes)]
           [:contents
            (h/alt
             [:caption-first
              (h/cat
               [:figcaption
                (->hiccup-model :figcaption (map elem-ref flow-tags))]
               [:rest (h/*
                       (h/not-inlined
                        (apply h/alt
                               [:atomic-element atomic-element]
                               (map elem-ref flow-tags))))])]
             [:caption-last
              (h/cat
               [:rest (h/*
                       (h/not-inlined
                        (apply h/alt
                               [:atomic-element atomic-element]
                               (map elem-ref flow-tags))))]
               [:figcaption
                (-> :figcaption
                    (->hiccup-model (map elem-ref flow-tags))
                    h/not-inlined)])]
             [:no-caption
              (h/*
               (h/not-inlined
                (apply h/alt
                       [:atomic-element atomic-element]
                       (map elem-ref flow-tags))))])]))
         'hr (h/in-vector (h/cat [:tag (h/val :hr)] [:attributes (h/? global-attributes)]))
         'ol (->hiccup-model
              :ol (h/with-optional-entries global-attributes
                    [:reversed boolean-gen]
                    [:start (->constrained-model pos-int? gen/small-integer)]
                    [:type (h/enum #{"a" "A" "i" "I" "1"})])
              (conj (map elem-ref #{:script #_:template})
                    [:li
                     (->hiccup-model
                      :li
                      (h/with-optional-entries global-attributes
                        [:value (->constrained-model pos-int? gen/small-integer)])
                      (map elem-ref flow-tags))]))
         'ul (->hiccup-model :ul
                             (conj (map elem-ref #{:script #_:template})
                                   [:li (->hiccup-model :li (map elem-ref flow-tags))]))
         'p (->hiccup-model :p (h/ref 'phrasing-contents))
         'pre (->hiccup-model :pre global-attributes
                              (h/ref 'phrasing-contents))
         ;; sectioning
         'address (->hiccup-model
                   :address
                   (map elem-ref (set/difference flow-tags
                                                 sectioning-tags
                                                 #{:h1 :h2 :h3 :h4 :h5 :h6
                                                   :header :footer})))
         'article (->hiccup-model :article (map elem-ref flow-tags))
         'aside (->hiccup-model :aside (map elem-ref flow-tags))
         'footer (->hiccup-model :footer (map elem-ref (set/difference
                                                        flow-tags
                                                        #{:header :footer})))
         'header (->hiccup-model :header (map elem-ref (set/difference
                                                        flow-tags
                                                        #{:header :footer})))
         'h1 (->hiccup-model :h1 (h/ref 'phrasing-contents))
         'h2 (->hiccup-model :h2 (h/ref 'phrasing-contents))
         'h3 (->hiccup-model :h3 (h/ref 'phrasing-contents))
         'h4 (->hiccup-model :h4 (h/ref 'phrasing-contents))
         'h5 (->hiccup-model :h5 (h/ref 'phrasing-contents))
         'h6 (->hiccup-model :h6 (h/ref 'phrasing-contents))
         'main (->hiccup-model :main (map elem-ref flow-tags))
         'nav (->hiccup-model :nav (map elem-ref flow-tags))
         'section (->hiccup-model :section (map elem-ref flow-tags))
         ;; then other stuff
         'script
         (->hiccup-model
          :script
          (h/with-optional-entries
            global-attributes
            [:async (h/enum #{true "async"})]
            [:crossorigin string-gen]
            [:defer (h/val true)]
            [:integrity string-gen]
            [:nomodule (h/val true)]
            [:referrerpolicy (h/enum #{"no-referrer"
                                       "no-referrer-when-downgrade"
                                       "origin"
                                       "origin-when-cross-origin"
                                       "same-origin"
                                       "strict-origin"
                                       "strict-origin-when-cross-origin"
                                       ""})]
            [:src url]
            [:type string-gen])
          (h/? string-gen))
         'details (->hiccup-model
                   :details
                   (h/in-vector
                    (h/cat
                     [:summary
                      (->hiccup-model
                       :summary
                       (h/alt
                        [:flow-content
                         (h/* (apply
                               h/alt
                               [:atomic-element atomic-element]
                               (map elem-ref flow-tags)))]
                        [:heading-content (apply h/alt (map elem-ref heading-tags))]))]
                     [:flow-content (h/* (apply h/alt
                                                [:atomic-element atomic-element]
                                                (map elem-ref flow-tags)))])))
         'table (h/let ['th
                        (->hiccup-model
                         :th
                         (h/with-optional-entries
                           global-attributes
                           [:abbr string-gen]
                           [:colspan (->constrained-model pos-int? gen/small-integer)]
                           [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                           [:headers string-gen]
                           [:scope (h/enum #{"row" "col" "rowgroup" "colgroup" "auto"})])
                         (map elem-ref (set/difference
                                        flow-tags sectioning-tags heading-tags
                                        #{:table :footer :header})))
                        'td (->hiccup-model
                             :td
                             (h/with-optional-entries
                               global-attributes
                               [:colspan (->constrained-model pos-int? gen/small-integer)]
                               [:rowspan (->constrained-model #(<= 0 % 65534) gen/small-integer)]
                               [:headers string-gen])
                             (map elem-ref flow-tags))
                        'tr
                        (->hiccup-model
                         :tr
                         global-attributes
                         (h/alt
                          [:all-header (h/* (h/ref 'th))]
                          [:rowdata (h/cat (h/? (h/ref 'th)) (h/* (h/ref 'td)))]))]
                  (h/in-vector
                   (h/cat
                    [:tag (h/val :table)]
                    [:caption (h/? (->hiccup-model :caption (map elem-ref flow-tags)))]
                    [:colgroups (h/* (->hiccup-model
                                      :colgroup
                                      [[:col (->hiccup-model
                                              :col
                                              (h/with-optional-entries
                                                global-attributes
                                                [:span int-gen])
                                              :empty)]]))]
                    [:header (h/? (->hiccup-model :thead [[:tr (h/ref 'tr)]]))]
                    [:contents
                     (h/alt
                      [:body (->hiccup-model :tbody [[:tr (h/ref 'tr)]])]
                      [:rows (h/+ (h/ref 'tr))])]
                    [:footer (h/? (->hiccup-model :tfoot [[:tr (h/ref 'tr)]]))])))]
        (h/alt
         [:phrasing-content (h/ref 'phrasing-content)]
         [:phrasing-contents (h/ref 'phrasing-contents)]
         [:a (h/ref 'a)]
         [:abbr (h/ref 'abbr)]
         [:article (h/ref 'article)]
         [:aside (h/ref 'aside)]
         #_[:audio (h/ref 'audio)]
         [:b (h/ref 'b)]
         [:bdo (h/ref 'bdo)]
         [:bdi (h/ref 'bdi)]
         [:blockquote (h/ref 'blockquote)]
         [:br (h/ref 'br)]
         #_[:button (h/ref 'button)]
         #_[:canvas (h/ref 'canvas)]
         [:cite (h/ref 'cite)]
         [:code (h/ref 'code)]
         [:data (h/ref 'data)]
         #_[:datalist (h/ref 'datalist)]
         [:del (h/ref 'del)]
         [:details (h/ref 'details)]
         [:dfn (h/ref 'dfn)]
         [:div (h/ref 'div)]
         [:dl (h/ref 'dl)]
         [:em (h/ref 'em)]
         #_[:embed (h/ref 'embed)]
         #_[:fieldset (h/ref 'fieldset)]
         [:figure (h/ref 'figure)]
         [:footer (h/ref 'footer)]
         #_[:form (h/ref 'form)]
         [:h1 (h/ref 'h1)] [:h2 (h/ref 'h2)] [:h3 (h/ref 'h3)]
         [:h4 (h/ref 'h4)] [:h5 (h/ref 'h5)] [:h6 (h/ref 'h6)]
         [:header (h/ref 'header)]
         [:hr (h/ref 'hr)]
         [:i (h/ref 'i)]
         #_[:iframe (h/ref 'iframe)]
         [:img (h/ref 'img)]
         #_[:input (h/ref 'input)]
         [:ins (h/ref 'ins)]
         [:kbd (h/ref 'kbd)]
         #_[:label (h/ref 'label)]
         [:link (h/ref 'link)]
         [:main (h/ref 'main)]
         #_[:map (h/ref 'map)]
         #_[:mark (h/ref 'mark)]
         #_[:math (h/ref 'math)]
         #_[:menu (h/ref 'menu)]
         #_[:meter (h/ref 'meter)]
         [:nav (h/ref 'nav)]
         #_[:noscript (h/ref 'noscript)]
         #_[:object (h/ref 'object)]
         [:ol (h/ref 'ol)]
         #_[:output (h/ref 'output)]
         [:p (h/ref 'p)]
         #_[:picture (h/ref 'picture)]
         [:pre (h/ref 'pre)]
         #_[:progress (h/ref 'progress)]
         [:q (h/ref 'q)]
         [:s (h/ref 's)]
         [:samp (h/ref 'samp)]
         [:script (h/ref 'script)]
         [:section (h/ref 'section)]
         #_[:select (h/ref 'select)]
         [:span (h/ref 'span)]
         [:strong (h/ref 'strong)]
         [:sub (h/ref 'sub)]
         [:sup (h/ref 'sup)]
         #_[:svg (h/ref 'svg)]
         [:table (h/ref 'table)]
         #_[:template (h/ref 'template)]
         #_[:textarea (h/ref 'textarea)]
         [:time (h/ref 'time)]
         [:ul (h/ref 'ul)]
         [:var (h/ref 'var)]
         [:wbr (h/ref 'wbr)])))

  (defn ->element-model
    "Produce the model specific to the given element from the global elements model."
    [elem]
    (merge (select-keys elements [:type :bindings])
           {:body
            {:type :ref :key (quote-kw elem)}}))



  (defn update-child-elements-model [model children]
    (let [[t attr contents] (:entries model)]
      (assoc
       model
       :entries
       [t attr (update-in contents [:model :elements-model :entries]
                          (fn [e] (into [] (filter #(contains? children (:key %)) e))))])))

  (defn restrict-alt-model [{:keys [type bindings body]} children]
    {:type type
     :bindings
     (->> (select-keys bindings (map symbol children))
          (map (fn [[k bind]] [k (update-child-elements-model bind children)]))
          (into {}))
     :body (update
            body :entries
            (fn [e] (into [] (filter #(contains? children (:key %)) e))))}))


(comment
  ;; dynamic programming to find the offending element(s)
  (require '[clojure.math.combinatorics :as combo])

  (let [ks (map keyword (keys (:bindings elements)))
        bad-elements
        (filter (fn [e]
                  (let [submodel
                        (restrict-alt-model
                         elements
                         #{e :atomic-element})]
                    (not (valid? minimap-model submodel)))) ks)
        bad-subsets (filter (fn [sub-elements]
                              (let [submodel
                                    (restrict-alt-model
                                     elements
                                     (conj (set sub-elements) :atomic-element))]
                                (not (valid? minimap-model submodel))))
                            (combo/subsets ks))]

    (println bad-elements)
    ;; => (:img :figure :hr :table :br)
    ;; (apply set (take 100 bad-subsets))
    )

  )

;; "content is palpable when it's neither empty or hidden;
;; it is content that is rendered and is substantive.
;; Elements whose model is flow content or phrasing content
;; should have at least one node which is palpable."

(defn palpable? [c]
  (some? (some
          #(valid? atomic-element %)
          (tree-seq #(and (vector? %) (keyword? (first %))) rest c))))

(def flow-content
  (apply h/alt
         (map #(h/with-condition (->element-model %)
                 (h/fn palpable?)) flow-tags)))

(def phrasing-content-m
  (apply h/alt
         atomic-element
         (map
          #(h/with-condition (->element-model %)
             (h/fn palpable?)) phrasing-tags)))


;; (def palpable)

(def article-outline
  "A model for an article with a clearly delineated structure"
  (h/in-vector
   (h/cat
    [:tag (h/val :article)]
    [:article-header (h/? (->element-model :header))]
    [:sections (h/+ (->element-model :section))]
    [:article-footer (h/? (->element-model :footer))])))

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
(defn in-para? [i] (or (valid? atomic-element i)
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
                         (valid? (->element-model :p) current-elem)
                         (recur (concat rest t)
                                (conj (first (ft-split-at final (- (count final) 1)))
                                      (conj current-elem hh)))
                         :else (recur (concat rest t) (conj final [:p hh]))))
                     (valid? (->element-model :phrasing-content) h)
                     (if (valid? (->element-model :p) current-elem)
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
  (valid? (->element-model (keyword (first e)))
          e))

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
