(ns respatialized.structure.fractals
  "Namespace for fractals and other strange attractors."
  (:require
   [clojure2d.core :as clj2d]
   [fastmath.core :as m]))

(m/use-primitive-operators)

(defn clifford
  "Returns a Clifford attractor function.
   Use with iterate."
  [^Double a ^Double b ^Double c ^Double d]
  (fn [[^Double x ^Double y]]
    [(m/- (m/sin (m/* a y)) (m/* c (m/cos (m/* a x))))
     (m/- (m/sin (m/* b x)) (m/* d (m/cos (m/* b y))))]))

(defn de-jong
  "Returns a Peter de Jong attractor function.
   Use with iterate."
  [^Double a ^Double b ^Double c ^Double d]
  (fn [[^Double x ^Double y]]
    [(m/- (m/sin (m/* a y)) (m/cos (m/* b x)))
     (m/- (m/sin (m/* c x)) (m/cos (m/* d y)))]))


(def dejong1 (de-jong 1.641 1.902 0.316 1.525))
(def dejong2 (de-jong 7.201 1.316 2.114 0.701))
(def dejong3 (de-jong 1.317 2.014 0.001 2.07))
(def dejong4 (de-jong 1.412 2.924 0.901 1.38))
(def dejong5 (de-jong 1.5 1.5 1.5 1.5))
(def dejong6 (de-jong 0.517 -2.001 0 2.07))
