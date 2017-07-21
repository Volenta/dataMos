(ns datamos.rdf-content
  (:require [datamos
             [util :as u]]
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
  [settings]
  (let [values (u/select-submap-values settings
                                       :datamos-cfg/component-uri
                                       :datamos-cfg/component-fn)]
    {(first values) {:rdf/type         :dms-def/component
                     :dms-def/requires :dms-def/configuration
                     :rdfs/label (name (last values))
                     :dms-def/provides (last values)}}))

(defn retrieve-sender
  "Returns settings value for key :datamos-cfg/component-uri. If option is :type
  key will be set to :datamos-cfg/component-type"
  ([settings] (retrieve-sender settings nil))
  ([settings option]
   (first (u/select-submap-values settings (case option
                                             :type :datamos-cfg/component-type
                                             :datamos-cfg/component-uri)))))

(defn compose-rdf-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  [settings content rcpt]
  (let [s (retrieve-sender settings)]
    {:datamos/logistic    {:datamos/rcpt-fn rcpt
                           :datamos/sender s}
     :datamos/rdf-content {:datamos/prefix  {}
                           :datamos/triples content}}))