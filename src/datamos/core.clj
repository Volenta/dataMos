(ns datamos.core
  (:refer-clojure)
  (:require [mount.core :as mnt :refer [stop start]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace :as ctn]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

; TODO - check if functions like dcom/sign-up-state-reference derive from an inconsistency between states, functions and namespaces.

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
  (do
    (log/merge-config!
      {:appenders
       {:println {:min-level :info}
        :spit (merge (appenders/spit-appender {:fname "log/datamos.log"})
                     {:min-level :trace})}}))
    (log/info "@go - Starting dataMos")
    (start)
    (log/info "@go - dataMos Running"))

(defn stp
  []
  (do
    (stop)
    (log/info "@stop - dataMos has stopped")))

(defn reset
  []
  (do
    (stp)
    (refresh :after 'datamos.core/go)))

(set-refresh-dirs "src")