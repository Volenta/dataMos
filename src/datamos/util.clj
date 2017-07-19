(ns datamos.util
  "Contains common utility functions"
  (:require [clojure
             [string :as str]]))

(defn deep-merge
  [& maps]
  (apply merge-with deep-merge maps))

(defn replace-key
  "Takes old-key and value from map. Replaces old-key with new-key and returns map with new-key and value."
  [old-key new-key map]
  (zipmap [new-key] (vector (get map old-key))))

(defn select-into-submap
  "Select keys, a sequence of keys, from map. Inserts these into new map, under given key. Returns new map."
  [key select-ks map]
  (into {} [[key (select-keys map select-ks)]]))

(defn select-submap-values
  "Takes all keys value pairs of submaps in map. Applies keys in given order, to return a vector of values."
  [m & ks]
  (let [mp (into {} (map m (keys m)))]
    (reduce #(conj %1 (mp %2))
            []
            ks)))

(defn select-subkeys
  [m & ks]
  (let [mp (into {} (map m (keys m)))]
    (select-keys mp ks)))

(defn select-subkeys+
  [m & ks]
  (let [mp (into {} (map m (keys m)))]
    (select-keys mp ks)))

(defn apply-submap-values
  "Gets values from map and applies function. Values will be applied in the order of the keys supplied."
  [f m & ks]
  (apply f (apply select-submap-values m ks)))

(defn keyword->string
  "Insert (namespaced) keyword, Returns (namespaced) string with colon removed"
  [keyword]
  (if (keyword? keyword)
    (str/replace (str keyword) #"^:+" "")
    keyword))

(defn string->keyword
  "Insert (namespaced) string (without leading colon), Returns keyword"
  [string]
  (keyword string))

(defn remove-nested-key
  "Takes map, keys (for keypath) and key. Returns the given map with the key removed."
  [map [ks k]]
  (update-in map ks dissoc k))

(defn give-qualified-name
  [settings k]
  (apply
    #(str (java.util.UUID/randomUUID) "." (name %) "." (namespace %))
    (select-submap-values settings k)))

(defn select-sub-keys
  "Returns a map containing only the submap entry, whose key is key, plus the parent key."
  [maps key]
  (into {}
        (remove nil?
                (map (fn [k v]
                       (and (v key) [k (select-keys v [key])])) (keys maps) (vals maps)))))

(defn replace-sub-value
  "Supply map, which contains submaps. Provide key, in submap, for which you want the value to change.
   returns supplied map with changed value. Only works for first nested layer of maps.
   Every key's value, matching k will change to the supplied value."
  [m k value]
  (apply conj
         (map
           #(or (and (some #{k} (keys (% m))) {% (assoc (% m) k value)})
                (select-keys m [%]))
           (keys m))))