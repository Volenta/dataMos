(ns datamos.rdf-content
  (:require [datamos
             [util :as u]
             [msg-content :as msg-cnt]]
            [datamos.spec.core :as dsc]))

; TODO - Sent request with identity, and components capability (function).
;      - use langohr function to send message to config queue
;      - read message from config queue
;      - interpret message from queue
;      - keep record of significant parts of message from queue
;      - create respond message
;      - respond to message from queue
;      - store response
; TODO - register component with name and function
; TODO - Build config for component
; TODO - Return config
; ... of type dms-def/component
; component requires datamos configuration
; component has function


(defn sign-up
  [component-settings]
  (let [values (mapv component-settings
                     [:datamos-cfg/component-uri
                      :datamos-cfg/component-fn])]
    {(first values) {:rdf/type         :dms-def/component
                     :dms-def/requires :dms-def/configuration
                     :rdfs/label (name (last values))
                     :dms-def/provides (last values)}}))

(defn compose-rdf-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  [component-settings content rcpt]
  (let [s (msg-cnt/retrieve-sender component-settings)]
    {:datamos/logistic    {:datamos/rcpt-fn rcpt
                           :datamos/sender s}
     :datamos/rdf-content {:datamos/prefix  {}
                           :datamos/triples content}}))