(ns datamos.sign-up
  (:require [datamos
             [communication :as dcom]
             [messaging :as dm]
             [base :as base]
             [rdf-function :as rdf-fn]]
            [mount.core :as mnt :refer [defstate]]
            [datamos.rdf-content :as rdf-cnt]))

(defn de-register-uri
  [component-settings]
  (let [predicate :dms-def/provides
        lr ((:datamos-cfg/local-register
              (rdf-fn/get-predicate-object-map component-settings)))]
    (apply (fn [x y]
             [x (if (set? y)
                  (y :datamos/de-register)
                  y)])
      (rdf-fn/subject-object-by-predicate
          (rdf-fn/predicate-filter lr #{predicate}) predicate))))

(defn de-register-component
  "De-register this component from the main register."
  [conn-settings ex-settings component-settings]
  (let [subject (rdf-fn/get-subject component-settings)
        [receipient f] (de-register-uri component-settings)
        content (rdf-cnt/rdf-triple subject :dms-def/apply f)]
    (dcom/speak conn-settings ex-settings component-settings receipient :dms-def/component f content)))

(defstate signed-up
          :start (dcom/speak dcom/speak-connection dm/exchange base/component)
          :stop (de-register-component dcom/speak-connection dm/exchange base/component))
