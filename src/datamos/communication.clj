(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [util :as u]]
            [langohr
             [consumers :as lc]]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-p]
            [taoensso.nippy :as nippy]))

(def default-consumer-settings
  "Default settings for the component consuming messages from the queue"
  {:auto-ack true :exclusive false})

(defn retrieve-sender
  [settings]
  (first (u/select-submap-values settings :datamos-cfg/component-uri)))

(defn compose-message
  [settings content]
  (let [s (retrieve-sender settings)]
    {:datamos/logistic    {:datamos/rcpt-fn :datamos-fn/registry
                           :datamos/sender s}
     :datamos/rdf-content {:datamos/prefix  {}
                           :datamos/triples content}}))

(defn open-local-channel
  [settings]
  (let [ch (async/chan)]
    {:datamos-cfg/listener {:datamos-cfg/listen-channel ch}}))

(defn speak
  [settings content]
  (as-> settings v
      (compose-message v content)
      (dm/send-message settings v)))

(defn channel-message
  [channel]
  (fn [ch meta ^bytes payload]
    (async/put! channel [ch meta payload])))

(defn get-local-channel
  [settings]
  (get-in settings [:datamos-cfg/listener :datamos-cfg/listen-channel]))

(defn listen
  [settings]
  (let [chan (get-local-channel settings)
        tag  (apply
               lc/subscribe
               (dm/vector-connection->channel
                 (into
                   (u/select-submap-values settings
                                           :datamos-cfg/low-level-connection
                                           :datamos-cfg/queue-name)
                   [(channel-message chan)
                    default-consumer-settings])))]
    {:datamos-cfg/listener {:datamos-cfg/listener-tag tag}}))

(defn response
  [settings function]
  (let [chan (get-local-channel settings)]
    (async/go (while true
                (let [[ch meta payload] (async/<! chan)]
                  (when (not (async-p/closed? chan))
                    (function ch meta (nippy/thaw payload))))))))

(defn close-local-channel
  [settings]
  (let [chan (get-local-channel settings)]
    (async/close! chan)))