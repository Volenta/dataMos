(ns datamos.util
  "Contains common utility functions")

(defn deep-merge
  [& maps]
  (apply merge-with deep-merge maps))


