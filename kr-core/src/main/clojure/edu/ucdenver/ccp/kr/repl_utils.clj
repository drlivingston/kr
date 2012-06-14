(ns edu.ucdenver.ccp.kr.repl-utils
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.unify))

;;; --------------------------------------------------------
;;; show-sym
;;; --------------------------------------------------------

(def ^:dynamic *show-sym-limits* [10 10 10])

(defn show-take-lim [l limit]
  (let [lim (take limit l)]
    (dorun (map prn lim))
    (println "Showing: " (min (count lim) limit) " of " (count l))))

;; this could be done with the rdfKB API instead of the sparqlKB API
(defn show-sym [s]
  (let [[s-lim p-lim o-lim] *show-sym-limits*]
    (println "subject")
    (show-take-lim (query-template '(?p ?o) `((~s ?p ?o))) s-lim)
    (println "\npredicate")
    (show-take-lim (query-template '(?s ?o) `((?s ~s ?o))) p-lim)
    (println "\nobject")
    (show-take-lim (query-template '(?s ?p) `((?s ?p ~s))) o-lim)))


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


