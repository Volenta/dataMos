(ns datamos.rdf-function-test
  (:require [clojure.test :refer :all]
            [datamos.rdf-function]))


(deftest get-predicate-object-map-test
  )

(deftest get-subject-test
  )


(deftest value-from-nested-map-test
  )

(deftest predicate-filter-test
  )

(deftest message-content-test
  )

(deftest subject-object-by-predicate
  (is (= [:datamos-fn/registry+dms-fn+9ad269a0-de8b-4393-a6a3-edacb7fafc83
          #{:datamos/de-register :datamos/registry :datamos/registration}]
         (datamos.rdf-function/subject-object-by-predicate
           #:datamos-fn{:registry+dms-fn+9ad269a0-de8b-4393-a6a3-edacb7fafc83 {:rdf/type :dms-def/component,
                                                                               :rdfs/label "registry",
                                                                               :dms-def/function :datamos-fn/registry,
                                                                               :dms-def/provides #{:datamos/de-register
                                                                                                   :datamos/registry
                                                                                                   :datamos/registration}}}

           :dms-def/provides))))

(deftest values-by-predicate-test
  )