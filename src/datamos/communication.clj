(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [util :as u]
             [rdf-content :as rdf-cnt]
             [msg-content :as msg-cnt]
             [component-fn :as cfn]]
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

(defn speak
  ([settings content rcpt] (settings content rcpt :rdf))
  ([settings content rcpt msg-format]
   (let [cm (case msg-format
                  :rdf rdf-cnt/compose-rdf-message
                  :config msg-cnt/compose-message)]
     (println "sending message to:" rcpt "Message format:" msg-format)
     (as-> settings v
           (cm v content rcpt)
           (dm/send-message settings v)))))

(defn speak-sign-up
  "Send message to sign-up functionality and retrieve configuration. Function will deliver messages to the config.datamos-fn
  queue. Use for initialization of a component."
  [settings]
  (dm/request-config settings
                     (rdf-cnt/compose-rdf-message settings (rdf-cnt/sign-up settings) "config.datamos-fn")))

(defn channel-message
  []
  (let [chan (:datamos-cfg/listen-channel local-channel)]
    (fn [ch meta ^bytes payload]
      (async/put! chan [ch meta payload]))))

(defn listen
  []
  (let [dp  {:datamos-cfg/dispatch channel-message}
        cset {:datamos-cfg/consumer-settings default-consumer-settings}
        q dm/queue
        ch {:datamos-cfg/remote-channel (dm/remote-channel)}
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
          :start (listen)
          :stop (close-listen listener))

(defn response
  []
  (let [function  (:datamos-cfg/response-fn cfn/component)]
    (async/go (while true
                (let [[ch meta payload] (async/<! (:datamos-cfg/listen-channel local-channel))]
                  (function ch meta (nippy/thaw payload)))))))


(defstate responder
          :start (response)
          :stop (async/close! responder))