(ns datamos.messaging
  (:require [langohr
             [basic :as lb]
             [channel :as lch]
             [core :as rmq]
             [queue :as lq]
             [exchange :as le]
             [consumers :as lc]]
            [clojure
             [string :as str]
             [repl :refer :all]]
            [datamos.spec.core :as dsc]
            [datamos
             [util :as u]
             [component-fn :as cfn]]
            [taoensso.nippy :as nippy]
            [mount.core :as mnt :refer [defstate]])
  (:import [com.rabbitmq.client AlreadyClosedException]))

; TODO: Sent config.

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
    {:datamos-cfg/exchange-name     "dataMos-ex"
     :datamos-cfg/exchange-type     ex-type
     :datamos-cfg/exchange-settings ex-settings}))

(def queue-conf
  (let [qu-settings default-queue-settings]
    {:datamos-cfg/queue-settings qu-settings}))

(defn unfreeze-message
  [payload]
  (nippy/thaw payload))

(defn ex-type
  ([] exchange-types)
  ([keyword]
   (keyword exchange-types)))

(defn close
  "Close a rabbitmq channel or connection. Returns :closed if succesfull.
  Returns :already-closed in case of an Already Closed Exception"
  ([rmq-object] (try
                  (rmq/close rmq-object) :closed
                  (catch AlreadyClosedException e nil :connection-already-closed)))
  ([rmq-object return]
   (try
     (rmq/close rmq-object) :closed
     (catch AlreadyClosedException e nil :connection-already-closed))
   return))

(defstate ^{:on-reload :noop} connection
          :start (rmq/connect)
          :stop (close connection))

(defn remote-channel
  []
  (lch/open connection))

(defn vector-connection->channel
  [setting-values]
  (update setting-values 0 remote-channel))

(defn provide-channel
  "Takes the function and its settings map and a collection of keys. Provides a channel on top of an existing connection
  for function execution. Channel is closed after use"
  [f m ks]
  (let [params (mapv m ks)
        chan   (remote-channel)]
    (->> (apply f chan params)
         (close chan))))

(defn set-queue
  "Create and name an RabbitMQ Queue. Return map with queue settings"
  []
  (let [qualified-name (u/component->queue-name cfn/component)
        queue-map      {:datamos-cfg/queue-name qualified-name}
        s              (u/deep-merge queue-map queue-conf)]
    (provide-channel lq/declare
                     s
                     [:datamos-cfg/queue-name
                      :datamos-cfg/queue-settings])
    s))

(defn remove-queue
  "Removes queue based on :datamos-cfg/queue-name key as supplied by settings map."
  [settings]
  (provide-channel lq/delete settings [:datamos-cfg/queue-name]))

(defstate ^{:on-reload :noop} queue
          :start (set-queue)
          :stop (remove-queue queue))

(defn set-config-queue
  [settings]
  (provide-channel lq/declare
                   settings
                   [:datamos-cfg/low-level-connection
                    :datamos-cfg/queue-name])
  settings)

(defn set-exchange
  "Creates the rabbitMQ exchange. Uses the values supplied. If not, it uses the default supplied values."
  []
  (provide-channel le/declare
                   exchange-conf
                   [:datamos-cfg/exchange-name
                    :datamos-cfg/exchange-type
                    :datamos-cfg/exchange-settings])
  exchange-conf)

(defn remove-exchange
  "Removes the RabbitMQ Exchange"
  [settings]
  (provide-channel le/delete
                   settings
                   [:datamos-cfg/exchange-name]))

(defstate ^{:on-reload :noop} exchange
          :start (set-exchange)
          :stop (remove-exchange exchange))

(defn bind-queue
  []
  (let [routing-vals    (select-keys cfn/component
                                     [:datamos-cfg/component-type
                                      :datamos-cfg/component-fn
                                      :datamos-cfg/component-uri])
        routing-args    (into {} (map #(mapv u/keyword->string %) routing-vals))
        header-matching {"x-match" "any"}
        args            {:arguments (conj routing-args header-matching)}
        s               (merge exchange (assoc queue :datamos-cfg/binding args))]
    (provide-channel lq/bind
                     s
                     [:datamos-cfg/queue-name
                      :datamos-cfg/exchange-name
                      :datamos-cfg/binding])
    s))

(defn remove-binding
  [settings]
  (provide-channel lq/unbind
                   settings
                   [:datamos-cfg/queue-name
                    :datamos-cfg/exchange-name]))

(defstate ^{:on-reload :noop} bind
          :start (bind-queue)
          :stop (remove-binding bind))

(defn request-config
  [settings message]
  (let [destination (get-in message [:datamos/logistic :datamos/rcpt-fn])
        m (nippy/freeze message)]
    (apply lb/publish
           (vector-connection->channel
             (into
               (u/select-submap-values
                 settings
                 :datamos-cfg/low-level-connection)
               ["" destination m])))))

(defn send-message
  [settings message]
  (let [destination (get-in message [:datamos/logistic :datamos/rcpt-fn])
        m (nippy/freeze message)]
    (apply lb/publish
           (vector-connection->channel
             (into
               (u/select-submap-values
                 settings
                 :datamos-cfg/low-level-connection
                 :datamos-cfg/exchange-name)
               ["" m {:headers (conj {}
                                     (mapv u/keyword->string
                                           [:datamos-cfg/component-fn destination]))}])))))