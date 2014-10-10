(defproject edu.ucdenver.ccp/kr-jena-core "1.4.20-SNAPSHOT"
  :description "KR Jena bindings."
  :parent-project {:path "../../project.clj"
                   :inherit [:dependencies :license]
                   :only-deps [org.clojure/clojure
                               org.apache.jena/jena-arq
                               com.stuartsierra/dependency]}
  :plugins [[lein-parent-mg "0.2.2"]]
  :profiles {:dev {:dependencies [[edu.ucdenver.ccp/kr-core "1.4.20-SNAPSHOT"]]}}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"])
