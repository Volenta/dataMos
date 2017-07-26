(ns datamos.system
  (:require [mount.core :as mnt :refer [stop start]]
            [clojure.tools.namespace :as ctn]
            [clojure.tools.namespace.repl :refer [refresh]]))


; --- Stop & Start system

(defn go
  []
  (start)
  :ready)

(defn stp
  []
  (stop)
  :clear)

(defn reset
  []
  (stop)
  (refresh :after 'sys/go))