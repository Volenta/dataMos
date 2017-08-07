(ns datamos.rdf-content-test
  (:refer-clojure)
  (:require [clojure.test :refer :all]
            [datamos.rdf-content]
            [datamos.config]))

(def test-message
  #:datamos-fn{:registry+dms-fn+2547e6c2-4a61-49d6-97b4-1344de796315 {:datamos-cfg/component-type :datamos-fn/core,
                                                                      :datamos-cfg/component-fn :datamos-fn/registry,
                                                                      :rdf/type :dms-def/component}})

(deftest rdf-triple-test
  (is (= {:een {:twee :drie}} (datamos.rdf-content/rdf-triple :een :twee :drie)))
  (is (= #:rdf{:subject #:rdf{:predicate :rdf/object}} (datamos.rdf-content/rdf-triple :rdf/subject :rdf/predicate :rdf/object))))

(deftest sign-up-test
  (is (= #:datamos-fn{:registry+dms-fn+2547e6c2-4a61-49d6-97b4-1344de796315 {:datamos-cfg/component-type :datamos-fn/core,
                                                                            :datamos-cfg/component-fn :datamos-fn/registry,
                                                                            :rdf/type :dms-def/component,
                                                                            :dms-def/requires :dms-def/configuration,
                                                                            :rdfs/label "registry",
                                                                            :dms-def/function :datamos-fn/registry,
                                                                            :dms-def/provides #{:datamos/de-register
                                                                                                :datamos/registry
                                                                                                :datamos/registration}}}
        (datamos.rdf-content/sign-up
           #:datamos-fn{:registry+dms-fn+2547e6c2-4a61-49d6-97b4-1344de796315 {:datamos-cfg/component-type :datamos-fn/core,
                                                                               :datamos-cfg/component-fn   :datamos-fn/registry,
                                                                               :dms-def/provides           #:datamos{:registration datamos.config/registration
                                                                                                                     :registry     datamos.config/register
                                                                                                                     :de-register  datamos.config/de-register},
                                                                               :datamos-cfg/local-register datamos.config/local-register
                                                                               :rdf/type                   :dms-def/component}}))))

(deftest message-sender-test
  )

(deftest message-receipient-test
  )

(deftest compose-rdf-message-test
  )
