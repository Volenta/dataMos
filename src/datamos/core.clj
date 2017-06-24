(ns datamos.core
  (:gen-class)
  (:require [datamos.messaging :as dm]
            [datamos.util :as u]))

(def connection-settings
  (atom {}))

(def component-settings
  {:datamos-cfg/component {:datamos-cfg/component-uri :datamos-fn/leader
                           :datamos-cfg/component-alias :datamos-fn/config}})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! connection-settings component-settings)
  (reset! connection-settings (dm/set-exchange @connection-settings)))

