(ns datamos.rdf-function-test
  (:require [clojure.test :refer :all]
            [datamos.rdf-function :refer :all]))


(deftest generate-qualified-uri-test
  (is (keyword? (generate-qualified-uri :test/uri))))

(deftest get-predicate-object-map-test
  (is (= {:twee :drie}
        (get-predicate-object-map {:een {:twee :drie}})))
  (is (= #:rdf{:type :dms-def/module}
         (get-predicate-object-map #:datamos-fn{:registry #:rdf{:type :dms-def/module}}))))

(deftest get-subject-test
  (is (= :een
         (get-subject {:een {:twee :drie}})))
  (is (= :datamos-fn/registry (get-subject #:datamos-fn{:registry #:rdf{:type :dms-def/module}}))))


(deftest value-from-nested-map-test
  )

(deftest predicate-filter-test
  )

(deftest message-content-test
  )

(deftest subject-object-by-predicate-test
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
  (is (= '(:datamos-fn/registry) (values-by-predicate :dms-def/function #:datamos-fn{:registry+dms-fn+b62056f7-fb5f-498a-a9a7-03c5a674c197 {:rdf/type :dms-def/module,
                                                                                                                                           :rdfs/label "registry",
                                                                                                                                           :dms-def/function :datamos-fn/registry,
                                                                                                                                           :dms-def/provides #{:datamos/de-register
                                                                                                                                                               :datamos/registry
                                                                                                                                                               :datamos/registration}}}))))