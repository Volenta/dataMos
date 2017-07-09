(ns datamos.spec.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]))

(s/def ::value string?)
(s/def ::lang string?)
(s/def ::type keyword?)

(s/def ::literal-type
  (s/keys :req-un [::value]
          :opt-un [::type ::lang]))

(s/def ::node-blank (s/map-of ::property ::object :conform-keys true))
(s/def ::coll (s/coll-of ::object :kind vector? :min-count 1))

(s/def ::subject-types (s/or :uri keyword?
                             :blank-node ::node-blank
                             :collection ::coll))

(s/def ::object-types (s/or :uri keyword?
                            :number number?
                            :literal string?
                            :typed-literal ::literal-type
                            :boolean-literal boolean?
                            :blank-node ::node-blank
                            :collection ::coll))

(s/def ::subject ::subject-types)
(s/def ::property keyword?)                                 ;; Using property instead of predicate. Because Clojure already uses predicate in functions like s/explain
(s/def ::object ::object-types)

(s/def ::property-object (s/map-of ::property ::object :conform-keys true :min-count 1))
(s/def ::statement (s/map-of ::subject ::property-object :conform-keys true :min-count 1))
(s/def ::named-graphs (s/map-of keyword? ::statement :conform-keys true :min-count 1))

(s/def ::prefix (s/map-of keyword? string? :conform-keys true :min-count 1))

(s/def ::rdf-content
  (s/or :prefixes (s/map-of :datamos/prefix ::prefix :conform-keys true :min-count 0 :max-count 1)
        :triples (s/map-of :datamos/triples ::statement :conform-keys true :min-count 0 :max-count 1)
        :quads (s/map-of :datamos/quads ::named-graphs :conform-keys true :min-count 0 :max-count 1)))

(s/def ::logis-props
  (s/keys :opt [:datamos-cfg/rcpt-fn]))

(s/def ::logistics (s/map-of :datamos-cfg/logistic ::logis-props :conform-keys true :min-count 0 :max-count 1))

(s/def ::message
  (s/or :rdf ::rdf-content
        :logis ::logistics))
