(ns datamos.module-helpers
  (:require [datamos
             [communication :as dcom]
             [messaging :as dm]]))

(defn retrieve-prefixes
  [rdf-map]
  (mapv keyword
       (set (keep namespace
                  (filter keyword?
                          (tree-seq coll? seq
                                    rdf-map))))))

(defn get-prefix-matches
  [speak-conn exchange-settings module-settings rdf-map]
  (dcom/speak speak-conn
              exchange-settings
              module-settings
              :datamos-fn/prefix
              :dms-def/module
              :datamos/match-prefix
              {:dms-def/message {:dms-def/namespaces (retrieve-prefixes rdf-map)}}))
