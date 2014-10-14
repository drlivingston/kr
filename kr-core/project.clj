(defproject edu.ucdenver.ccp/kr-core "1.4.20-SNAPSHOT"
  :description "KR core API and tools."
  :parent-project {:path "../project.clj"
                   :inherit [:dependencies  :license]
                   :only-deps [org.clojure/clojure
                               org.clojure/java.classpath
                               com.stuartsierra/dependency]}
  :plugins [[lein-parent "0.2.1"]]
  :source-paths ["src/main/clojure" "src/test/clojure"]
  :test-paths ["src/test/clojure"])
