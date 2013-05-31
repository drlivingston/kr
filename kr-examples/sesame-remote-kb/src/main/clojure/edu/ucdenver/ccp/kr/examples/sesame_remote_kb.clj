(ns edu.ucdenver.ccp.kr.examples.sesame-remote-kb
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql)
  (require edu.ucdenver.ccp.kr.sesame.kb)
  (import org.openrdf.repository.http.HTTPRepository))

;;; --------------------------------------------------------
;;; create kb
;;; --------------------------------------------------------

(defn sesame-remote-test-kb []
  (open
   (edu.ucdenver.ccp.kr.sesame.kb/new-sesame-server
    :server "http://dbpedia.org/sparql"
    :repo "")))


(defn add-namespaces [kb]
  (update-namespaces kb
                     '(("ex" "http://www.example.org/") 
                       ("rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                       ("rdfs" "http://www.w3.org/2000/01/rdf-schema#")
                       ("owl" "http://www.w3.org/2002/07/owl#")
                       ("foaf" "http://xmlns.com/foaf/0.1/")
                       
                       ("dbpedia-owl" "http://dbpedia.org/ontology/")
                       ("dbpedia" "http://dbpedia.org/resource/"))))



;;; --------------------------------------------------------
;;; sparql api
;;; --------------------------------------------------------

(def ski-pattern '((?/ski rdf/type dbpedia-owl/SkiArea)))

(defn count-ski [kb]
  (query-count kb ski-pattern))

(defn ski-areas [kb]
  (query kb '((?/ski rdf/type dbpedia-owl/SkiArea))))

(defn visit-ski-areas [kb]
  (query-visit kb
               (fn [bindings]
                 (let [area (get bindings '?/ski)]
                   (println "visiting " (name area))))
               ski-pattern))

;;; --------------------------------------------------------
;;; REPL trace:
;;; --------------------------------------------------------

;; user> (use 'edu.ucdenver.ccp.kr.examples.sesame-remote-kb)
;; nil

;; user> (def a-kb (add-namespaces (sesame-remote-test-kb)))
;; #'user/a-kb

;; user> (count-ski a-kb)
;; 495

;; user> (take 10 (ski-areas a-kb))
;; ({?/ski dbpedia/Grouse_Mountain} {?/ski dbpedia/Killington_Ski_Resort} {?/ski dbpedia/Whakapapa_skifield} {?/ski dbpedia/Treble_Cone} {?/ski dbpedia/Invincible_Snowfields} {?/ski dbpedia/Thredbo,_New_South_Wales} {?/ski dbpedia/Tyrol_Basin} {?/ski dbpedia/Squaw_Valley_Ski_Resort} {?/ski dbpedia/Brighton_Ski_Resort} {?/ski dbpedia/Keystone_Resort})


;; user> (visit-ski-areas a-kb)
;; visiting  Breuil-Cervinia
;; visiting  Snowbasin
;; visiting  Charlotte_Pass,_New_South_Wales
;; visiting  Ski_Dubai
;; visiting  Mont-Sainte-Anne
;; visiting  Sunshine_Village
;; visiting  Madonna_di_Campiglio
;; visiting  Reiteralm


;; user> (use 'edu.ucdenver.ccp.kr.kb)
;; nil
;; user> (close a-kb)
;; #edu.ucdenver.ccp.kr.sesame.kb.SesameKB{:server #<HTTPRepository org.openrdf.repository.http.HTTPRepository@f4c7f77>, :connection nil, :kb-features (:sparql-1-0 :sparql-1-1), :ns-map-to-long {"dbpedia" "http://dbpedia.org/resource/", "dbpedia-owl" "http://dbpedia.org/ontology/", "foaf" "http://xmlns.com/foaf/0.1/", "owl" "http://www.w3.org/2002/07/owl#", "rdfs" "http://www.w3.org/2000/01/rdf-schema#", "rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "ex" "http://www.example.org/"}, :ns-map-to-short {"http://dbpedia.org/resource/" "dbpedia", "http://dbpedia.org/ontology/" "dbpedia-owl", "http://xmlns.com/foaf/0.1/" "foaf", "http://www.w3.org/2002/07/owl#" "owl", "http://www.w3.org/2000/01/rdf-schema#" "rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" "rdf", "http://www.example.org/" "ex"}, :value-factory #<ValueFactoryImpl org.openrdf.model.impl.ValueFactoryImpl@67446579>}


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
