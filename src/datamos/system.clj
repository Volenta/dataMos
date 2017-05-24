(ns datamos.system)

(defn clear-repl
  []
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))))

(defn remove-symbol
  [symbol]
  (ns-unmap *ns* symbol))
