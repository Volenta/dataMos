(ns datamos.util
  "Contains common utility functions")

(defn deep-merge
  [& maps]
  (apply merge-with deep-merge maps))

(defn replace-key
  "Takes old-key and value from map. Replaces old-key with new-key and returns map with old-key and value."
  [old-key new-key map]
  (zipmap [new-key] (vector (get map old-key))))

(defn into-submap
  "Select keys, a collection of keys, from map. Inserts these into new map, under given key. Returns new map."
  [key select-ks map]
  (into {} [[key (select-keys map select-ks)]]))


