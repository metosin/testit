(defproject metosin/testit "0.4.0-SNAPSHOT"
  :description "Midje style assertions for clojure.test"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]]

  :test-paths ["test" "examples"]
  :profiles {:dev {:dependencies [[eftest "0.5.2"]
                                  [clj-http "3.9.0"]
                                  [org.slf4j/slf4j-nop "1.7.25"]]}}

  :plugins [[lein-eftest "0.5.2"]]
  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)})
