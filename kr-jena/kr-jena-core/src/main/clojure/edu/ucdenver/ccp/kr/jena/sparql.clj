(ns edu.ucdenver.ccp.kr.jena.sparql
  (use edu.ucdenver.ccp.kr.variable
       ;[edu.ucdenver.ccp.kr.rdf :exclude (resource)]
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       edu.ucdenver.ccp.kr.jena.rdf)

  (import 
   ;interfaces
   (com.hp.hpl.jena.query QueryFactory
                          QueryExecutionFactory
                          QueryExecution
                          Query
                          QuerySolution)
                          
))


;;; --------------------------------------------------------
;;; result processing helpsers
;;; --------------------------------------------------------

;; (defn count-results [results]
;;   (count (jena-iteration-seq results)))

;; (defn result-map [results] 
;;   (lazy-seq
;;    ;(when-let [s (results-seq results)]
;;    (when-let [s (seq results)]
;;      (cons 
;;       (reduce conj {} (map (fn [binding]
;;                              (vector (variable (.getName binding))
;;                                      (clj-ify (.getValue binding))))
;;                            (first s)))
;;       (result-map (rest s))))))

;;; --------------------------------------------------------
;;; clj-ify
;;; --------------------------------------------------------

;; (defmethod clj-ify org.openrdf.query.TupleQueryResult [results]
;;   (result-map (jena-iteration-seq results)))

(defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.LiteralImpl [kb l] 
  (.getValue l))


;;; --------------------------------------------------------
;;; main query helper
;;; --------------------------------------------------------

(defn jena-query-setup [kb query-string]
  (QueryExecutionFactory/create (QueryFactory/create query-string)
                                (model! kb)))

(defn jena-result-to-map [kb result]
  (let [vars (iterator-seq (.varNames result))]
    (reduce conj {} (map (fn [var]
                           (vector (variable var)
                                   (clj-ify kb (.get result var))))
                         vars))))

;;; --------------------------------------------------------
;;; main queries
;;; --------------------------------------------------------

;;this returns a boolean
(defn jena-ask-sparql [kb query-string]
  (let [qexec (jena-query-setup kb query-string)
        result (.execAsk qexec)]
    (.close qexec)
    result))

;;this returns a binding set iteration type thingy that can be clj-ified
(defn jena-query-sparql [kb query-string]
  (let [qexec (jena-query-setup kb query-string)]
    (try
     (doall (map (partial jena-result-to-map kb)
                 (iterator-seq (.execSelect qexec))))
     (finally (.close qexec)))))

(defn jena-count-sparql [kb query-string]
  (let [qexec (jena-query-setup kb query-string)]
    (try
     (count (iterator-seq (.execSelect qexec)))
     (finally (.close qexec)))))


;;TODO verify if this is correct?
(defn jena-visit-sparql [kb visitor query-string]
  (let [qexec (jena-query-setup kb query-string)]
    (try
      (dorun (map (fn [result]
                    (visitor (jena-result-to-map kb result)))
                 (iterator-seq (.execSelect qexec))))
     (finally (.close qexec)))))


;;this returns a boolean
(defn jena-ask-pattern [kb pattern & [options]]
  (jena-ask-sparql kb (sparql-ask-query pattern options)))
  ;; (let [qexec (jena-query-setup kb (sparql-ask-query pattern))
  ;;       result (.execAsk qexec)]
  ;;   (.close qexec)
  ;;   result))

;;this returns a binding set iteration type thingy that can be clj-ified
(defn jena-query-pattern [kb pattern & [options]]
  (jena-query-sparql kb (sparql-select-query pattern options)))
  ;; (let [qexec (jena-query-setup kb (sparql-select-query pattern))]
  ;;   (try
  ;;    (doall (map jena-result-to-map
  ;;                (iterator-seq (.execSelect qexec))))
  ;;    (finally (.close qexec)))))

(defn jena-count-pattern [kb pattern & [options]]
  (jena-count-sparql kb (sparql-select-query pattern options)))
  ;; (let [qexec (jena-query-setup kb (sparql-select-query pattern))]
  ;;   (try
  ;;    (count (iterator-seq (.execSelect qexec)))
  ;;    (finally (.close qexec)))))
;;  (count-results (jena-query-pattern kb pattern)))

(defn jena-visit-pattern [kb visitor pattern & [options]]
  (jena-visit-sparql kb visitor (sparql-select-query pattern options)))


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


