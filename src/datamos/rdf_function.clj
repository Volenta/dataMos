(ns datamos.rdf-function
  "Contains functions working on RDF data-sets"
  (:require [datamos.util :as u]))

(defn generate-qualified-uri
  "Return unique uri, based on type-kw."
  [type-kw]
  (keyword (str (namespace type-kw) "/" (name type-kw) "+dms-fn+" (u/return-uuid))))

(defn get-predicate-object-map
  "Takes a triple map. Returns the sub-map (= predicate and object)"
  [triple-map]
  (apply second triple-map))

(defn get-subject
  "takes a triple map. Returns the subject"
  [triple-map]
  (apply first triple-map))


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
    (when (seq m)
      (map
        (fn [x] (value-from-nested-map
                  (predicate-filter (conj {} x) #{:dms-def/function})))
        m))))

(defn get-message-id-from-content
  "Returns the message id from the message content"
  [message]
  (:dms-def/message-id (:dms-def/message (:datamos/triples (:datamos/rdf-content message)))))