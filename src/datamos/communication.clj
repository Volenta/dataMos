(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [util :as u]
             [rdf-content :as rdf-cnt]
             [msg-content :as msg-cnt]
             [base :as base]]
            [langohr
             [consumers :as lc]
             [basic :as lb]]
            [clojure.repl :refer :all]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-p]
            [taoensso.nippy :as nippy]
            [mount.core :as mnt :refer [defstate]]))

; TODO : Reduce speak and speak-rdf to speak. Add parameter to speak to specifiy what kind of content is send.
;        Allow function to follow a different path based on the value supplied for this parameter.

(def default-consumer-settings
  "Default settings for the component consuming messages from the queue"
  {:auto-ack true :exclusive false})

(defn channel
  []
  (let [ch (async/chan)]
    {:datamos-cfg/listen-channel ch}))

(defstate local-channel
          :start (channel))

(defn create-config-message
  [component-settings]
  (rdf-cnt/compose-rdf-message component-settings (rdf-cnt/sign-up component-settings) "config.datamos-fn"))

(defn speak
  "Send message to another component. Component-settings is a map containing the :datamos-cfg/component-uri key.
  content is the message to be send, the rcpt is the receipient. Msg-format is :rdf or :config,
  depending on the provided content."
  ([connection-settings exchange-settings component-settings]
   (speak connection-settings exchange-settings component-settings :rdf))
  ([connection-settings exchange-settings component-settings msg-format]
   (let [generate-message (case msg-format
                            :rdf rdf-cnt/compose-rdf-message
                            :config create-config-message)]
     (println "Message format:" msg-format)
     (dm/send-message connection-settings exchange-settings
                      (generate-message component-settings)))))

(comment
  (defn speak-sign-up
   "Send message to sign-up functionality and retrieve configuration. Function will deliver messages to the config.datamos-fn
   queue. Use for initialization of a component."
   [settings]
   (dm/request-config settings)))

(defn channel-message
  [ch-map]
  (let [chan (:datamos-cfg/listen-channel ch-map)]
    (fn [ch meta ^bytes payload]
      (async/put! chan [ch meta payload]))))

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
  (let [function  (:datamos-cfg/response-fn settings-map)]
    (async/go (while true
                (let [[ch meta payload] (async/<! (:datamos-cfg/listen-channel ch-map))]
                  (function ch meta (nippy/thaw payload)))))))


(defstate responder
          :start (response local-channel base/component)
          :stop (async/close! responder))