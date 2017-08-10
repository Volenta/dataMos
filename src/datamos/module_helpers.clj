(ns datamos.module-helpers)

(defn retrieve-prefixes
  [rdf-map]
  (map keyword
       (set (keep namespace
                  (filter keyword?
                          (tree-seq coll? seq
                                    rdf-map))))))
