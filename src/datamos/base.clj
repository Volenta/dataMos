(ns datamos.base
  (:require [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log]
            [datamos.rdf-function :as rdf-fn]))

(defonce ^:private component-config (atom {}))               ; Stores the component specific configuration for first mount state.

(defn component-function
  "Give the type of component, by providing type keyword and function keyword"
  [settings]
  (reset! component-config settings))

(defn set-component
  "Returns component settings. With component-type, module-fn and module-uri as a submap of :datamos-cfg/module."
  [settings]
  (let [{:keys [:datamos-cfg/module-fn
                :datamos-cfg/module-type
                :dms-def/provides
                :dms-def/function
                :datamos-cfg/local-register]
         :or {module-type :datamos-fn/enrichment
              module-fn :datamos-fn/function}} settings]
    (log/trace "@set-component" (log/get-env))
    {(rdf-fn/generate-qualified-uri module-fn) {:datamos-cfg/module-type module-type
                                                :datamos-cfg/module-fn   module-fn
                                                :dms-def/provides           provides
                                                :datamos-cfg/local-register local-register
                                                :rdf/type                   :dms-def/module}}))

(defstate ^{:on-reload :noop} component :start (set-component @component-config))
