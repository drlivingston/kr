(ns edu.ucdenver.ccp.kr.sesame.sparql
  (use edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       edu.ucdenver.ccp.kr.sesame.rdf)

  (import org.openrdf.model.impl.StatementImpl
          org.openrdf.model.impl.URIImpl
          
          org.openrdf.repository.Repository
          org.openrdf.repository.http.HTTPRepository
          org.openrdf.repository.http.HTTPTupleQuery
          org.openrdf.repository.http.HTTPBooleanQuery
          org.openrdf.repository.RepositoryConnection
          org.openrdf.query.TupleQuery
          org.openrdf.query.BooleanQuery
          
          org.openrdf.query.TupleQueryResult
          org.openrdf.query.QueryLanguage
          org.openrdf.query.resultio.TupleQueryResultParserRegistry
          org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParserFactory
          org.openrdf.query.resultio.TupleQueryResultFormat
          org.openrdf.query.TupleQueryResultHandlerBase))


(defn count-results
  ([results] (count-results results 0))
  ([results count]
     (if (.hasNext results)
       (do (.next results)
           (recur results (+ 1 count)))
       count)))


;; (defn results-seq [results]
;;   (lazy-seq
;;    (when (.hasNext results)
;;      (cons 
;;       (.next results)
;;       (results-seq results)))))
  
;;TODO? are these even useful?

(defn result-to-map [kb result]
  (reduce conj {} (map (fn [binding]
                         (vector (variable (.getName binding))
                                 (clj-ify kb (.getValue binding))))
                       result)))

(defn get-results-for-key [key results]
  (set
   (map (fn [result]
          (result key))
        results)))

;;; --------------------------------------------------------
;;; result processing helpsers
;;; --------------------------------------------------------

(defn count-results [results]
  (count (sesame-iteration-seq results)))

(defn clj-ify-bindings [kb bindings]
  (reduce (fn [results binding]
            (conj results
                  (vector (variable (.getName binding))
                          (clj-ify kb (.getValue binding)))))
          {}
          bindings))

(defn result-map [kb results] 
  (lazy-seq
   ;(when-let [s (results-seq results)]
   (when-let [s (seq results)]
     (cons 
      (reduce conj {} (map (fn [binding]
                             (vector (variable (.getName binding))
                                     (clj-ify kb (.getValue binding))))
                           (first s)))
      (result-map kb (rest s))))))

;;TODO 
;; the above can be made to capture the kb if made like the following
;; (defn bar 
;;   ([l] (bar *foo* l))
;;   ([x l]
;;      (lazy-seq
;;       (when-let [s (seq l)]
;;         (cons (list x (first s))
;;               (bar x (rest s)))))))


;;; --------------------------------------------------------
;;; clj-ify
;;; --------------------------------------------------------

(defmethod clj-ify org.openrdf.query.TupleQueryResult [kb results]
  (result-map kb (sesame-iteration-seq results)))


;;; --------------------------------------------------------
;;; main queries
;;; --------------------------------------------------------

;;this returns a boolean
(defn sesame-ask-sparql [kb query-string]
  (.evaluate ^BooleanQuery
             (.prepareBooleanQuery ^RepositoryConnection (connection! kb)
                                 QueryLanguage/SPARQL
                                 query-string)))

(defn sesame-query-sparql [kb query-string]
  ;(prn query-string)
  (let [tuplequery (.prepareTupleQuery ^RepositoryConnection (connection! kb)
                                       QueryLanguage/SPARQL
                                       query-string)]
    (.setIncludeInferred tuplequery *use-inference*)
    (clj-ify kb (.evaluate ^TupleQuery tuplequery))))


(defn sesame-visit-sparql [kb visitor query-string]
  ;;(prn "SESAME-VISIT-SPARQL QUERY STRING: " query-string)
  (let [tuplequery (.prepareTupleQuery ^RepositoryConnection (connection! kb)
                                       QueryLanguage/SPARQL
                                       query-string)]
    (.setIncludeInferred tuplequery *use-inference*)
    (.evaluate ^TupleQuery tuplequery
               (proxy [TupleQueryResultHandlerBase] []
                 (handleSolution [bindings]
                   ;;(visitor (clj-ify kb bindings))))))
                   (visitor (clj-ify-bindings kb bindings)))))))

(defn sesame-visit-count-sparql [kb query-string]
  (let [count (atom 0)
        tuplequery (.prepareTupleQuery ^RepositoryConnection (connection! kb)
                                       QueryLanguage/SPARQL
                                       query-string)]
    (.setIncludeInferred tuplequery *use-inference*)
    (.evaluate ^TupleQuery tuplequery
               (proxy [TupleQueryResultHandlerBase] []
                 (handleSolution [bindings]
                   ;;(if (= 0 (mod @count 1000)) (println @count))
                   (swap! count inc))))
    @count))
;; (defn sesame-visit-count-sparql [kb query-string]
;;   (with-local-vars [count 0]
;;     (.evaluate ^TupleQuery
;;                (.prepareTupleQuery ^RepositoryConnection (connection! kb)
;;                                    QueryLanguage/SPARQL
;;                                    query-string)
;;                (proxy [TupleQueryResultHandlerBase] []
;;                  (handleSolution [bindings]
;;                                  (var-set count 
;;                                           (+ 1 (var-get count))))))
;;     (var-get count)))



(defn sesame-count-sparql [kb query-string]
  (sesame-visit-count-sparql kb query-string))
  ;; (count-results
  ;;  (.evaluate ^TupleQuery
  ;;             (.prepareTupleQuery ^RepositoryConnection (connection! kb)
  ;;                                 QueryLanguage/SPARQL
;;                                 query-string))))

(defn sesame-count-1-1 [kb pattern options]
  ;;TODO this is rediculous, really? there's nothing better?
  ;;     why is the number a raw string? (is this true of all stores?)
  ;;(read-string
   (second
    (first
     (first 
      (sesame-query-sparql kb
                           (sparql-1-1-count-query pattern options))))));)


;;this returns a boolean
(defn ^boolean sesame-ask-pattern [kb pattern & [options]]
  (sesame-ask-sparql kb (sparql-ask-query pattern options)))

(defn sesame-query-pattern [kb pattern & [options]]
  (sesame-query-sparql kb (sparql-select-query pattern options)))

(defn sesame-count-pattern [kb pattern & [options]]
  ;;check if 1.1 support is available and if so try that way
  (if (has-feature? kb sparql-1-1) ;use fast 1.1 query
    (sesame-count-1-1 kb pattern options)
    ;;otherwise turn and burn
    (sesame-count-sparql kb (sparql-select-query pattern options))))

(defn sesame-visit-pattern [kb visitor pattern & [options]]
  (sesame-visit-sparql kb visitor (sparql-select-query pattern options)))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


