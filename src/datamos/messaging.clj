(ns datamos.messaging
  (:require [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lc]
            [taoensso.nippy :as nippy]
            [datamos.util :as u])
  (:import [com.rabbitmq.client AlreadyClosedException]))


(declare close)

(def connection-object-set
  #{com.novemberain.langohr.Connection
    com.rabbitmq.client.impl.recovery.AutorecoveringChannel})

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

(defn vector-connection->channel
  [setting-values]
  (update setting-values 0 #(channel %)))

(defn ex-type
  ([] exchange-types)
  ([keyword]
   (keyword exchange-types)))

(defn close
  "Close a rabbitmq channel or connection. Returns :closed if succesfull.
  Returns :already-closed in case of an Already Closed Exception"
  [rmq-object]
  (try
    (rmq/close rmq-object) :closed
    (catch AlreadyClosedException e nil :connection-already-closed)))

(defn close-return
  "Close channel. Returns supplied settings map.
  Returns :already-closed in case of an Already Closed Exception"
  [rmq-object m]
  (try
    (rmq/close rmq-object) :closed
    m
    (catch AlreadyClosedException e nil :connection-already-closed)))

(defn provide-channel
  "Takes the function and its settings map and parameters. Supplies a channel on top of the connection
  during the execution of the function. Returns map"
  [f m & ks]
  (let [params (vector-connection->channel
                 (apply u/select-submap-values m ks))]
    (apply f params)
    (close-return (first params) m)))

(defn configure-exchange
  "configures the exchange"
  [settings]
  (provide-channel le/declare
                   settings
                   :datamos-cfg/low-level-connection
                   :datamos-cfg/exchange-name
                   :datamos-cfg/exchange-type
                   :datamos-cfg/exchange-settings))

(defn subscribe-queue
  ([ch queue-name handler] (subscribe-queue ch queue-name handler {:auto-ack true}))
  ([ch queue-name handler subscr-settings]
   (lc/subscribe ch queue-name handler subscr-settings)))

(defn define-queue
  ([ch queue-name] (define-queue ch queue-name {:auto-ack true}))
  ([ch queue-name queue-settings]
   (lq/declare ch queue-name queue-settings)))

(defn bind-queue
  [settings]
  (let [routing-vals    (u/select-subkeys settings :datamos-cfg/component-uri :datamos-cfg/component-alias)
        routing-args    (into {} (map #(mapv u/keyword->string %) routing-vals))
        header-matching {"x-match" "any"}
        args            {:arguments (conj routing-args header-matching)}
        bind-values     (vector-connection->channel
                          (u/select-submap-values settings :datamos-cfg/low-level-connection
                                                 :datamos-cfg/queue-name
                                                 :datamos-cfg/exchange-name))
        bind-settings   (conj bind-values args)]
    (apply lq/bind bind-settings)
    (close (first bind-settings))
    (assoc-in settings [:datamos-cfg/queue :datamos-cfg/binding] args)))

(comment
  (defn subscribe-message-handler
   ([ch exchange handler queue-name arguments] (subscribe-message-handler ch exchange handler queue-name arguments {:auto-ack true} {:exclusive false :auto-delete true}))
   ([ch exchange handler queue-name arguments subscr-settings queue-settings]
    (define-queue ch queue-name queue-settings)
    (bind-queue-exchange ch queue-name exchange arguments)
    (subscribe-queue ch queue-name handler subscr-settings))))

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

(defn set-connection
  []
  (let [conn (connection)]
    {:datamos-cfg/connection {:datamos-cfg/low-level-connection conn}}))

(defn create-queue
  "Creates queue with qualified name. Returns the settings map with new queue values added."
  [channel qualified-name queue-settings settings]
  (let [merge (u/deep-merge
                (->> (lq/declare channel qualified-name queue-settings)
                     ((juxt (partial u/replace-key :queue :datamos-cfg/queue-name)
                            (partial u/into-submap :datamos-cfg/queue-state [:message-count :consumer-count])))
                     (into {})
                     (u/into-submap :datamos-cfg/queue [:datamos-cfg/queue-name :datamos-cfg/queue-state]))
                settings)]
    (close channel)
    merge))

(defn set-queue
  [settings]
  (let [queue-settings queue-conf
        channel        (channel (get-in settings [:datamos-cfg/connection :datamos-cfg/low-level-connection]))]
    (if-let [qualified-name (:datamos-cfg/queue-name (:datamos-cfg/queue settings))]
      (create-queue channel qualified-name queue-settings settings)
      (let [uri-key        (get-in settings [:datamos-cfg/component :datamos-cfg/component-uri])
            main-name      (str (name uri-key) "." (namespace uri-key))
            name-uuid      (str (java.util.UUID/randomUUID))
            qualified-name (str name-uuid "." main-name)]
        (create-queue channel qualified-name queue-settings settings)))))

(defn set-exchange
  "Creates the rabbitMQ exchange. Uses the values supplied. If not, it uses the default supplied values."
  [settings]
  (let [ex-settings exchange-conf
        connection  (if (instance? com.novemberain.langohr.Connection (get-in settings [:datamos-cfg/connection
                                                                                        :datamos-cfg/low-level-connection]))
                      (select-keys settings [:datamos-cfg/connection])
                      (set-connection))
        ex-config (u/deep-merge connection ex-settings settings)]
    (configure-exchange ex-config)
    ex-config))

(defn close-connection-by-objectset
  "Given a map, takes a submap supplied by key. Filters for keys which values contain objects from the objectset.
  Closes the connections. Removes keys and values from the map. Returns the remainder of the map."
  [m k oset]
  (reduce u/remove-nested-key
          m
          (map (fn [x y]
                 (close (y (k m)))
                 [x y])
               (repeat [k])
               (keys
                 (filter #(oset (type (% 1))) (k m))))))

(defn stop-queue
  [{{:keys [:datamos-cfg/low-level-connection]} :datamos-cfg/connection {:keys [:datamos-cfg/queue-name]} :datamos-cfg/queue :as settings}]
  (try
    (lq/delete (channel low-level-connection) queue-name)
    (catch AlreadyClosedException e nil :queue-already-closed))
  (dissoc settings :datamos-cfg/queue))

(defn stop-exchange
  [{{:keys [:datamos-cfg/low-level-connection]} :datamos-cfg/connection {:keys [:datamos-cfg/exchange-name]} :datamos-cfg/exchange :as settings}]
  (try
    (le/delete (channel low-level-connection) exchange-name)
    (catch AlreadyClosedException e nil :exchange-already-closed))
  (dissoc settings :datamos-cfg/exchange))

(defn stop-connection
  "Stop channel and connection to rabbitMQ broker. Supply settings map which contains the applicable connections"
  [settings]
  (close-connection-by-objectset settings :datamos-cfg/connection connection-object-set))

(defn message-handler
  [ch metadata ^bytes payload]
  )

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))