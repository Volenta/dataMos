(ns datamos.rdf-content
  (:require [datamos
             [rdf-function :as rdf-fn]]
            [datamos.spec.core :as dsc]
            [taoensso.timbre :as log]
            [datamos.util :as u]))

(defn rdf-triple
  "Provides a simple triple. Can be used as an dummy value"
  [subject predicate object]
  {subject {predicate object}})

(defn sign-up
  [component-settings]
  (let [component-fn (rdf-fn/value-from-nested-map
                       (rdf-fn/predicate-filter component-settings #{:dmsfn-def/module-name}))
        pred-filter #{:rdf/type :dmsfn-def/module-type :dmsfn-def/module-name}
        sign-up-msg (rdf-fn/predicate-filter component-settings pred-filter)
        response-fn-set (set
                          (keys
                            (rdf-fn/get-predicate-object-map
                              (rdf-fn/get-predicate-object-map
                                (rdf-fn/predicate-filter component-settings #{:dms-def/provides})))))]
    (log/trace "@sign-up" (log/get-env))
    (update-in
      sign-up-msg
      (keys component-settings)
      conj
      {:dms-def/requires :datamos-fn/registry
       :rdfs/label       (name component-fn)
       :dms-def/function component-fn
       :dms-def/provides response-fn-set})))

(defn message-sender
  [component-settings]
  (update-in
    (datamos.rdf-function/predicate-filter component-settings
                                           #{:dmsfn-def/module-type
                                             :dmsfn-def/module-name
                                             :rdf/type})
    (keys component-settings)
    conj
    {:dms-def/transmit :dms-def/sender}))

(defn message-receipient
  ([rcpt]
   (if (= rcpt "config.datamos-fn")
     {{} {:dmscfg-def/rcpt-queue rcpt
          :dms-def/transmit      :dms-def/recipient}}
     {{} {:dmsfn-def/module-name rcpt
          :dms-def/transmit      :dms-def/recipient}}))
  ([component-settings rcpt rcpt-type]
   (let [rcpt-id (if (= :dmsfn-def/module rcpt-type)
                   (if (= (rdf-fn/get-subject component-settings) rcpt)
                     :dms-def/idem-sender
                     rcpt)
                   {})]
     {rcpt-id {rcpt-type         rcpt
               :dms-def/transmit :dms-def/recipient}}))
  ([component-settings rcpt rcpt-type rcpt-component]
   {rcpt-component {rcpt-type rcpt
                    :dms-def/transmit :dms-def/recipient}}))

(defn generate-message-id
  "Returns keyword with dataMos unique Message ID, using UUID"
  []
  (keyword (str "datamos-id/msg-id+" (u/return-uuid))))

(defn compose-rdf-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  ([component-settings subject content rcpt] (compose-rdf-message component-settings subject content rcpt nil))
  ([component-settings subject content rcpt rcpt-type] (compose-rdf-message component-settings subject content rcpt rcpt-type
                                                                            (generate-message-id)))
  ([component-settings subject content rcpt rcpt-type m-id]
   (let [r (if rcpt-type
             (message-receipient component-settings rcpt rcpt-type)
             (message-receipient rcpt))
         s (message-sender component-settings)]
     (log/trace "@compose-rdf-message" (log/get-env))
     {:datamos/logistic    (conj r s
                                 {:dms-def/message {:dms-def/subject subject
                                                    :dms-def/message-id m-id}})
      :datamos/rdf-content {:datamos/prefix  {}
                            :datamos/triples content}})))