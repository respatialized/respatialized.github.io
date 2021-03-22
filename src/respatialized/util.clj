(ns respatialized.util)

(defn select-values [map ks]
  (into [] (remove nil? (reduce #(conj %1 (map %2)) [] ks))))
