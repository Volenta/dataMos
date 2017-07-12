(ns datamos.multi-value
  "From the Clojure Cookbook, functions to work with multiple values for a key"
  (:import (datamos.multi_value MultiAssociative)))

(defprotocol MultiAssociative
  "An associative structure for containing multiple values per key"
  (insert [m key value] "Insert a value into a MultiAssociative")
  (delete [m key value] "Remove a value from a MultiAssociative")
  (get-all [m key] "Returns a set of all values stored at key in a MultiAssociative.
  Returns the empty set if there are no values"))

(defn- value-set?
  "Helper predicate that returns true if the value is a set that represents multiple values in a MultiAssociative"
  [v]
  (and (set? v) (::multi-value (meta v))))

(defn value-set
  "Given any number of items as arguments, returns a set representing multiple values in a MultiAssociative.
  If there is only one item, simply returns the item."
  [& items]
  (if (= 1 (count items))
    (first items)
    (with-meta (set items) {::multi-value true})))

(extend-protocol MultiAssociative
  clojure.lang.Associative
  (insert [m key value]
    (let [v (get m key)]
      (assoc m key (cond
                     (nil? v) value
                     (value-set? v) (conj v value)
                     :else (value-set v value)))))
  (delete [m key value]
    (let [v (get m key)]
      (if (value-set? v)
        (assoc m key (apply value-set (disj v value)))
        (if (= v value)
          (dissoc m key)
          m))))
  (get-all [m key]
    (let [v (get m key)]
      (cond
        (value-set? v) v
        (nil v) #{}
        :else #{v}))))


