(ns datamos.rdf-function
  "Contains functions working on RDF data-sets")

(defn get-predicate-object-map
  "Takes a triple map. Returns the sub-map (= predicate and obejct)"
  [triple-map]
  ((first triple-map) 1))

(defn get-subject
  "takes a triple map. Returns the subject"
  [triple-map]
  ((first triple-map) 0))


(defn value-from-nested-map
  "Goes down the nested map structure, m. Returns first value which differs from a map"
  [m]
  (if (and (map? m) (not-empty m))
    (value-from-nested-map (apply #(% 1) m))
    m))

(defn predicate-filter
  "Returns message filtered by supplied set of rdf predicates."
  [message predicates]
  (into {}
        (map (fn [[k v]]
               (let [pmatch (into {}
                                  (keep (fn [[x y]]
                                          (and (predicates x) {x y})) v))]
                 (if (not-empty pmatch)
                   [k pmatch]
                   nil)))
             message)))

(defn message-content
  "takes a message and retrieves the message content, for RDF messages"
  [message]
  (get-in message
          [:datamos/rdf-content :datamos/triples]))

(defn subject-object-by-predicate
  "Provide triple-map and predicate. Returns vector of subject and object."
  [m predicate]
  (let [s (get-subject m)
        po (get-predicate-object-map m)
        o (get po predicate "Predicate Object is no map")]
    [s o]))

(defn values-by-predicate
  "Provide a predicate and multiple triple maps. Returns sequence of values for predicate"
  [predicate & maps]
  (for [m maps]
    (if (empty? m)
      false
      (value-from-nested-map
        (predicate-filter m #{predicate})))))