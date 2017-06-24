(ns datamos.messaging
  (:require [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lc]
            [taoensso.nippy :as nippy]
            [datamos.util :as u]))

; todo: configure queue
; todo: bind queue to exchange
; todo: run command with local var
; todo: build set-queue function
; todo: add set-exchange to datamos.core -main
; todo: remove set-messaging component
; todo: rework set-exchange to use only one parameter for settings

(def exchange-types
  "Select one to define the way the exchange functions"
  {:datamos-cfg/direct "direct" :datamos-cfg/fanout "fanout" :datamos-cfg/topic "topic" :datamos-cfg/headers "headers"})

(def default-exchange-settings
  "Defautl settings for the exchange"
  {:auto-delete false :durable true :internal false})

(def default-queue-settings
  "Defautl settings for the exchange"
  {:durable true :auto-delete false :exclusive false})

(def exchange-conf
  (let [ex-type     (:datamos-cfg/headers exchange-types)
        ex-settings default-exchange-settings]
    {:datamos-cfg/exchange {:datamos-cfg/exchange-name     "dataMos-ex"
                            :datamos-cfg/exchange-type     ex-type
                            :datamos-cfg/exchange-settings ex-settings}}))

(def queue-conf
  (let [qu-settings default-queue-settings]
    {:datamos-cfg/queue {:datamos-cfg/queue-settings qu-settings}}))

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

(defn configure-exchange
  "configures the exchange"
  [{{:keys [:datamos-cfg/channel]} :datamos-cfg/connection {:keys [:datamos-cfg/exchange-name :datamos-cfg/exchange-type :datamos-cfg/exchange-settings]} :datamos-cfg/exchange :as settings}]
  (le/declare channel exchange-name exchange-type exchange-settings))

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

(defn set-channel
  []
  (let [conn (connection)
        chan (channel conn)]
    {:datamos-cfg/connection {:datamos-cfg/low-level-connection conn
                              :datamos-cfg/channel    chan}}))

(defn set-exchange
  [config]
  (let [ex-settings exchange-conf
        connection  (if (instance? com.rabbitmq.client.impl.recovery.AutorecoveringChannel (get-in config [:datamos-cfg/connection :datamos-cfg/channel]))
                      (select-keys config [:datamos-cfg/connection])
                      (set-channel))
        ex-config (u/deep-merge connection ex-settings config)]
    (println ex-config)
    (configure-exchange ex-config)
    ex-config))

(defn set-queue
  [queue-cfg settings])

(defn stop-exchange
  [{{:keys [:datamos-cfg/channel]} :datamos-cfg/connection {:keys [:datamos-cfg/exchange-name]} :datamos-cfg/exchange :as settings}]
  (le/delete channel exchange-name))

(defn stop-connection
  [{{:keys [:datamos-cfg/channel :datamos-cfg/low-level-connection]} :datamos-cfg/connection :as settings}]
  (when (rmq/open? channel) (rmq/close channel))
  (when (rmq/open? low-level-connection) (rmq/close low-level-connection)))

(comment
  (defn set-messaging-component
   "Provide keyword to set up the requested component. Returns a map with component references. Applicable keys:
    :channel, :exchange, :queue
    Provide settings from previous connections when available."
   ([keyword] (set-messaging-component keyword nil))
   ([keyword settings]
    (case keyword
      :channel {:connection (set-channel)}
      :exchange (set-exchange exchange-conf settings)
      :queue (set-queue queue-conf settings)
      (str "Keyword " keyword "is different from the ones provided. Please select one of:
    :exchange, :queue")))))

(defn message-handler
  [ch metadata ^bytes payload]
  )

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))