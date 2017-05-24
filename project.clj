(defproject datamos "0.1.0-SNAPSHOT"
  :description "Messaging platform displaying the capabilities of RDF triples."
  :url "http://theinfotect.org/datamos"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3"
            :url "https://www.gnu.org/licenses/agpl-3.0.nl.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.2.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [com.novemberain/langohr "4.0.0"]]
  :main ^:skip-aot datamos.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
