(ns datamos.core
  (:gen-class)
  (:require [datamos.messaging :as dm]
            [datamos.util :as u]))

(def component-settings
  (atom {}))

(def config-queue-settings
  (atom {}))

(def core-identifiers
  {:datamos-cfg/component {:datamos-cfg/component-uri :datamos-fn/core
                           :datamos-cfg/component-alias :datamos-fn/config}})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! component-settings
    (dm/start-messaging-connection core-identifiers)))

