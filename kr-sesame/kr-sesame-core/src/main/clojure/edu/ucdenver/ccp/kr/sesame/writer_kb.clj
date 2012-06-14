(ns edu.ucdenver.ccp.kr.sesame.writer-kb
  (use edu.ucdenver.ccp.kr.kb
        [edu.ucdenver.ccp.kr.rdf :exclude (resource)]
        edu.ucdenver.ccp.kr.sesame.kb
        [edu.ucdenver.ccp.kr.sesame.rdf :exclude (resource)]
        [clojure.java.io :exclude (resource)])
  (import  ;;org.openrdf.model.impl.ValueFactoryBase
           org.openrdf.model.impl.ValueFactoryImpl
           java.nio.charset.Charset
           (org.openrdf.rio Rio
                            RDFFormat
                            RDFWriter
                            RDFWriterFactory)
           org.openrdf.rio.ntriples.NTriplesWriterFactory))

;;; --------------------------------------------------------
;;; connections
;;; --------------------------------------------------------

;;this is nonsese becasue to the circular defintions
;;  and what can and cannot be forward delcared
(declare initialize-sesame-writer
         open-sesame-writer
         close-sesame-writer
         sesame-write-statement
         sesame-write-statements)

;;; --------------------------------------------------------
;;; protocol implementation
;;; --------------------------------------------------------

(defrecord SesameWriterKB [target connection]
  KB

  (native [kb] target)
  (initialize [kb] kb) ;(initialize-sesame-writer kb))
  (open [kb] (open-sesame-writer kb))
  (close [kb] (close-sesame-writer kb))

  rdfKB

  ;; (ns-maps [kb] ns-maps-var)
  ;; (ns-map-to-short [kb] (:ns-map-to-short (deref ns-maps-var)))
  ;; (ns-map-to-long [kb] (:ns-map-to-long (deref ns-maps-var)))
  (root-ns-map [kb] (ns-map-to-long kb))
  (register-ns [kb short long] nil) ; no-op
  
  (create-resource [kb name] (sesame-create-resource kb name))
  (create-property [kb name] (sesame-create-property kb name))
  (create-literal [kb val] (sesame-create-literal kb val))
  (create-literal [kb val type] (sesame-create-literal kb val type))

  ;;TODO convert to creating proper string literals
  ;; (create-string-literal [kb str] (sesame-create-string-iteral kb val))
  ;; (create-string-literal [kb str lang] 
  ;;                        (sesame-create-string literal kb val type))
  (create-string-literal [kb str] (sesame-create-literal kb str))
  (create-string-literal [kb str lang] 
                         (sesame-create-literal kb str lang))


  (create-blank-node [kb name] (sesame-create-blank-node kb name))
  (create-statement [kb s p o] (sesame-create-statement kb s p o))

  (add-statement [kb stmt] (sesame-write-statement kb stmt))
  (add-statement [kb stmt context] (sesame-write-statement kb stmt context))
  (add-statement [kb s p o] (sesame-write-statement kb s p o))
  (add-statement [kb s p o context] (sesame-write-statement kb s p o context))

  (add-statements [kb stmts] (sesame-write-statements kb stmts))
  (add-statements [kb stmts context] (sesame-write-statements kb stmts context))

  ;; (ask-statement  [kb s p o context] (sesame-ask-statement kb s p o context))
  ;; (query-statement [kb s p o context]
  ;;   (sesame-query-statement kb s p o context))

  ;; (load-rdf-file [kb file] (sesame-load-rdf-file kb file))
  ;; (load-rdf-file [kb file type] (sesame-load-rdf-file kb file type))
  ;;the following will throw exception for unknown rdf format
  ;;(load-rdf-stream [kb stream] (sesame-load-rdf-stream kb stream))
  ;;(load-rdf-stream [kb stream type] (sesame-load-rdf-stream kb stream type))
)

;;; "constructors"
;;; --------------------------------------------------------
    
(defn new-writer [target]
  (let [writer (Rio/createWriter RDFFormat/NTRIPLES
                                 (output-stream target))]
    (.startRDF writer) ;side effect function doesn't return itself
    writer))

(defn open-sesame-writer [kb]
  (copy-sesame-slots (assoc (SesameWriterKB. (:target kb)
                                             (new-writer (:target kb)))
                       :value-factory (:value-factory kb))
                     kb)) 

(defn close-sesame-writer [kb]
  (if (:connection kb)
    (.endRDF (:connection kb)))
  (copy-sesame-slots (assoc (SesameWriterKB. (:target kb)
                                             nil)
                       :value-factory (:value-factory kb))
                     kb))

(defn new-sesame-writer-kb [target]
  (initialize-ns-mappings
   (assoc (SesameWriterKB. target nil) ;(initial-ns-mappings) nil)
     :value-factory (org.openrdf.model.impl.ValueFactoryImpl.))))
  ;;(.getValueFactory repository)))


;;these can't handle graphs ... TODO change to NQUAD writer??

(defn sesame-write-statement
  ([kb stmt] (.handleStatement (connection! kb)
                               ^Statment stmt))
  ([kb stmt context] (.handleStatement (connection! kb)
                                       ^Statement stmt))
  ([kb s p o] (.handleStatement (connection! kb) 
                                ^Statement (statement kb s p o)))
  ([kb s p o context] (.handleStatement (connection! kb) 
                                        ^Statement (statement kb s p o))))


(defn sesame-write-statements
  ([kb stmts] (dorun (map (partial sesame-write-statement kb) stmts)))
  ([kb stmts context]  (dorun (map (partial sesame-write-statement kb) stmts))))


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
