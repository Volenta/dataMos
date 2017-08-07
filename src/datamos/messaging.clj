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
            [mount.core :as mnt :refer [defstate]])
  (:import [com.rabbitmq.client AlreadyClosedException]))

; TODO - Check if meta information to maps is serializable by EDN.

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
    (->> (apply f chan params)
         (close chan))))

(defn set-queue
  "Create and name an RabbitMQ Queue. Return map with queue settings"
  [conn-settings settings-map]
  (let [v (if (settings-map :datamos-cfg/queue-name nil)
            (let [s (merge settings-map queue-conf)] s)
            (let [qualified-name (u/component->queue-name settings-map)
                  queue-map      {:datamos-cfg/queue-name qualified-name}
                  s              (merge queue-map queue-conf)] s))]
    (provide-channel lq/declare
                     v
                     conn-settings
                     [:datamos-cfg/queue-name
                      :datamos-cfg/queue-settings])
    v))

(defn remove-queue
  "Removes queue based on :datamos-cfg/queue-name key as supplied by settings map."
  [conn-settings settings]
  (provide-channel lq/delete settings conn-settings [:datamos-cfg/queue-name]))

(defstate queue
          :start (set-queue connection base/component)
          :stop (remove-queue connection queue))

(defn set-exchange
  "Creates the rabbitMQ exchange. Uses the values supplied. If not, it uses the default supplied values."
  [conn-settings]
  (provide-channel le/declare
                   exchange-conf
                   conn-settings
                   [:datamos-cfg/exchange-name
                    :datamos-cfg/exchange-type
                    :datamos-cfg/exchange-settings])
  exchange-conf)

(defn remove-exchange
  "Removes the RabbitMQ Exchange"
  [conn-settings settings]
  (provide-channel le/delete
                   settings
                   conn-settings
                   [:datamos-cfg/exchange-name]))

(defstate ^{:on-reload :noop} exchange
          :start (set-exchange connection)
          :stop (remove-exchange connection exchange))

(defn bind-queue
  [conn-settings component-settings exch q]
  (let [cs-subset (((first component-settings) 0) component-settings)
        routing-vals (into (select-keys cs-subset
                                        [:datamos-cfg/component-type
                                         :datamos-cfg/component-fn])
                           [[(:rdf/type cs-subset) ((first component-settings) 0)]])
        routing-args    (into {} (map #(mapv u/keyword->string %) routing-vals))
        header-matching {"x-match" "any"}
        args            {:arguments (conj routing-args header-matching)}
        s               (merge exch (assoc q :datamos-cfg/binding args))]
    (provide-channel lq/bind
                     s
                     conn-settings
                     [:datamos-cfg/queue-name
                      :datamos-cfg/exchange-name
                      :datamos-cfg/binding])
    s))

(defn remove-binding
  [conn-settings settings]
  (provide-channel lq/unbind
                   settings
                   conn-settings
                   [:datamos-cfg/queue-name
                    :datamos-cfg/exchange-name]))

(defstate bind
          :start (bind-queue connection base/component exchange queue)
          :stop (remove-binding connection bind))

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

(defn send-message-by-header
  [conn-settings exchange-settings message]
  (let [predicate #{:dms-def/component}
        msg-header (:datamos/logistic message)
        [rcpt-type rcpt] (first (rdf-fn/get-predicate-object-map (rdf-fn/predicate-filter msg-header predicate)))
        m (nippy/freeze message)]
    (apply lb/publish
           (remote-channel conn-settings)
           (:datamos-cfg/exchange-name exchange-settings)
           ["" m {:headers (conj {}
                                 (mapv u/keyword->string
                                       [rcpt-type rcpt]))}])))

(defn send-message-by-queue
  [conn-settings exchange-settings message]
  (let [predicate #{:datamos-cfg/rcpt-queue}
        msg-header (:datamos/logistic message)
        destination (rdf-fn/value-from-nested-map (rdf-fn/predicate-filter msg-header predicate))
        m (nippy/freeze message)]
    (println "@send-message-by-queue - just tell me what the exchange is:" exchange-settings)
    (lb/publish
      (remote-channel conn-settings)
      (:datamos-cfg/exchange-name exchange-settings)
      destination
      m)))

(defn send-message
  [conn-settings exchange-settings message]
  (let [predicate #{:datamos-cfg/rcpt-queue}
        msg-header (:datamos/logistic message)]
    (if (empty? (rdf-fn/predicate-filter msg-header predicate))
      (do #_(println "@send-message - yep, going for headers:" conn-settings exchange-settings message)
          (send-message-by-header conn-settings
                                  exchange-settings
                                  message))
      (do #_(println "@send-message - And now, the message is send by queue" conn-settings exchange-settings message)
          (send-message-by-queue conn-settings
                                 {:datamos-cfg/exchange-name ""}
                                 message)))))