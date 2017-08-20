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
             [base :as base]
             [rdf-function :as rdf-fn]]
            [taoensso.nippy :as nippy]
            [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log])
  (:import [com.rabbitmq.client AlreadyClosedException]))

(def connection-object-set
  #{com.novemberain.langohr.Connection
    com.rabbitmq.client.impl.recovery.AutorecoveringChannel})

(def exchange-types
  "Select one to define the way the exchange functions"
  {:datamos/direct "direct" :datamos/fanout "fanout" :datamos/topic "topic" :datamos/headers "headers"})

(def default-exchange-settings
  "Defautl settings for the exchange"
  {:auto-delete false :durable true :internal false})

(def default-queue-settings
  "Defautl settings for the exchange"
  {:durable true :auto-delete false :exclusive false})

(def exchange-conf
  (let [ex-type     (:datamos/headers exchange-types)
        ex-settings default-exchange-settings]
    {:datamos/exchange-name     "dataMos-ex"
     :datamos/exchange-type     ex-type
     :datamos/exchange-settings ex-settings}))

(def queue-conf
  (let [qu-settings default-queue-settings]
    {:datamos/queue-settings qu-settings}))

(defn base-component-state-reference
  []
  base/component)

(defn rmq-connection
  []
  (rmq/connect))

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
          :start (rmq-connection)
          :stop (close connection))

(defn remote-channel
  [conn-settings]
  (lch/open conn-settings))

(defn vector-connection->channel
  [setting-values]
  (update setting-values 0 remote-channel))

(defn provide-channel
  "Takes the function and its settings map and a collection of keys. Provides a channel on top of an existing connection
  for function execution. Channel is closed after use"
  [f m conn-settings ks]
  (let [params (mapv m ks)
        chan   (remote-channel conn-settings)]
    (log/debug "@provide-channel" m params ks)
    (log/trace "@provide-channel" (log/get-env))
    (->> (apply f chan params)
         (close chan))))

(defn set-queue
  "Create and name an RabbitMQ Queue. Return map with queue settings"
  [conn-settings settings-map]
  (let [v (if (settings-map :datamos/queue-name nil)
            (let [s (merge settings-map queue-conf)] s)
            (let [qualified-name (u/component->queue-name settings-map)
                  queue-map      {:datamos/queue-name qualified-name}
                  s              (merge queue-map queue-conf)] s))]
    (log/debug "@set-queue" v)
    (log/trace "@set-queue" (log/get-env))
    (provide-channel lq/declare
                     v
                     conn-settings
                     [:datamos/queue-name
                      :datamos/queue-settings])
    v))

(defn remove-queue
  "Removes queue based on :datamos/queue-name key as supplied by settings map."
  [conn-settings settings]
  (provide-channel lq/delete settings conn-settings [:datamos/queue-name]))

(defstate queue
          :start (set-queue connection base/component)
          :stop (remove-queue connection queue))

(defn set-exchange
  "Creates the rabbitMQ exchange. Uses the values supplied. If not, it uses the default supplied values."
  [conn-settings]
  (do
    (log/trace "@set-exchange" (log/get-env))
    (provide-channel le/declare
                     exchange-conf
                     conn-settings
                     [:datamos/exchange-name
                      :datamos/exchange-type
                      :datamos/exchange-settings])
    exchange-conf))

(defn remove-exchange
  "Removes the RabbitMQ Exchange"
  [conn-settings settings]
  (provide-channel le/delete
                   settings
                   conn-settings
                   [:datamos/exchange-name]))

(defstate ^{:on-reload :noop} exchange
          :start (set-exchange connection)
          :stop (remove-exchange connection exchange))

(defn bind-queue
  [conn-settings component-settings exch q]
  (let [cs-subset (rdf-fn/get-predicate-object-map component-settings)
        routing-vals (into (select-keys cs-subset
                                        [:dmsfn-def/module-type
                                         :dmsfn-def/module-name])
                           [[:dmsfn-def/module-id (rdf-fn/get-subject component-settings)]])
        routing-args    (into {} (map #(mapv u/keyword->string %) routing-vals))
        header-matching {"x-match" "any"}
        args            {:arguments (conj routing-args header-matching)}
        s               (merge exch (assoc q :datamos/binding args))]
    (log/debug "@bind-queue" cs-subset routing-vals routing-args header-matching args s)
    (log/trace "@bind-queue" (log/get-env))
    (provide-channel lq/bind
                     s
                     conn-settings
                     [:datamos/queue-name
                      :datamos/exchange-name
                      :datamos/binding])
    s))

(defn remove-binding
  [conn-settings settings]
  (provide-channel lq/unbind
                   settings
                   conn-settings
                   [:datamos/queue-name
                    :datamos/exchange-name]))

(defstate bind
          :start (bind-queue connection base/component exchange queue)
          :stop (remove-binding connection bind))

(defn send-message-by-header
  [conn-settings exchange-settings message]
  (do
    (log/debug "@send-message-by-header" message)
    (log/trace "@send-message-by-header" (log/get-env))
    (let [predicates [:dmsfn-def/module-id :dmsfn-def/module-name :dmsfn-def/module-type]
          msg-header (:datamos/logistic message)
          [[[rcpt-type rcpt]]] (keep (fn [m]
                                   (keep #(find m %) predicates))
                                 (rdf-fn/get-predicate-object-map-by-value msg-header :dms-def/recipient))
          m          (nippy/freeze message)]
      (apply lb/publish
             (remote-channel conn-settings)
             (:datamos/exchange-name exchange-settings)
             ["" m {:headers (conj {}
                                   (mapv u/keyword->string
                                         [rcpt-type rcpt]))}]))))

(defn send-message-by-queue
  [conn-settings exchange-settings message]
  (do
    (log/debug "@send-message-by-queue" message)
    (log/trace "@send-message-by-queue" (log/get-env))
    (let [predicate   #{:dmscfg-def/rcpt-queue}
          msg-header  (:datamos/logistic message)
          destination (rdf-fn/value-from-nested-map (rdf-fn/predicate-filter msg-header predicate))
          m           (nippy/freeze message)]
      (lb/publish
        (remote-channel conn-settings)
        (:datamos/exchange-name exchange-settings)
        destination
        m))))

(defn send-message
  [conn-settings exchange-settings message]
  (let [predicate #{:dmscfg-def/rcpt-queue}
        msg-header (:datamos/logistic message)]
    (if (empty? (rdf-fn/predicate-filter msg-header predicate))
      (do (log/trace "@send-message - by headers" (log/get-env))
          (send-message-by-header conn-settings
                                  exchange-settings
                                  message))
      (do (log/trace "@send-message - by queue" (log/get-env))
          (send-message-by-queue conn-settings
                                 {:datamos/exchange-name ""}
                                 message)))))