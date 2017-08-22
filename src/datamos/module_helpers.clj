(ns datamos.module-helpers
  (:require [datamos
             [rdf-function :as rdf-fn]
             [messaging :as dm]]
            [taoensso.timbre :as log]))

(defn retrieve-prefixes
  [rdf-map]
  (mapv keyword
       (set (keep namespace
                  (filter keyword?
                          (tree-seq coll? seq
                                    rdf-map))))))

(defn private-register
  [lr]
  (fn [_ _ message]
    (let [rdf-content (rdf-fn/message-content message)]
      (log/debug "@private-register" rdf-content)
      (log/trace "@private-register" (log/get-env))
      (reset! lr rdf-content))))

(defn local-module-register
  [local-register]
  {:datamos-fn/registry (private-register local-register)})