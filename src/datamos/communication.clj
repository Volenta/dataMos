(ns datamos.communication
  (:require [datamos
             [messaging :as dm]
             [util :as u]]
            [langohr [basic :as lb]]))

(defn retrieve-sender
  [settings]
  (first (u/select-submap-values settings :datamos-cfg/component-uri)))

(defn compose-message
  [content sender]
  {:datamos/logistic {:datamos/rcpt-fn :datamos-fn/config
                      :datamos/sender sender}
   :datamos/rdf-content {:datamos/prefix {}
                         :datamos/triples content}})

(defn send-message
  [settings message]
  (let [destination (get-in message [:datamos/logistic :datamos/rcpt-fn])]
    (apply lb/publish
           (dm/vector-connection->channel
             (conj
               (u/select-submap-values
                 settings
                 :datamos-cfg/low-level-connection
                 :datamos-cfg/exchange-name)
               ["" message {:headers (conj {}
                                           (mapv u/keyword->string
                                                 [:datamos-cfg/component-fn destination]))}])))))

(defn communicate
  [settings content]
  (send-message
    settings (compose-message content (retrieve-sender settings))))