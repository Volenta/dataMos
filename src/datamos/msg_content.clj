(ns datamos.msg-content
  (:require [datamos.util :as u]))

(defn retrieve-sender
  "Returns settings value for key :datamos-cfg/component-uri. If option is :type
  key will be set to :datamos-cfg/component-type"
  ([settings] (retrieve-sender settings nil))
  ([settings option]
   (first (u/select-submap-values settings (case option
                                             :type :datamos-cfg/component-type
                                             :datamos-cfg/component-uri)))))

(defn compose-message
  "Returns full message, with values for :datamos/logistic and :datamos/rdf-content.
  Settings is used to retrieve sender. Content is the RDF message to be sent. RCPT is the recepient of the message.
  RCPT is a datamos function defined as a keyword. Example :datamos-fn/registry"
  [settings content rcpt]
  (let [s (retrieve-sender settings)]
    {:datamos/logistic {:datamos/rcpt-fn rcpt
                        :datamos/sender  s}
     :datamos/config content}))
