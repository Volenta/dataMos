(ns datamos.sign-up
  (:require [datamos
             [communication :as dcom]
             [rdf-function :as rdf-fn]
             [rdf-content :as rdf-cnt]]
            [mount.core :as mnt :refer [defstate]]))

(defn de-register-uri
  [component-settings]
  (let [predicate :dms-def/provides
        lr-fn (:datamos-cfg/local-register
                (rdf-fn/get-predicate-object-map component-settings))
        lr (if lr-fn
             lr-fn
             {})]
    (if (empty? lr)
      nil
      (apply (fn [x y]
               [x (if (set? y)
                    (y :datamos/de-register)
                    y)])
             (rdf-fn/subject-object-by-predicate
               (rdf-fn/predicate-filter lr #{predicate}) predicate)))))

(defn de-register-component
  "De-register this component from the main register."
  [conn-settings ex-settings component-settings]
  (let [subject (rdf-fn/get-subject component-settings)
        response (de-register-uri component-settings)
        [recipient f] (if response
                        response
                        [:datamos-fn/registry :datamos/de-register])
        content (rdf-cnt/rdf-triple subject :dms-def/apply f)]
    (dcom/speak conn-settings ex-settings component-settings recipient :dms-def/component f content)))

(defstate signing-up
          :start (apply dcom/speak (dcom/sign-up-state-reference))
          :stop (apply de-register-component (dcom/sign-up-state-reference)))