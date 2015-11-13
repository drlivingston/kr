(ns edu.ucdenver.ccp.kr.jena.kb
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       edu.ucdenver.ccp.kr.jena.rdf
       edu.ucdenver.ccp.kr.jena.sparql)
  (import org.apache.jena.query.DatasetFactory
          org.apache.jena.sparql.core.DatasetImpl
          org.apache.jena.graph.Graph
          org.apache.jena.rdf.model.Model))

;;; --------------------------------------------------------
;;; specials and types
;;; --------------------------------------------------------

(def ^:dynamic *default-model-type* :rdf) ; :rdfs and :owl supported

;;; --------------------------------------------------------
;;; triple store connection and setup
;;; --------------------------------------------------------

(declare new-jena-model
         jena-connection)

(defn jena-initialize [kb]
  (synch-ns-mappings kb)
  kb)

;;; --------------------------------------------------------
;;; protocols and records
;;; --------------------------------------------------------

;;this is nonsese becasue to the circular defintions
;;  and what can and cannot be forward delcared
(declare new-jena-connection
         close-existing-jena-connection)

(defn jena-connection [kb]
  (new-jena-model kb))

(defn close-jena-connection [kb]
  (close-existing-jena-connection kb))


;;; KB
;;; --------------------------------------------------------

;;there could be different types for different backing-stores TDB/SDB

(defrecord JenaKB [dataset model-type kb-features]
  KB

  (native [kb] dataset)
  (initialize [kb] (jena-initialize kb))
  (open [kb] (new-jena-connection kb))
  (close [kb] (close-jena-connection kb))

  rdfKB

  (root-ns-map [kb] (jena-server-ns-map kb))
  ;; (ns-maps [kb] ns-maps-var)
  ;; (ns-map-to-short [kb] (:ns-map-to-short (deref ns-maps-var)))
  ;; (ns-map-to-long [kb] (:ns-map-to-long (deref ns-maps-var)))
  ;; (ns-map-to-short [kb] (:ns-map-to-short (deref (ns-maps kb)))))
  ;; (ns-map-to-long [kb] (:ns-map-to-long (deref (ns-maps kb)))))
  (register-ns [kb short long] (jena-register-ns kb short long))
  
  (create-resource [kb name] (jena-create-resource kb name))
  (create-property [kb name] (jena-create-property kb name))
  (create-literal [kb val] (jena-create-literal kb val))
  (create-literal [kb val type] (jena-create-literal kb val type))

  ;;TODO convert to creating proper string literals
  ;; (create-string-literal [kb str] (jena-create-string-iteral kb val))
  ;; (create-string-literal [kb str lang] 
  ;;                        (jena-create-string literal kb val type))
  (create-string-literal [kb str] (jena-create-literal kb str))
  (create-string-literal [kb str lang] 
                         (jena-create-literal kb str lang))


  (create-blank-node [kb name] (jena-create-blank-node kb name))
  (create-statement [kb s p o] (jena-create-statement kb s p o))

  (add-statement [kb stmt] (jena-add-statement kb stmt))
  (add-statement [kb stmt context] (jena-add-statement kb stmt context))
  (add-statement [kb s p o] (jena-add-statement kb s p o))
  (add-statement [kb s p o context] (jena-add-statement kb s p o context))
  (add-statements [kb stmts] (jena-add-statements kb stmts))
  (add-statements [kb stmts context] (jena-add-statements kb stmts context))

  ;; (ask-statement [kb stmt] (jena-ask-statement kb stmt))
  (ask-statement  [kb s p o context] (jena-ask-statement kb s p o context))
  ;; (query-statement [kb stmt] (jena-query-statement kb stmt))
  (query-statement [kb s p o context] (jena-query-statement kb s p o context))

  
  (load-rdf-file [kb file] (jena-load-rdf-file kb file))
  (load-rdf-file [kb file type] (jena-load-rdf-file kb file type))

  ;;the following will throw exception for unknown rdf format
  (load-rdf-stream [kb stream] (jena-load-rdf-stream kb stream))

  (load-rdf-stream [kb stream type] (jena-load-rdf-stream kb stream type))


  sparqlKB

  (ask-pattern [kb pattern] 
    (jena-ask-pattern kb pattern))
  (ask-pattern [kb pattern options] 
    (jena-ask-pattern kb pattern options))
  
  (query-pattern [kb pattern]
        (jena-query-pattern kb pattern))
  (query-pattern [kb pattern options]
        (jena-query-pattern kb pattern options))

  (count-pattern [kb pattern]
        (jena-count-pattern kb pattern))
  (count-pattern [kb pattern options]
        (jena-count-pattern kb pattern options))

  (visit-pattern [kb visitor pattern]
        (jena-visit-pattern kb visitor pattern))
  (visit-pattern [kb visitor pattern options]
    (jena-visit-pattern kb visitor pattern options))

  (construct-pattern [kb create-pattern pattern]
        (jena-construct-pattern kb create-pattern pattern))
  (construct-pattern [kb create-pattern pattern options]
        (jena-construct-pattern kb create-pattern pattern options))
  (construct-visit-pattern [kb visitor create-pattern pattern]
    (jena-construct-visit-pattern kb visitor create-pattern pattern))
  (construct-visit-pattern [kb visitor create-pattern pattern options]
    (jena-construct-visit-pattern kb visitor create-pattern pattern options))
  

  (ask-sparql [kb query-string]
    (jena-ask-sparql kb query-string))
  (query-sparql [kb query-string]
    (jena-query-sparql kb query-string))
  (count-sparql [kb query-string]
    (jena-count-sparql kb query-string))
  (visit-sparql [kb visitor query-string]
    (jena-visit-sparql kb visitor query-string))

  (construct-sparql [kb sparql-string]
    (jena-construct-sparql kb sparql-string))
  (construct-visit-sparql [kb visitor sparql-string]
    (jena-construct-visit-sparql kb visitor sparql-string))



  )

;;models:

;; createOntologyModel() Creates an ontology model which is in-memory and presents OWL ontologies.

;; createOntologyModel(OntModelSpec spec, Model base) Creates an ontology model according the OntModelSpec spec which presents the ontology of base.

;; createOntologyModel(OntModelSpec spec, ModelMaker maker, Model base) Creates an OWL ontology model according to the spec over the base model. If the ontology model needs to construct additional models (for OWL imports), use the ModelMaker to create them. [The previous method will construct a MemModelMaker for this.]

;; Where do OntModelSpecs come from? There's a cluster of constants in the class which provide for common uses; to name but three: - OntModelSpec.OWL_MEM_RDFS_INF OWL ontologies, model stored in memory, using RDFS entailment only

;; OntModelSpec.RDFS_MEM RDFS ontologies, in memory, but doing no additional inferences

;; OntModelSpec.OWL_DL_MEM_RULE_INF OWL ontologies, in memory, with the full OWL Lite inference



;;; "constructors"
;;; --------------------------------------------------------
    
;; the way new JenaKBConnection is being called it isn't preserving
;;   the additional keys that are added on to the jena server
;;   specifically the :value-factory

(defn copy-jena-slots [target-kb source-kb]
  (copy-rdf-slots (copy-kb-slots target-kb source-kb)
                  source-kb))


(defn new-jena-kb
  ([] (new-jena-kb (org.apache.jena.query.DatasetFactory/createMem)))
  ([dataset]
     (jena-initialize
      (initialize-ns-mappings
       (JenaKB. dataset *default-model-type* nil)))))

(defn jena-kb-from-model [model]
  ;;creates a dataset with this model as the default model
  (let [dataset (org.apache.jena.query.DatasetFactory/create model)]
    (new-jena-kb dataset)))

;; (defn new-jena-server [model-factory]
;;   (JenaKB. (:server model-factory) (initial-ns-mappings) nil))
  ;; (let [repository (HTTPRepository. *default-server* *repository-name*)]
  ;;   (.setPreferredTupleQueryResultFormat repository
  ;;                                        TupleQueryResultFormat/SPARQL)
  ;;   (if (and *username* *password*)
  ;;     (.setUsernameAndPassword repository *username* *password*))
  ;;   (assoc (JenaKB. repository (initial-ns-mappings))
  ;;     :value-factory (.getValueFactory repository))))

;;Jena uses locking symantics and supports multiple reader single writer
;;  semantics --- this is currently unhandled with un-predicable results
(defn new-jena-connection [kb]
  kb)
  
(defmethod kb :jena-mem [_]
  (new-jena-kb))

(defmethod kb org.apache.jena.sparql.core.DatasetImpl [arg]
  (if (class? arg)
    (new-jena-kb)
    (new-jena-kb arg)))

(defmethod kb org.apache.jena.rdf.model.ModelCon [arg]
  (if (class? arg)
    (new-jena-kb)
    (jena-kb-from-model arg)))


(defn close-existing-jena-connection [kb]
  (when (:dataset kb)
    (doseq [model (iterator-seq (.listNames (:dataset kb)))]
      (.close (.getNamedModel (:dataset kb) name)))
    (.close (.getDefaultModel (:dataset kb)))
    (.close (:dataset kb)))
  (copy-jena-slots (JenaKB. nil (:model-type kb) nil)
                   kb))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------

