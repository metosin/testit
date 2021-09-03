(defproject metosin/testit "0.4.1-SNAPSHOT"
  :description "Midje style assertions for clojure.test"
  :url "https://github.com/metosin/testit"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [eftest "0.5.9" :scope "test"]
                 [clj-http "3.4.1" :scope "test"]
                 [org.slf4j/slf4j-nop "1.7.25" :scope "test"]
                 [net.cgrand/macrovich "0.2.1"]]

  :deploy-repositories [["releases" :clojars]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.879"]]}}

  :test-paths ["test" "examples"]

  :plugins [[lein-cljfmt "0.8.0"]
            [lein-eftest "0.5.9"]
            [lein-doo "0.1.11"]]

  :cljsbuild
  {:builds [{:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/test.js"
                        :main "testit.runner"
                        :optimizations :none}}]}

  :doo {:build "test"
        :alias {:default [:firefox]}
        :paths {:karma "./node_modules/karma/bin/karma"}}

  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)})
