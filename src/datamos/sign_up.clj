(ns datamos.sign-up
  (:require [datamos
             [communication :as dcom]
             [rdf-function :as rdf-fn]
             [rdf-content :as rdf-cnt]]
            [mount.core :as mnt :refer [defstate]]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]))

(defn initialize-registration
  [conn-settings ex-settings module-settings]
  (async/go
    (Thread/sleep 1000)
    (dcom/speak conn-settings ex-settings module-settings)))

(defn de-register-uri
  [component-settings]
  (let [predicate :dms-def/provides
        lr (:datamos/local-register
             (rdf-fn/get-predicate-object-map component-settings))]
    (if (seq lr)
      (apply (fn [x y]
               [x (if (set? y)
                    (y :datamos-fn/de-register)
                    y)])
             (rdf-fn/subject-object-by-predicate
               (rdf-fn/predicate-filter lr #{predicate}) predicate))
      nil)))

(defn de-register-component
  "De-register this component from the main register."
  [conn-settings ex-settings component-settings]
  (let [subject (rdf-fn/get-subject component-settings)
        response (de-register-uri component-settings)
        [recipient f] (if response
                        response
                        [:dmsfn-def/registry :datamos-fn/de-register])
        content (rdf-cnt/rdf-triple subject :dms-def/apply f)]
    (log/trace "@de-register-component" (log/get-env))
    (dcom/speak conn-settings ex-settings component-settings recipient :dmsfn-def/module-id f content)))

(defstate signing-up
          :start (apply initialize-registration (dcom/sign-up-state-reference))
          :stop (apply de-register-component (dcom/sign-up-state-reference)))