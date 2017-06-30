(ns datamos.run
  "Contains functions for starting and stopping datamos. Mostly to be used in REPL, for development purposes."
  (:require [datamos.core :as dc]
            [datamos.messaging :as dm]))

(defn start
  []
  (dc/-main))

(defn stop
  []
  (dm/stop-exchange @dc/component-settings)
  (dm/stop-queue @dc/component-settings)
  (dm/stop-connection @dc/component-settings)
  (reset! dc/component-settings (atom {})))

(defn restart
  []
  (stop)
  (Thread/sleep 2000)
  (start))
