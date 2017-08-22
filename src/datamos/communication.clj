(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [rdf-content :as rdf-cnt]
             [rdf-function :as rdf-fn]
             [module-helpers :as hlp]
             [msg-functions :as msg-fn]]
            [langohr
             [consumers :as lc]
             [basic :as lb]]
            [clojure.repl :refer :all]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-p]
            [taoensso.nippy :as nippy]
            [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log]))

(declare get-prefix-matches speak-connection local-channel listener responder)

(def register-fn-keywords
  #{:datamos-fn/registration
    :datamos-fn/de-register
    :datamos-fn/registry
    nil})

(def default-consumer-settings
  "Default settings for the component consuming messages from the queue"
  {:auto-ack true :exclusive false})

(defn create-config-message
  [component-settings]
  (do
    (log/trace "@create-config-message" (log/get-env))
    (rdf-cnt/compose-rdf-message component-settings :datamos-fn/registration (rdf-cnt/sign-up component-settings) "config.datamos-fn")))

(defn retrieve-prefix-list
  "Gets namespaces from content and calls speak to send message for a list of prefixes"
  [connection-settings exchange-settings component-settings content m-id]
  (async/go (get-prefix-matches connection-settings exchange-settings component-settings content m-id)))

(defn generate-message-minus-prefixes
  "Returns a message, with header and contents."
  [component-settings subject content rcpt rcpt-type m-id]
  (do
    (log/debug "@generate-message-minus-prefixes" subject content rcpt rcpt-type m-id)
    (log/trace "@generate-message-minus-prefixes" (log/get-env))
    (if content
      (rdf-cnt/compose-rdf-message component-settings subject content rcpt rcpt-type m-id)
      (create-config-message component-settings))))

(defn store-message
  "Stores message while retrieving prefixes."
  [connection-settings exchange-settings component-settings subject content rcpt rcpt-type]
  (let [m-id    (rdf-cnt/generate-message-id)
        message (generate-message-minus-prefixes component-settings subject content rcpt rcpt-type m-id)
        cnt     (rdf-fn/message-content message)]
    (log/debug "@store-message" subject content rcpt rcpt-type)
    (log/trace "@store-message" (log/get-env))
    (retrieve-prefix-list connection-settings exchange-settings component-settings cnt m-id)
    (swap! msg-fn/message-store assoc m-id message)))

(defn generate-message-with-one-time-id
  "Generates a message with an unique ID, just for this message"
  [connection-settings exchange-settings component-settings subject content rcpt rcpt-type]
  (let [m-id (rdf-cnt/generate-message-id)]
    (log/debug "@generate-message-with-one-time-id" subject content rcpt rcpt-type)
    (log/trace "@generate-message-with-one-time-id" (log/get-env))
    (dm/send-message connection-settings exchange-settings
                     (generate-message-minus-prefixes component-settings subject content rcpt rcpt-type m-id))))

(defn speak
  "Send message to another component. Component-settings is a map containing the :datamos-cfg/module-uri key.
  content is the message to be send, the rcpt is the receipient. Msg-format is :rdf or :config,
  depending on the provided content."
  ([connection-settings exchange-settings component-settings]
   (speak connection-settings exchange-settings component-settings nil nil nil nil))
  ([connection-settings exchange-settings component-settings rcpt rcpt-type subject content]
   (do
     (log/debug "@speak" "\n rcpt:" rcpt "\n   rcpt-type:" rcpt-type "\n   subject:" subject "\n   content:" content)
     (log/trace "@speak" (log/get-env))
     (cond
       (contains? register-fn-keywords subject) (generate-message-with-one-time-id connection-settings exchange-settings component-settings subject content rcpt rcpt-type)
       (= :datamos-fn/match-prefix subject) (generate-message-with-one-time-id connection-settings exchange-settings component-settings subject content rcpt rcpt-type)
       (= :datamos-fn/prefix-list subject) (generate-message-with-one-time-id connection-settings exchange-settings component-settings subject content rcpt rcpt-type)
       :else (store-message connection-settings exchange-settings component-settings subject content rcpt rcpt-type)))))

(defn get-prefix-matches
  [speak-conn exchange-settings module-settings rdf-map msg-id]
  (speak speak-conn
         exchange-settings
         module-settings
         :dmsfn-def/prefix
         :dmsfn-def/module-name
         :datamos-fn/match-prefix
         {:dms-def/message {:dms-def/namespaces (hlp/retrieve-prefixes rdf-map)
                            :dms-def/message-id msg-id}}))

(defstate ^{:on-reload :noop} speak-connection
          :start (dm/rmq-connection)
          :stop (dm/close speak-connection))

(defn sign-up-state-reference
  []
  [speak-connection dm/exchange (dm/base-component-state-reference)])

(defn channel
  []
  (let [ch (async/chan)]
    {:datamos/listen-channel ch}))

(defstate local-channel
          :start (channel))

(defn channel-message
  [ch-map]
  (let [chan (:datamos/listen-channel ch-map)]
    (fn [ch meta ^bytes payload]
      (do
        (log/debug "@channel-message" meta)
        (log/trace "@channel-message" (log/get-env))
        (async/put! chan [ch meta payload])))))

(defn listen
  [conn-settings local-ch-settings queue-settings]
  (let [dp   {:datamos/dispatch (channel-message local-ch-settings)}
        cset {:datamos/consumer-settings default-consumer-settings}
        q    queue-settings
        ch   {:datamos/remote-channel (dm/remote-channel conn-settings)}
        s    (merge dp cset q ch)
        tag  (apply lc/subscribe
                    (mapv
                      s
                      [:datamos/remote-channel
                       :datamos/queue-name
                       :datamos/dispatch
                       :datamos/consumer-settings]))]
    (merge s {:datamos/listener-tag tag})))

(defn close-listen
  [settings]
  (apply lb/cancel
         (mapv
           settings
           [:datamos/remote-channel
            :datamos/listener-tag]))
  (dm/close (:datamos/remote-channel settings)))

(defstate listener
          :start (listen dm/connection local-channel dm/queue)
          :stop (close-listen listener))

(defn add-message-prefixes
  [_ _ message]
  (let [content (rdf-fn/message-content message)]
    (msg-fn/get-from-message-store speak-connection dm/exchange content)))

(def base-functions {:datamos-fn/prefix-list add-message-prefixes})

(defn response
  [ch-map settings-map]
  (async/go
    (if-let [fn-map
             (into base-functions (:dms-def/provides (rdf-fn/get-predicate-object-map settings-map)))]
      (while true
                (let [[ch meta payload] (async/<! (:datamos/listen-channel ch-map))
                      message (nippy/thaw payload)
                      msg-header (:datamos/logistic message)
                      subject (rdf-fn/value-from-nested-map
                                (rdf-fn/predicate-filter msg-header #{:dms-def/subject}))]
                  (log/debug "@response" message msg-header subject)
                  (log/trace "@response" (log/get-env))
                  ((fn-map subject #(println "@response - No function available for message subject:" subject)) ch meta message)))
      (do
        (log/warn "A map with functions is unavailable. Unable to fulfill the request")
        (log/trace "@response - No function map" (log/get-env))))))


(defstate responder
          :start (response local-channel (dm/base-component-state-reference))
          :stop (async/close! responder))