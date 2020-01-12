(ns respatialized.relay.io-test
  (:require [clojure.test :as t]
            [clojure.spec.alpha :as spec]
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
  (def sample-markdown (slurp "resources/sample-markdown.md"))
  (def sample-md-hiccup (md->hiccup sample-markdown))

  (t/testing "plaintext parsers"
    (t/is
     (every? #(spec/valid? :respatialized.archive/tidy-table %) (pull-tables sample-md-hiccup))
     "tabular input should be captured by the markdown parser")

    (t/is
     (=
       ["A good class project for undergraduates who have not become too tainted with either the commercial or research computing milieu, is to have them design a computer system for a think tank such as RAND or the Institute for Advanced Study at Princeton. It is a delightfully nebulous question, since they quickly realize it will be impossible for them to even discover what the majority of the thinkers are doing. Indeed, many of the researchers will not know themselves or be able to articulate that state of mind of just feeling around. It is at this point that a wide philosophical division appears in the students. Almost all of them agree that there is really nothing that they can do for the scientists. The more engineering-minded of the students, seeing no hard and fast solution, stop there. The rest, who are somewhat more fanciful in their thoughts, say ...maybe 'nothing' is exactly the right thing to deliver, providing it is served up in the proper package. They have articulated an important thought. Not being able to solve any one scientist's problems, they nevertheless feel that they can provide tools in which the thinker can describe his own solutions and that these tools need not treat specifically any given area of discourse."
        "The latter group of students has come to look at a computing engine not as a device to solve differential equations, nor to process data in any given way, but rather as an abstraction of a well-defined universe which may resemble other well-known universes to any necessary degree. When viewed from this vantage point, it is seen that some models may be less interesting than the basic machine (payroll programs). Others may be more interesting (simulation of new designs, etc.). Finally, when they notice that the power of modeling can extend to simulate a communications network, an entirely new direction for providing a system is suggested. While they may not know the jargon and models of an abstruse field, yet possibly enough in general of human communications is known for a meta-system to he created in which the specialist himself may describe the symbol system necessary to his work. In order for a tool such as this to be useful, several constraints must be true.  1) The communications device must be as available (in every way) as a slide rule.  2) The service must not be esoteric to use. (It must be learnable in private.)  3) The transactions must inspire confidence. (Kindness should be an integral part.)"]
      (:prose (first (pull-quotes sample-md-hiccup))))
     "blockquotes should be pulled out of markdown input and assigned values as distinct entities")
    )
  )


(t/deftest edn
  (t/testing "edn delimiters"
    (t/is (= (etn->edn "◊(+ 1 1)") '(+ 1 1))
          "The lozenge special character should denote a clojure form.")
    (t/is (= (etn->edn "text can contain code like: ◊(+ 1 1) within it") '(+ 1 1))
          "Clojure forms should be parsed mid-text without affecting the other contents.")
    (t/is (= (etn->edn "(+ 2 3)") "(+ 2 3)")
          "Ordinary text should be ignored.")))
