(defproject org.volenta/datamos "0.1.6.1"
  :description "Messaging platform displaying the capabilities of RDF triples."
  :url "http://theinfotect.org/datamos"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3"
            :url "https://www.gnu.org/licenses/agpl-3.0.nl.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/core.async "0.3.443"]
                 [http-kit "2.2.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [com.novemberain/langohr "3.7.0"]
                 [mount "0.1.11"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [com.taoensso/timbre "4.10.0"]]
  :main ^:skip-aot datamos.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}}
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]])
