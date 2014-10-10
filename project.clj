(defproject edu.ucdenver.ccp/kr "1.4.20-SNAPSHOT"
  :description "knowledge representation and reasoning tools"
  :url "https://github.com/drlivingston/kr"
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"
            :distribution "open"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.classpath "0.2.2"]
                 [org.openrdf.sesame/sesame-runtime "2.7.13"]
                 [org.openrdf.sesame/sesame-queryresultio "2.7.13" :extension "pom"]
                 [org.openrdf.sesame/sesame-queryresultio-sparqlxml "2.7.13"]
                 [org.openrdf.sesame/sesame-queryresultio-binary "2.7.13"]
                 [org.apache.jena/jena-arq "2.12.1"]
                 [log4j "1.2.17"]
                 [commons-logging "1.1.1"]
                 [commons-codec "1.6"]
                 [com.stuartsierra/dependency "0.1.1"]
                 [org.slf4j/slf4j-log4j12 "1.6.5"]]
  :pom-plugins [[com.theoryinpractcise/clojure-maven-plugin "1.3.9"]]
  :profiles {:test {:dependencies [[junit "3.8.1"]]}})
