(ns datamos.sign-up
  (:require [datamos
             [communication :as dcom]
             [messaging :as dm]
             [base :as base]]
            [mount.core :as mnt :refer [defstate]]))

(defstate ^{:on-reload :noop} send-connection
          :start (dm/rmq-connection)
          :stop (dm/close send-connection))

(defstate signed-up
          :start (dcom/speak send-connection dm/exchange base/component :config)
          :stop +)
