(ns datamos.system
  (:require [datamos
             [core :as dc]
             [core-vars :as dcv]
             [messaging :as dm]
             [communication :as dcom]]))

(defn clear-repl
  []
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))))

(defn remove-symbol
  [symbol]
  (ns-unmap *ns* symbol))

; --- Stop & Start system

(defn start
  []
  (dc/-main))

(defn stop
  []
  (let [initial-settings @dcv/local-settings]
    (reset! dcv/local-settings
            (dm/stop-messaging-connection initial-settings))
    (dcom/close-local-channel initial-settings)))

(defn restart
  []
  (stop)
  (Thread/sleep 500)
  (start))