(ns datamos.core
  (:gen-class)
  (:refer-clojure)
  (:require [mount.core :as mnt :refer [stop start]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace :as ctn]
            [clojure.tools.namespace.repl :refer [refresh]]))

; TODO - check if state reference functions actually mean an inconsistency between states, functions and namespaces.
; TODO - make dataMos core available for prefix project

(defn stop-nr
  "Given a number n, it runs all the states up and included to the amount of states stated by n.
  Provide statelist (s) as a collection of states as provided by (:started mount.core/start"
  ([n] (if (not-empty (mnt/running-states))
         (stop-nr n (mnt/running-states))
         "No running states, provide state list seperately"))
  ([n s]
   (let [c (count (mnt/running-states))]
     (-> (mnt/only (take (- c n) (:stopped s)))
         mnt/stop))))

(defn run-nr
  "Given a number n, it runs all the states up and included to the amount of states stated by n.
  Provide statelist (s) as a collection of states as provided by (:started mount.core/start"
  ([n] (if (not-empty (mnt/running-states))
         (run-nr n (mnt/running-states))
         "No running states, provide state list seperately"))
  ([n s]
   (-> (mnt/only (take n (:started s)))
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
  (refresh :after 'datamos.core/go))

(defn -main
  "Initializes datamos.core. Configures the exchange"
  [& args]
  (reset))