(ns datamos.msg-functions
  (:require [datamos.rdf-function :as rdf-fn]
            [taoensso.timbre :as log]
            [datamos.messaging :as dm]))

(def message-store (atom {}))

(defn retrieve-sender
  "Returns component-settings value for key :datamos-cfg/module-uri. If option is :type
  key will be set to :datamos-cfg/module-type"
  ([component-settings] (retrieve-sender component-settings nil))
  ([component-settings option]
   (select-keys component-settings [(case option
                                      :type :dmsfn-def/module-type
                                      :datamos-cfg/module-uri)])))

(defn compose-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  [component-settings content rcpt]
  (let [s (retrieve-sender component-settings)]
    {:datamos/logistic {:dms-def/rcpt-fn rcpt
                        :dms-def/sender  (vals s)}
     :datamos/config   content}))

(defn get-message-id
  "Returns message-id from a message"
  [message]
  (:dms-def/message-id (:dms-def/message (:datamos/logistic message))))

(defn add-prefixes-to-message
  "Adds the map of prefixes to the message."
  [message prefixes]
  (assoc-in message [:datamos/rdf-content :datamos/prefix] prefixes))

(defn get-prefixes
  [content]
  (:dms-def/has-prefixes (rdf-fn/get-predicate-object-map content)))

(defn get-from-message-store
  "Get stored message from the message store"
  [connection-settings exchange-settings content]
  (let [m-id           (rdf-fn/get-subject content)
        prefixes       (get-prefixes content)
        stored-message (@message-store m-id)
        m              (add-prefixes-to-message stored-message prefixes)]
    (log/debug "@get-from-message-store" "\n content:" content "\n m-id:" m-id "\n m:"  m)
    (log/trace "@get-from-message-store" (log/get-env))
    (swap! message-store dissoc m-id)
    (dm/send-message connection-settings exchange-settings m)))

