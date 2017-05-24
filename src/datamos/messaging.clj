(ns datamos.messaging
  (:require [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lc]
            [taoensso.nippy :as nippy]))

(defn connection
  []
  (rmq/connect))

(defn channel
  [connection]
  (lch/open connection))

(defn define-exchange
  "configures the exchange"
  ([ch exchange type] (define-exchange ch exchange type {:durable false :auto-delete true}))
  ([ch exchange type settings]
    (le/declare ch exchange type settings)))

(defn subscribe-queue
  ([ch queue-name handler] (subscribe-queue ch queue-name handler {:auto-ack true}))
  ([ch queue-name handler subscr-settings]
   (lc/subscribe ch queue-name handler subscr-settings)))

(defn define-queue
  ([ch queue-name] (define-queue ch queue-name {:auto-ack true}))
  ([ch queue-name queue-settings]
   (lq/declare ch queue-name queue-settings)))

(defn bind-queue-exchange
  [ch queue-name exchange]
  (lq/bind ch queue-name exchange))

(defn message-handler
  ([ch exchange handler queue-name] (message-handler ch exchange handler queue-name {:auto-ack true} {:exclusive false :auto-delete true}))
  ([ch exchange handler queue-name subscr-settings] (message-handler ch exchange handler queue-name subscr-settings {:exclusive false :auto-delete true}))
  ([ch exchange handler queue-name subscr-settings queue-settings]
   (define-queue ch queue-name queue-settings)
   (bind-queue-exchange ch queue-name exchange)
   (subscribe-queue ch queue-name handler subscr-settings)))

(defn server-named-queue
  ([ch] (server-named-queue ch {:exclusive true :durable false :auto-delete true}))
  ([ch settings]
    (lq/declare-server-named ch settings)))

(defn send-message
  ([ch local-queue message exchange] (send-message ch local-queue message exchange ""))
  ([ch local-queue message exchange remote-queue]
    (lb/publish ch exchange remote-queue
                (nippy/freeze {:contents message :return-queue local-queue})
                {:type "frozen-nippy"})))

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))
