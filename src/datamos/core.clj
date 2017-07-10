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
                           :datamos-cfg/component-fn :datamos-fn/config}})

(def config-identifiers
  {:datamos-cfg/queue {:datamos-cfg/queue-name "config.datamos-fn"}})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! component-settings
    (dm/start-messaging-connection core-identifiers))
  (reset! config-queue-settings
          (dm/start-config-queue config-identifiers @component-settings)))

