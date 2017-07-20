(ns datamos.core
  (:gen-class)
  (:require [datamos
             [messaging :as dm]
             [communication :as dcom]
             [util :as u]
             [rdf-content :as rdf]]
            [clojure.repl :refer :all]))

(def component-settings
  (atom {}))

(def config-queue-settings
  (atom {}))

(def core-identifiers
  {:datamos-cfg/component {:datamos-cfg/component-type :datamos-fn/core
                           :datamos-cfg/component-fn :datamos-fn/registry}})

(def config-identifiers
  {:datamos-cfg/queue {:datamos-cfg/queue-name "config.datamos-fn"}})


(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! component-settings
          (dm/start-messaging-connection (u/deep-merge (rdf/component-uri core-identifiers) core-identifiers)))
  (swap! component-settings u/deep-merge (dcom/open-local-channel @component-settings))
  (swap! component-settings u/deep-merge (dcom/listen @component-settings))
  (reset! config-queue-settings
          (dm/start-config-queue config-identifiers @component-settings))
  (swap! config-queue-settings u/deep-merge (dcom/open-local-channel @config-queue-settings))
  (swap! config-queue-settings u/deep-merge (dcom/listen @config-queue-settings)))