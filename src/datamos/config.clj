(ns datamos.config
  (:require [mount.core :as mnt :refer [defstate]]
            [datamos
             [communication :as dcom]
             [base :as cfn]
             [messaging :as dm]]
            [clojure.core.async :as async]))

(defonce ^:private config (atom {}))

(reset! config {:datamos-cfg/queue-name "config.datamos-fn"
                :datamos-cfg/response-fn println})

(defstate ^{:on-reoload :noop} config-settings :start @config)

(defstate ^{:on-reload :noop} config-connection
          :start (dm/rmq-connection)
          :stop (dm/close config-connection))

(defstate ^{:on-reload :noop} config-queue
          :start (dm/set-queue config-connection config-settings)
          :stop (dm/remove-queue config-connection config-queue))

(defstate config-local-channel
          :start (dcom/channel))

(defstate config-listener
          :start (dcom/listen config-connection config-local-channel config-queue)
          :stop (dcom/close-listen config-listener))

(defstate config-responder
          :start (dcom/response config-local-channel config-settings)
          :stop (async/close! config-responder))