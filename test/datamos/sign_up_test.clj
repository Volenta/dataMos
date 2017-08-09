(ns datamos.sign-up-test
  (:require [clojure.test :refer :all]
            [datamos.sign-up :refer :all]))

(deftest de-register-uri-test
  (is (= nil (de-register-uri
           #:datamos-fn{:function+dms-fn+38eb4067-8980-44d1-8d23-3cf3bd14c10b {:datamos-cfg/module-type    :datamos-fn/enrichment,
                                                                               :datamos-cfg/module-fn      :datamos-fn/function,
                                                                               :dms-def/provides           nil,
                                                                               :datamos-cfg/local-register nil,
                                                                               :rdf/type                   :dms-def/module}}))))
