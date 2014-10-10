(defproject edu.ucdenver.ccp/kr-sesame-core "1.4.20-SNAPSHOT"
  :description "KR Sesame bindings."
  :parent-project {:path "../../project.clj"
                   :inherit [:dependencies :license]
                   :only-deps [org.clojure/clojure
                               com.stuartsierra/dependency
                               org.openrdf.sesame/sesame-runtime
                               org.openrdf.sesame/sesame-queryresultio
                               org.openrdf.sesame/sesame-queryresultio-sparqlxml
                               org.openrdf.sesame/sesame-queryresultio-binary
                               commons-logging
                               commons-codec
                               org.slf4j/slf4j-log4j12]}
  :plugins [[lein-parent-mg "0.2.2"]]
  :profiles {:test {:dependencies [[edu.ucdenver.ccp/kr-core "1.4.20-SNAPSHOT"]]}}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"])
