(defproject queues "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.4.1"]
                 [cheshire "5.8.1"]
                 [io.pedestal/pedestal.service "0.5.1"]
                 [io.pedestal/pedestal.route   "0.5.1"]
                 [io.pedestal/pedestal.jetty   "0.5.1"]
                 [org.clojure/data.json        "0.2.6"]
                 [org.slf4j/slf4j-simple       "1.7.21"]]
  :main ^:skip-aot queues.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.10.0-alpha4"]
                                      [midje "1.9.8"]]}})