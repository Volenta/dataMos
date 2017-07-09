(ns datamos.communication
  (:require [datamos.messaging :as dm]
            [langohr [basic :as lb]]
            [datamos.util :as u]))

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
               ["" message destination])))))

