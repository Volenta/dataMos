(ns datamos.core
  (:gen-class)
  (:require [datamos.messaging :as dm]
            [datamos.util :as u]))

(def component-settings
  (atom {}))

(def component-identifiers
  {:datamos-cfg/component {:datamos-cfg/component-uri :datamos-fn/core
                           :datamos-cfg/component-alias :datamos-fn/config}})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset! component-settings component-identifiers)
  (reset! component-settings (dm/set-exchange @component-settings))
  (reset! component-settings (dm/set-queue @component-settings)))

