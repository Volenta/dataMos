(ns datamos.run
  "Contains functions for starting and stopping datamos. Mostly to be used in REPL, for development purposes."
  (:require [datamos.core :as dc]
            [datamos.messaging :as dm]))

(defn start
  []
  (dc/-main))

(defn stop
  []
  (reset! dc/component-settings
    (dm/stop-messaging-connection @dc/component-settings)))

(defn restart
  []
  (stop)
  (Thread/sleep 2000)
  (start))
