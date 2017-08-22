(ns datamos.base
  (:require [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log]
            [datamos.rdf-function :as rdf-fn]))

(declare component)

(defonce ^:private component-config (atom {}))               ; Stores the component specific configuration for first mount state.

(defn component-function
  "Give the type of component, by providing type keyword and function keyword"
  [settings]
  (reset! component-config settings))

(defn set-component
  "Returns component settings. With component-type, module-fn and module-uri as a submap of :datamos-cfg/module."
  [settings]
  (let [{:keys [:dmsfn-def/module-name
                :dmsfn-def/module-type
                :dms-def/provides
                :datamos/local-register]
         :or   {module-type :dmsfn-def/enrichment
                module-name :dmsfn-def/module}} settings]
    (log/trace "@set-component" (log/get-env))
    {(rdf-fn/generate-qualified-uri module-name) {:dmsfn-def/module-type  module-type
                                                  :dmsfn-def/module-name  module-name
                                                  :dms-def/provides       provides
                                                  :datamos/local-register local-register
                                                  :rdf/type               :dmsfn-def/module}}))

(defstate ^{:on-reload :noop} component :start (set-component @component-config))
