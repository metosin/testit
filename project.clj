(defproject metosin/testit "0.0.1-SNAPSHOT"
  :description "Midje style assertions for clojure.test"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [clj-http "3.4.1" :scope "test"]]
  :test-paths ["test" "examples"])
