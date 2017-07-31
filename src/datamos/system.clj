(ns datamos.system
  (:require [mount.core :as mnt :refer [stop start]]
            [clojure.tools.namespace :as ctn]
            [clojure.tools.namespace.repl :refer [refresh]]))


(defn stop-nr
  "Given a number n, it runs all the states up and included to the amount of states stated by n.
  Provide statelist (s) as a collection of states as provided by (:started mount.core/start"
  ([n] (if (not-empty (mnt/running-states))
         (stop-nr n (mnt/running-states))
         "No running states, provide state list seperately"))
  ([n s]
   (let [c (count (mnt/running-states))]
     (-> (mnt/only (take (- c n) s))
         mnt/stop))))

(defn run-nr
  "Given a number n, it runs all the states up and included to the amount of states stated by n.
  Provide statelist (s) as a collection of states as provided by (:started mount.core/start"
  ([n] (if (not-empty (mnt/running-states))
         (run-nr n (mnt/running-states))
         "No running states, provide state list seperately"))
  ([n s]
   (-> (mnt/only (take n s))
       mnt/start)))

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
  (refresh :after 'datamos.system/go))
