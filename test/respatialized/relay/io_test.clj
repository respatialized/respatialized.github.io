(ns respatialized.relay.io-test
  (:require [clojure.test :as t]
            [respatialized.relay.io :refer :all]))


(def edn-text
  '({:text
  "In nearly all hot-arid and temperate climates, earth has always been the most prevalent building material. Even today, one third of the human population resides in earthen houses; in developing countries this figure is more than one half. It has proven impossible to fulfil the immense requirements for shelter in the developing countries with industrial building materials, i.e. brick, concrete and steel, nor with industrialised construction techniques. Worldwide, no region is endowed with the productive capacity or financial resources needed to satisfy this demand. In the developing countries, requirements for shelter can be met only by using local building materials and relying on do-it-yourself construction techniques. Earth is the most important natural building material, and it is available in most regions of the world. It is frequently obtained directly from the building site when excavating foundations or basements. In the industrialised countries, careless exploitation of resources and centralised capital combined with energy-intensive production is not only wasteful; it also pollutes the environment and increases unemployment. In these countries, earth is being revived as a building material."}
 {:text
  "Increasingly, people when building homes demand energy- and cost-effective buildings that emphasise a healthy, balanced indoor climate. They are coming to realise that mud, as a natural building material, is superior to industrial building materials such as concrete, brick and lime-sandstone. Newly developed, advanced earth building techniques demonstrate the value of earth not only in do-it-yourself construction, but also for industrialised construction involving contractors."}
 {:text
  "This handbook presents the basic theoretical data concerning this material, and it provides the necessary guidelines, based on scientific research and practical experience, for applying it in a variety of contexts."}))

(def test-plaintext (slurp "resources/building-with-earth-excerpt.txt"))

(def test-edn-file "resources/building-with-earth-excerpt.edn")

(t/deftest text-parsers
  (t/testing "basic parsing"
    (t/is (= 3 (count (txt->edn test-plaintext))))
    (t/is (= edn-text (file->edn test-edn-file)))
    (t/is (= edn-text (txt->edn test-plaintext) (file->edn test-edn-file)))
    )

  (t/testing "identical output for regular and malformed inputs")


  (t/testing "identical output for plaintext and hiccup-style inputs")
  )

(t/deftest textual-input
  (def sample-markdown-txt (slurp "resources/sample-markdown.md"))

  (t/testing "plaintext parsers"
    (t/is false "tabular input should be captured by the markdown parser")
    (t/is false "tabular input should be captured by the ETN parser"))
  )
