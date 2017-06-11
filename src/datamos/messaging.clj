(ns datamos.messaging
  (:require [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lc]
            [taoensso.nippy :as nippy]))

(def exchange-types
  "Select one to define the way the exchange functions"
  {:direct "direct" :fanout "fanout" :topic "topic" :headers "headers"})

(def exchange-options
  "Default settings for the exchange, adjust accordingly"
  {:auto-delete false :durable false :internal false})

(def rdf-function-name
  {:fn-sparql    {:fn :volentafn/sparql-query-builder}
   :fn-end-point {:fn :volentafn/http-end-point}})

(defn connection
  []
  (rmq/connect))

(defn channel
  [connection]
  (lch/open connection))

(defn ex-type
  ([] exchange-types)
  ([keyword]
   (keyword exchange-types)))

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
  "Makes a queue available via an specific exchange.
  ch - The direct communication channel, an OBJECT, between the software component and RabbitMQ
  queue-name - The name of the queue, a STRING
  exchange - The name of the exchange, a STRING
  arguments - A set, a MAP, of keys and values, defining the kind of messages the component wants to receive."
  ([ch queue-name exchange]
   (lq/bind ch queue-name exchange))
  ([ch queue-name exchange arguments]
   (lq/bind ch queue-name exchange {:arguments arguments})))

(defn subscribe-message-handler
  ([ch exchange handler queue-name arguments] (subscribe-message-handler ch exchange handler queue-name arguments {:auto-ack true} {:exclusive false :auto-delete true}))
  ([ch exchange handler queue-name arguments subscr-settings queue-settings]
   (define-queue ch queue-name queue-settings)
   (bind-queue-exchange ch queue-name exchange arguments)
   (subscribe-queue ch queue-name handler subscr-settings)))

(defn server-named-queue
  ([ch] (server-named-queue ch {:exclusive true :durable false :auto-delete true}))
  ([ch settings]
   (lq/declare-server-named ch settings)))

(defn send-message
  ([ch local-queue message exchange headers] (send-message ch local-queue message exchange headers ""))
  ([ch local-queue message exchange headers remote-queue]
   (lb/publish ch exchange remote-queue
               (nippy/freeze {:contents message :return-queue local-queue})
               {:type    "frozen-nippy"
                :headers headers})))

(defn message-handler
  [ch metadata ^bytes payload]
  )

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))