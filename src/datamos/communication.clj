(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [rdf-content :as rdf-cnt]
             [rdf-function :as rdf-fn]]
            [langohr
             [consumers :as lc]
             [basic :as lb]]
            [clojure.repl :refer :all]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-p]
            [taoensso.nippy :as nippy]
            [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log]))

(def default-consumer-settings
  "Default settings for the component consuming messages from the queue"
  {:auto-ack true :exclusive false})

(defn create-config-message
  [component-settings]
  (do
    (log/trace "@create-config-message" (log/get-env))
    (rdf-cnt/compose-rdf-message component-settings :datamos/registration (rdf-cnt/sign-up component-settings) "config.datamos-fn")))

(defn speak
  "Send message to another component. Component-settings is a map containing the :datamos-cfg/component-uri key.
  content is the message to be send, the rcpt is the receipient. Msg-format is :rdf or :config,
  depending on the provided content."
  ([connection-settings exchange-settings component-settings]
   (do
     (log/trace "@speak - 3arity" (log/get-env))
     (speak connection-settings exchange-settings component-settings nil nil nil nil)))
  ([connection-settings exchange-settings component-settings rcpt rcpt-type subject content]
   (let [m (if content
             (rdf-cnt/compose-rdf-message component-settings subject content rcpt rcpt-type)
             (create-config-message component-settings))]
     (log/debug "@speak - 7arity" (log/get-env))
     (dm/send-message connection-settings exchange-settings m))))

(defstate ^{:on-reload :noop} speak-connection
          :start (dm/rmq-connection)
          :stop (dm/close speak-connection))

(defn sign-up-state-reference
  []
  [speak-connection dm/exchange (dm/base-component-state-reference)])

(defn channel
  []
  (let [ch (async/chan)]
    {:datamos-cfg/listen-channel ch}))

(defstate local-channel
          :start (channel))

(defn channel-message
  [ch-map]
  (let [chan (:datamos-cfg/listen-channel ch-map)]
    (fn [ch meta ^bytes payload]
      (do
        (log/debug "@channel-message" (log/get-env))
        (async/put! chan [ch meta payload])))))

(defn listen
  [conn-settings local-ch-settings queue-settings]
  (let [dp   {:datamos-cfg/dispatch (channel-message local-ch-settings)}
        cset {:datamos-cfg/consumer-settings default-consumer-settings}
        q queue-settings
        ch {:datamos-cfg/remote-channel (dm/remote-channel conn-settings)}
        s (merge dp cset q ch)
        tag  (apply lc/subscribe
                    (mapv
                      s
                      [:datamos-cfg/remote-channel
                       :datamos-cfg/queue-name
                       :datamos-cfg/dispatch
                       :datamos-cfg/consumer-settings]))]
    (merge s {:datamos-cfg/listener-tag tag})))

(defn close-listen
  [settings]
  (apply lb/cancel
         (mapv
           settings
           [:datamos-cfg/remote-channel
            :datamos-cfg/listener-tag]))
  (dm/close (:datamos-cfg/remote-channel settings)))

(defstate listener
          :start (listen dm/connection local-channel dm/queue)
          :stop (close-listen listener))

(defn response
  [ch-map settings-map]
  (async/go
    (let [fn-map (:dms-def/provides (rdf-fn/get-predicate-object-map settings-map))]
      (while true
                (let [[ch meta payload] (async/<! (:datamos-cfg/listen-channel ch-map))
                      message (nippy/thaw payload)
                      msg-header (:datamos/logistic message)
                      subject (rdf-fn/value-from-nested-map
                                (rdf-fn/predicate-filter msg-header #{:dms-def/subject}))]
                  (do
                    (log/trace "@response" (log/get-env))
                    ((fn-map subject println) ch meta message)))))))


(defstate responder
          :start (response local-channel (dm/base-component-state-reference))
          :stop (async/close! responder))