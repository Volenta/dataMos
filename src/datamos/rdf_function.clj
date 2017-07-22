
(ns datamos.rdf-function
  "Contains functions working on RDF data-sets")


(defn predicate-filter
  "Returns message filtered by supplied set of rdf predicates."
  [message predicates]
  (into {}
        (map (fn [[k v]]
               [k (into {}
                        (keep (fn [[x y]]
                                (and (predicates x) {x y})) v))])
             message)))
