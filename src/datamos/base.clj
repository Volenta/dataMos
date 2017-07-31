(ns datamos.base
  (:require [mount.core :as mnt :refer [defstate]]))

(defonce ^:private component-config (atom {}))               ; Stores the component specific configuration for first mount state.

(defn component-function
  "Give the type of component, by providing type keyword and function keyword"
  [settings]
  (reset! component-config settings))

(defn generate-qualified-uri
  "Return unique uri, based on type-kw."
  [type-kw]
  (keyword (str (namespace type-kw) "/" (name type-kw) "+dms-fn+" (java.util.UUID/randomUUID))))

(defn set-component
  "Returns component settings. With component-type, component-fn and component-uri as a submap of :datamos-cfg/component."
  []
  (let [{:keys [:datamos-cfg/component-fn :datamos-cfg/component-type]
         :or {component-type :datamos-fn/enrichment
              component-fn :datamos-fn/function}} @component-config]
    {:datamos-cfg/component-type component-type
     :datamos-cfg/component-fn   component-fn
     :datamos-cfg/component-uri  (generate-qualified-uri component-fn)}))

(defstate ^{:on-reload :noop} component :start (set-component))
