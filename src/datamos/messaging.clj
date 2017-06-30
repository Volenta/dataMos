(ns datamos.messaging
  (:require [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lc]
            [taoensso.nippy :as nippy]
            [datamos.util :as u]
            [clojure.string :as s])
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

(defn provide-channel
  "Takes the function and its settings map and parameters. Provides a channel on top of an existing connection
  for function execution."
  [f m & ks]
  (let [params (vector-connection->channel
                 (apply u/select-submap-values m ks))]
    (apply f params)
    (close (first params))))

(defn bind-queue
  [settings]
  (let [routing-vals    (u/select-subkeys settings :datamos-cfg/component-uri :datamos-cfg/component-alias)
        routing-args    (into {} (map #(mapv u/keyword->string %) routing-vals))
        header-matching {"x-match" "any"}
        args            {:arguments (conj routing-args header-matching)}
        s (assoc-in settings [:datamos-cfg/queue :datamos-cfg/binding] args)]
    (provide-channel lq/bind
                     s
                     :datamos-cfg/low-level-connection
                     :datamos-cfg/queue-name
                     :datamos-cfg/exchange-name
                     :datamos-cfg/binding)
    s))

(defn set-connection
  [settings]
  (let [conn (connection)
        s {:datamos-cfg/connection {:datamos-cfg/low-level-connection conn}}]
    (u/deep-merge settings s)))

(defn set-queue
  [settings]
  (let [queue-settings queue-conf
        qualified-name (u/give-qualified-name settings :datamos-cfg/component-uri)
        queue-map {:datamos-cfg/queue {:datamos-cfg/queue-name qualified-name}}
        s (u/deep-merge settings queue-map queue-settings)]
    (provide-channel lq/declare
                     s
                     :datamos-cfg/low-level-connection
                     :datamos-cfg/queue-name
                     :datamos-cfg/queue-settings)
    s))

(defn set-exchange
  "Creates the rabbitMQ exchange. Uses the values supplied. If not, it uses the default supplied values."
  [settings]
  (let [ex-settings exchange-conf
        ex-config (u/deep-merge ex-settings settings)]
    (provide-channel le/declare
                     ex-config
                     :datamos-cfg/low-level-connection
                     :datamos-cfg/exchange-name
                     :datamos-cfg/exchange-type
                     :datamos-cfg/exchange-settings)
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
  [settings]
  (provide-channel lq/delete
                   settings
                   :datamos-cfg/low-level-connection
                   :datamos-cfg/queue-name)
  (dissoc settings :datamos-cfg/queue))

(defn stop-exchange
  [settings]
  (provide-channel le/delete
                   settings
                   :datamos-cfg/low-level-connection
                   :datamos-cfg/exchange-name)
  (dissoc settings :datamos-cfg/exchange))

(defn stop-connection
  "Stop channel and connection to rabbitMQ broker. Supply settings map which contains the applicable connections"
  [settings]
  (close-connection-by-objectset settings :datamos-cfg/connection connection-object-set)
  (dissoc settings :datamos-cfg/connection))

(defn message-handler
  [ch metadata ^bytes payload]
  )

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))