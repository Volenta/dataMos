(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [util :as u]
             [rdf-content :as rdf]]
            [langohr
             [consumers :as lc]]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-p]
            [taoensso.nippy :as nippy]))

(def dev-tst (atom true))

(defn stop-thread
  []
  (reset! dev-tst false))

(defn run-thread
  []
  (reset! dev-tst true))

(def default-consumer-settings
  "Default settings for the component consuming messages from the queue"
  {:auto-ack true :exclusive false})

(defn generate-qualified-uri
  "Return unique uri, based on type-kw."
  [type-kw]
  (keyword (str (namespace type-kw) "/" (name type-kw) "+dms-fn+" (java.util.UUID/randomUUID))))

(defn set-component
  "Returns component settings. With component-type, component-fn and component-uri as a submap of :datamos-cfg/component."
  [type-kw fn-kw]
  {:datamos-cfg/component {:datamos-cfg/component-type type-kw
                           :datamos-cfg/component-fn fn-kw
                           :datamos-cfg/component-uri (generate-qualified-uri fn-kw)}})

(defn open-local-channel
  [settings]
  (let [ch (async/chan)]
    {:datamos-cfg/listener {:datamos-cfg/listen-channel ch}}))

(defn speak
  [settings content rcpt]
  (as-> settings v
      (rdf/compose-rdf-message v content rcpt)
      (dm/send-message settings v)))

(defn speak-sign-up
  "Send message to sign-up functionality and retrieve configuration. Function will deliver messages to the config.datamos-fn
  queue. Use for initialization of a component."
  [settings]
  (dm/request-config settings
    (rdf/compose-rdf-message settings (rdf/sign-up settings) "config.datamos-fn")))

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
    (async/go (while (and true @dev-tst)
                (let [[ch meta payload] (async/<! chan)]
                  (function ch meta (nippy/thaw payload)))))))

(defn close-local-channel
  [settings]
  (let [chan (get-local-channel settings)]
    (async/close! chan)))