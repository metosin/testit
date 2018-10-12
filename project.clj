(defproject metosin/testit "0.3.0"
  :description "Midje style assertions for clojure.test"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [eftest "0.5.3" :scope "test"]
                 [clj-http "3.4.1" :scope "test"]
                 [org.slf4j/slf4j-nop "1.7.25" :scope "test"]
                 [net.cgrand/macrovich "0.2.1"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.339"]]}}

  :test-paths ["test" "examples"]

  :plugins [[lein-eftest "0.5.3"]
            [lein-doo "0.1.10"]]

  :cljsbuild
  {:builds [{:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/test.js"
                        :main "testit.runner"
                        :optimizations :none}}]}

  :doo {:build "test"}

  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)})
