(ns datamos.rdf-content
  (:require [datamos
             [util :as u]
             [rdf-prefixes :as rdfp]]
            [datamos.spec.core :as dsc]))

; TODO - Sent request with identity, and components capability (function).
; TODO - register component with name and function
; TODO - Build config for component
; TODO - Return config
; ... of type dms-def/component
; component requires datamos configuration
; component has function


(defn sign-up
  [settings]
  (let [values (u/select-submap-values settings
                                       :datamos-cfg/component-uri
                                       :datamos-cfg/component-fn)]
    {(first values) {:rdf/type         :dms-def/component
                     :dms-def/requires :dms-def/configuration
                     :dms-def/provides (last values)}}))

(defn get-prefixes
  [rdf-map]
  (select-keys @rdfp/prefixes
               (map keyword
                    (set (keep namespace
                              (filter keyword?
                                      (tree-seq coll? seq
                                                rdf-map)))))))