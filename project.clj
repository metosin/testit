(defproject metosin/testit "0.2.2"
  :description "Midje style assertions for clojure.test"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [eftest "0.3.1" :scope "test"]
                 [clj-http "3.4.1" :scope "test"]
                 [org.slf4j/slf4j-nop "1.7.25" :scope "test"]]

  :test-paths ["test" "examples"]

  :plugins [[lein-eftest "0.3.1"]]

  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)})
