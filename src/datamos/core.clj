(ns datamos.core
  (:gen-class)
  (:require [datamos
             [communication :as dcom]
             [component-fn :as cfn]
             [messaging :as dm]
             [util :as u]
             [core-vars :refer :all]
             [system :as sys :refer [reset]]]
            [clojure.repl :refer :all]))

(cfn/component-function {:datamos-cfg/component-type :datamos-fn/core
                         :datamos-cfg/component-fn :datamos-fn/registry
                         :datamos-cfg/response-fn println})

(def config-identifiers
  {:datamos-cfg/queue {:datamos-cfg/queue-name "config.datamos-fn"}})

(def registry-predicates-set
  #{:rdf/type :rdfs/label :dms-def/provides})

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args])