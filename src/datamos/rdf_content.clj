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
                       (rdf-fn/predicate-filter component-settings #{:datamos-cfg/module-fn}))
        pred-filter #{:rdf/type :datamos-cfg/module-type :datamos-cfg/module-fn}
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
      {:dms-def/requires :dms-def/configuration
       :rdfs/label       (name component-fn)
       :dms-def/function component-fn
       :dms-def/provides response-fn-set})))

(defn message-sender
  [component-settings]
  (update-in
    (datamos.rdf-function/predicate-filter component-settings
                                           #{:datamos-cfg/module-type
                                             :datamos-cfg/module-fn
                                             :rdf/type})
    (keys component-settings)
    conj
    {:dms-def/transmit :dms-def/sender}))

(defn message-receipient
  ([rcpt]
   (if (= rcpt "config.datamos-fn")
     {{} {:datamos-cfg/rcpt-queue rcpt
          :dms-def/transmit :dms-def/recipient}}
     {{} {:datamos-cfg/module-fn rcpt
          :dms-def/transmit :dms-def/recipient}}))
  ([component-settings rcpt rcpt-type]
   (let [rcpt-id (if (= :dms-def/module rcpt-type)
                   (if (= (rdf-fn/get-subject component-settings) rcpt)
                     :datamos/idem-sender
                     rcpt)
                   {})]
     {rcpt-id {rcpt-type         rcpt
               :dms-def/transmit :dms-def/recipient}}))
  ([component-settings rcpt rcpt-type rcpt-component]
   {rcpt-component {rcpt-type rcpt
                    :dms-def/transmit :dms-def/recipient}}))

(defn compose-rdf-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  ([component-settings subject content rcpt] (compose-rdf-message component-settings subject content rcpt nil))
  ([component-settings subject content rcpt rcpt-type]
   (let [r (if rcpt-type
             (message-receipient component-settings rcpt rcpt-type)
             (message-receipient rcpt))
         s (message-sender component-settings)
         m-id (keyword (str "datamos/msg-id+" (u/return-uuid)))]
     (log/trace "@compose-rdf-message" (log/get-env))
     {:datamos/logistic    (conj r s
                                 {:dms-def/message {:dms-def/subject subject
                                                    :dms-def/message-id m-id}})
      :datamos/rdf-content {:datamos/prefix  {}
                            :datamos/triples content}})))