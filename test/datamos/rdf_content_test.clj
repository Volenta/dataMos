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
  )
