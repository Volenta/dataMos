(ns datamos.core
  (:gen-class)
  (:require [datamos
             [messaging :as dm]
             [communication :as dcom]
             [util :as u]
             [rdf-content :as rdf-cnt]
             [rdf-function :as rdf-fn]
             [core-vars :refer :all]]
            [clojure.repl :refer :all]))

(def config-identifiers
  {:datamos-cfg/queue {:datamos-cfg/queue-name "config.datamos-fn"}})

(def registry-predicates-set
  #{:rdf/type :rdfs/label :dms-def/provides})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! local-settings
          (dm/start-messaging-connection (dcom/set-component :datamos-fn/core :datamos-fn/registry)))
  (swap! local-settings u/deep-merge (dcom/open-local-channel @local-settings))
  (swap! local-settings u/deep-merge (dcom/listen @local-settings))
  (reset! global-config (select-keys @local-settings [:datamos-cfg/exchange]))
  (reset! config-queue-settings
          (dm/start-config-queue config-identifiers @local-settings))
  (swap! config-queue-settings u/deep-merge (dcom/open-local-channel @config-queue-settings))
  (swap! config-queue-settings u/deep-merge (dcom/listen @config-queue-settings)))