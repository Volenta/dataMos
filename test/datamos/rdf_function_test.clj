(ns datamos.rdf-function-test
  (:require [clojure.test :refer :all]
            [datamos.rdf-function :refer :all]))


(deftest generate-qualified-uri-test
  (is (keyword? (generate-qualified-uri :test/uri))))

(deftest get-predicate-object-map-test
  (is (= {:twee :drie}
        (get-predicate-object-map {:een {:twee :drie}})))
  (is (= #:rdf{:type :dmsfn-def/module-id}
         (get-predicate-object-map #:datamos-fn{:dmsfn-def/registry #:rdf{:type :dmsfn-def/module-id}}))))

(deftest get-subject-test
  (is (= :een
         (get-subject {:een {:twee :drie}})))
  (is (= :dmsfn-def/registry (get-subject #:datamos-fn{:dmsfn-def/registry #:rdf{:type :dmsfn-def/module-id}}))))


(deftest value-from-nested-map-test
  )

(deftest predicate-filter-test
  )

(deftest message-content-test
  )

(deftest subject-object-by-predicate-test
  (is (= [:datamos-fn/registry+dms-fn+9ad269a0-de8b-4393-a6a3-edacb7fafc83
          #{:datamos-fn/de-register :datamos-fn/registry :datamos-fn/registration}]
         (datamos.rdf-function/subject-object-by-predicate
           #:datamos-fn{:registry+dms-fn+9ad269a0-de8b-4393-a6a3-edacb7fafc83 {:rdf/type         :dms-def/component,
                                                                               :rdfs/label       "registry",
                                                                               :dms-def/function :dmsfn-def/registry,
                                                                               :dms-def/provides #{:datamos-fn/de-register
                                                                                                   :datamos-fn/registry
                                                                                                   :datamos-fn/registration}}}

           :dms-def/provides))))

(deftest values-by-predicate-test
  )