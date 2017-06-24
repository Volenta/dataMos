(ns datamos.run
  "Contains functions for starting and stopping datamos. Mostly to be used in REPL, for development purposes."
  (:require [datamos.core :as dc]))

(defn start
  []
  (dc/-main))

(defn stop
  []
  )
