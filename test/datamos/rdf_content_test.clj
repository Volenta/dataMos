(ns datamos.rdf-content-test
  (:refer-clojure)
  (:require [clojure.test :refer :all]
            [datamos.rdf-content :refer :all]
            [datamos.util :as u]))

(def msg-id generate-message-id)

(deftest rdf-triple-test
  (is (= {:een {:twee :drie}} (datamos.rdf-content/rdf-triple :een :twee :drie)))
  (is (= #:rdf{:subject #:rdf{:predicate :rdf/object}} (datamos.rdf-content/rdf-triple :rdf/subject :rdf/predicate :rdf/object))))

(deftest sign-up-test
  )

(deftest message-sender-test
  )

(deftest message-receipient-test
  )

(deftest compose-rdf-message-test
  (is (= (compose-rdf-message #:datamos-fn{:function+dms-fn+98866fb4-4124-4f9a-b7f9-39920be68929 {:dmsfn-def/module-type  :dmsfn-def/enrichment,
                                                                                                  :dmsfn-def/module-name  :dmsfn-def/module,
                                                                                                  :dms-def/provides       nil,
                                                                                                  :datamos/local-register nil,
                                                                                                  :rdf/type               :dmsfn-def/module-id}}
                              :dms-def/tst-msg
                              (sign-up #:datamos-fn{:function+dms-fn+98866fb4-4124-4f9a-b7f9-39920be68929 {:dmsfn-def/module-type  :dmsfn-def/enrichment,
                                                                                                           :dmsfn-def/module-name  :dmsfn-def/module,
                                                                                                           :dms-def/provides       nil,
                                                                                                           :datamos/local-register nil,
                                                                                                           :rdf/type               :dmsfn-def/module-id}})
                              :dms-def/message-tester
                              nil
                              msg-id)
         #:datamos{:logistic    {{}                                                               {:dmsfn-def/module-name :dms-def/message-tester, :dms-def/transmit :dms-def/recipient},
                                 :datamos-fn/function+dms-fn+98866fb4-4124-4f9a-b7f9-39920be68929 {:dmsfn-def/module-type :dmsfn-def/enrichment,
                                                                                                   :dmsfn-def/module-name :dmsfn-def/module,
                                                                                                   :rdf/type              :dmsfn-def/module-id,
                                                                                                   :dms-def/transmit      :dms-def/sender},
                                 :dms-def/message                                                 #:dms-def{:subject    :dms-def/tst-msg,
                                                                                                            :message-id msg-id}},
                   :rdf-content #:datamos{:prefix  {},
                                          :triples #:datamos-fn{:function+dms-fn+98866fb4-4124-4f9a-b7f9-39920be68929 {:dmsfn-def/module-type :dmsfn-def/enrichment,
                                                                                                                       :dmsfn-def/module-name :dmsfn-def/module,
                                                                                                                       :rdf/type              :dmsfn-def/module-id,
                                                                                                                       :dms-def/requires      :datamos-fn/registry,
                                                                                                                       :rdfs/label            "function",
                                                                                                                       :dms-def/function      :dmsfn-def/module,
                                                                                                                       :dms-def/provides      #{}}}}})))
