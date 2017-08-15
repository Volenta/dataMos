(ns datamos.sign-up-test
  (:require [clojure.test :refer :all]
            [datamos.sign-up :refer :all]))

(deftest de-register-uri-test
  (is (= nil (de-register-uri
           #:datamos-fn{:function+dms-fn+38eb4067-8980-44d1-8d23-3cf3bd14c10b {:dmsfn-def/module-type  :dmsfn-def/enrichment,
                                                                               :dmsfn-def/module-name  :dmsfn-def/module,
                                                                               :dms-def/provides       nil,
                                                                               :datamos/local-register nil,
                                                                               :rdf/type               :dmsfn-def/module-id}}))))
