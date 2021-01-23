(ns respatialized.util)

(defn select-values [map ks]
  (into [] (remove nil? (reduce #(conj %1 (map %2)) [] ks))))

(defmacro ^:no-doc implies
  "Logically equivalent to `(or (not condition) expression)`"
  [cause consequence]
  `(if ~cause
     ~consequence
     true))

(defn- comp-bindings [context bindings]
  (conj context bindings))

(defn- s-sequential? [x]
  (or (sequential? x)
      (string? x)))

(defn- resolve-ref [context key]
  (loop [bindings-vector context]
    (if (seq bindings-vector)
      (let [bindings (peek bindings-vector)]
        (if (contains? bindings key)
          [bindings-vector (get bindings key)]
          (recur (pop bindings-vector))))
      (throw (ex-info (str "Cannot resolve reference " key)
                      {:context context, :key key})))))

(declare valid-model?)

(defn left-overs
  "Returns a sequence of possible left-overs from the seq-data after matching the model with it."
  [context model seq-data]
  (if (and (#{:alt :cat :repeat :let :ref} (:type model))
           (:inlined model true))
    (case (:type model)
      :alt (mapcat (fn [entry]
                     (left-overs context (:model entry) seq-data))
                   (:entries model))
      :cat (reduce (fn [seqs-data entry]
                     (mapcat (fn [seq-data]
                               (left-overs context (:model entry) seq-data))
                             seqs-data))
                   [seq-data]
                   (:entries model))
      :repeat (->> (iterate (fn [seqs-data]
                              (mapcat (fn [seq-data]
                                        (left-overs context (:elements-model model) seq-data))
                                      seqs-data))
                            [seq-data])
                   (take-while seq)
                   (take (inc (:max model))) ; inc because it includes the "match zero times"
                   (drop (:min model))
                   (apply concat))
      :let (left-overs (comp-bindings context (:bindings model)) (:body model) seq-data)
      :ref (let [[context model] (resolve-ref context (:key model))]
             (left-overs context model seq-data)))
    (if (and seq-data
             (valid-model? context (dissoc model :inlined) (first seq-data)))
      [(next seq-data)]
      [])))

(defn valid-model?
  ([context model data]
   (case (:type model)
     :fn (and ((:fn model) data)
              (implies (contains? model :condition-model)
                       (valid-model? context (:condition-model model) data)))
     :enum (contains? (:values model) data)
     :and (every? (fn [entry]
                    (valid-model? context (:model entry) data))
                  (:entries model))
     (:or :alt) (some (fn [entry]
                        (valid-model? context (:model entry) data))
                      (:entries model))
     :set-of (and (set? data)
                  (implies (contains? model :count-model)
                           (valid-model? context (:count-model model) (count data)))
                  (implies (contains? model :elements-model)
                           (every? (partial valid-model? context (:elements-model model)) data))
                  (implies (contains? model :condition-model)
                           (valid-model? context (:condition-model model) data)))
     (:map-of :map) (and (map? data)
                         (implies (contains? model :entries)
                                  (every? (fn [entry]
                                            (if (contains? data (:key entry))
                                              (valid-model? context (:model entry) (get data (:key entry)))
                                              (:optional entry)))
                                          (:entries model)))
                         (implies (contains? model :entry-model)
                                  (every? (partial valid-model? context (:entry-model model)) data))
                         (implies (contains? model :condition-model)
                                  (valid-model? context (:condition-model model) data)))
     (:sequence-of :sequence) (and (s-sequential? data)
                                   ((-> (:coll-type model :any) {:any any?
                                                                 :list seq?
                                                                 :vector vector?
                                                                 :string string?}) data)
                                   (implies (contains? model :entries)
                                            (and (= (count (:entries model)) (count data))
                                                 (every? identity (map (fn [entry data-element]
                                                                         (valid-model? context (:model entry) data-element))
                                                                       (:entries model)
                                                                       data))))
                                   (implies (contains? model :count-model)
                                            (valid-model? context (:count-model model) (count data)))
                                   (implies (contains? model :elements-model)
                                            (every? (partial valid-model? context (:elements-model model)) data))
                                   (implies (contains? model :condition-model)
                                            (valid-model? context (:condition-model model) data)))
     (:cat :repeat) (and (s-sequential? data)
                         ((-> (:coll-type model :any) {:any any?
                                                       :list seq?
                                                       :vector vector?
                                                       :string string?}) data)
                         (some nil? (left-overs context (dissoc model :inlined) (seq data)))
                         (implies (contains? model :count-model)
                                  (valid-model? context (:count-model model) (count data)))
                         (implies (contains? model :condition-model)
                                  (valid-model? context (:condition-model model) data)))
     :let (valid-model? (comp-bindings context (:bindings model)) (:body model) data)
     :ref (let [[context model] (resolve-ref context (:key model))]
            (valid-model? context model data))))
  ([model data] (valid-model? [] model data)))
