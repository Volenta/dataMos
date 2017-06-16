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
(s/def ::statement (s/tuple ::subject ::property ::object))

(s/def ::object ::object-types)
(s/def ::graph (s/coll-of ::statement :kind set? :min-count 1))
(s/def ::named-graphs (s/map-of keyword? ::graph :conform-keys true :min-count 1))

(s/def ::prefix (s/map-of keyword? string? :conform-keys true :min-count 1))
(s/def ::triples ::graph)
(s/def ::quads ::named-graphs)

(s/def ::rdf-content
  (s/keys :req-un [::prefix]
          :opt-un [::triples ::quads]))


