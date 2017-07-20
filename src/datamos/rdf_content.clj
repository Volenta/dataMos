(ns datamos.rdf-content
  (:require [datamos
             [util :as u]
             [rdf-prefixes :as rdfp]]
            [datamos.spec.core :as dsc]))

; TODO - Set component identity.
;   -- idenitity = datamos-fn/'type'+dms-fn+uuid example: datamos-fn/core+dms-fn+13d9c3a5-4516-4b0e-99aa-adab68672041
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

(defn generate-qualified-uri
  "Return unique uri, based on type. Type information is supplied by a settings map
  as the value of key :datamos-cfg/component-type, in a submap"
  [settings]
  (apply
    #(keyword (str (namespace %) "/" (name %) "+dms-fn+" (java.util.UUID/randomUUID)))
    (u/select-submap-values settings :datamos-cfg/component-type)))

(defn component-uri
  "Returns unique uri identifier. Based on provided :datamos-cfg/component-type value in submap"
  [settings]
  {:datamos-cfg/component {:datamos-cfg/component-uri (generate-qualified-uri settings)}})

(defn get-prefixes
  [rdf-map]
  (select-keys @rdfp/prefixes
               (map keyword
                    (set (keep namespace
                              (filter keyword?
                                      (tree-seq coll? seq
                                                rdf-map)))))))