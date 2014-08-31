(ns edu.ucdenver.ccp.kr.sesame.rdf
  (use edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       [edu.ucdenver.ccp.kr.rdf :exclude (resource)]
       clojure.java.io)
  (import 
   java.io.IOException

   ;interfaces
   org.openrdf.model.URI
   org.openrdf.model.Resource
   org.openrdf.model.Statement
   org.openrdf.model.Literal

   ;Classes
   org.openrdf.model.impl.StatementImpl
   org.openrdf.model.impl.URIImpl
   org.openrdf.model.impl.LiteralImpl

   org.openrdf.repository.Repository
   org.openrdf.repository.http.HTTPRepository
   org.openrdf.repository.RepositoryConnection
   org.openrdf.query.resultio.TupleQueryResultFormat
   org.openrdf.rio.RDFFormat))

;;; --------------------------------------------------------
;;; specials
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn sesame-iteration-seq [results]
  (lazy-seq
   (when (.hasNext results)
     (cons 
      (.next results)
      (sesame-iteration-seq results)))))


;;; --------------------------------------------------------
;;; sesame specific connection
;;; --------------------------------------------------------

(defn connection! [kb]
  ^RepositoryConnection (:connection (connection kb)))

;;; --------------------------------------------------------
;;; graphs and models
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; namespaces
;;; --------------------------------------------------------

(defn sesame-register-ns [kb short long]
  (.setNamespace (connection! kb) short long))

;;TODO? use clj-ify under the hood?
(defn sesame-server-ns-map [kb] 
  (reduce (fn [m ns]
            (assoc m (.getPrefix ns) (.getName ns)))
          {}
          (doall
           (sesame-iteration-seq
            (.getNamespaces (connection! kb))))))

;;; --------------------------------------------------------
;;; sesame-ify
;;; --------------------------------------------------------

(defn sesame-uri [uri-string]
  (URIImpl. uri-string))
;;maybe this shoudl be
;;  (.createURI (kb :value-factory) uri-string))

(defn sesame-create-resource 
  [kb r] 
  ^Resource (sesame-uri r))

(defn sesame-create-property 
  [kb p] 
  (sesame-uri p))

(defn sesame-create-blank-node 
  [kb id] 
  (.createBNode (:value-factory kb) id))

(defn sesame-create-literal 
  ([kb l]
     (.createLiteral (:value-factory kb) l))
  ([kb s type-or-lang]
     ;(println kb s type-or-lang)
     (.createLiteral (:value-factory kb)
                     s 
                     (if (string? type-or-lang)
                       type-or-lang
                       (edu.ucdenver.ccp.kr.rdf/resource kb type-or-lang)))))


(defn sesame-create-statement 
  [kb s p o]
    (StatementImpl. s p o))

(defn sesame-context-array
  ([] (make-array Resource 0))
  ([kb c] (if c 
            (let [a (make-array Resource 1)]
              (aset a 0 ^Resource (edu.ucdenver.ccp.kr.rdf/resource kb c))
              ;;(sesame-uri (resource-ify c)))
              a)
            (sesame-context-array)))
  ([kb c & rest] (let [a (make-array Resource (+ 1 (count rest)))]
                   (map (fn [v i]
                          (aset a i (edu.ucdenver.ccp.kr.rdf/resource kb v)))
                     (cons c rest)
                     (range))
                a)))

;;; --------------------------------------------------------
;;; clj-ify
;;; --------------------------------------------------------


;;; literal-types
;;; --------------------------------------------------------


(defmulti literal-clj-ify (fn [kb l]
                            (let [t (.getDatatype l)]
                              (and t (.toString t)))))

(defmethod literal-clj-ify :default [kb l]
  (.stringValue l))

;;for some reason strings come back as nil which is different than :default
(defmethod literal-clj-ify nil [kb l]
  (.stringValue l))

;; need to flesh this out and test...

;; (defmethod literal-clj-ify "http://www.w3.org/2001/XMLSchema#integer" [kb l]
;;   (.intValue l))

(defmacro lit-clj-ify [type & body]
  `(defmethod literal-clj-ify ~type ~(vec '(kb l))
     ~@body))

(lit-clj-ify "http://www.w3.org/2001/XMLSchema#boolean" (.booleanValue l))

(lit-clj-ify "http://www.w3.org/2001/XMLSchema#int" (.intValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#integer" (.integerValue l))

(lit-clj-ify "http://www.w3.org/2001/XMLSchema#long" (.longValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#float" (.floatValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#double" (.doubleValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#decimal" (.decimalValue l))

(lit-clj-ify "http://www.w3.org/2001/XMLSchema#dateTime" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#time" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#date" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#gYearMonth" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#gMonthDay" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#gYear" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#gMonth" (.calendarValue l))
(lit-clj-ify "http://www.w3.org/2001/XMLSchema#gDay" (.calendarValue l))


(defn literal-to-string-value [kb l]
  (.stringValue l))
  ;;(str l))

(defn literal-to-value [kb l]
  (literal-clj-ify kb l)) 

(defn literal-language [l]
  (let [lang (.getLanguage l)]
    (if (= "" lang)
      nil
      lang)))


(defn literal-type-or-language [kb l]
  (or (let [dt (.getDatatype l)]
        (and dt 
             (clj-ify kb dt)))
             ;;(convert-string-to-sym kb dt)))
      (literal-language l)))
      ;;(.getLanguage l)))

(defn literal-to-clj [kb l]
  (clj-ify-literal kb l 
                   literal-to-value 
                   literal-to-string-value 
                   literal-type-or-language))

;;; clj-ify
;;; --------------------------------------------------------

(defmethod clj-ify org.openrdf.model.URI [kb r] 
  (if (or (= "" (.getLocalName r))
          (= "" (.getNamespace r)))
    (convert-string-to-sym kb (.stringValue r))
    (convert-string-to-sym kb 
                           (.stringValue r) 
                           (.getNamespace r) 
                           (.getLocalName r))))

(defmethod clj-ify org.openrdf.model.Resource [kb r] 
  (convert-string-to-sym kb (.stringValue r)))

(defmethod clj-ify org.openrdf.model.BNode [kb bnode]
  (symbol *anon-ns-name* (.stringValue bnode)))


;;need to get numbers back out of literals
;;(defmethod clj-ify org.openrdf.model.impl.LiteralImpl [kb v]
(defmethod clj-ify org.openrdf.model.Literal [kb v]
  (literal-to-clj kb v))
  ;;(literal-clj-ify kb v))


(defmethod clj-ify org.openrdf.model.Value [kb v]
  (.stringValue v))

(prefer-method clj-ify [org.openrdf.model.Literal] [org.openrdf.model.Value])


(defmethod clj-ify org.openrdf.model.Statement [kb s]
  (list (clj-ify kb (.getSubject s))
        (clj-ify kb (.getPredicate s))
        (clj-ify kb (.getObject s))))

(defmethod clj-ify org.openrdf.repository.RepositoryResult [kb results]
  (map (partial clj-ify kb)
       (sesame-iteration-seq results)))

;;; --------------------------------------------------------
;;; adding
;;; --------------------------------------------------------

(defn sesame-add-statement
  ([kb stmt] (.add (connection! kb)
                   ^Statment stmt 
                   (sesame-context-array))) ;;(make-array Resource 0)))
  ([kb stmt context] (.add (connection! kb)
                           ^Statement stmt 
                           (sesame-context-array kb context)))
  ([kb s p o] (.add (connection! kb) 
                    ^Statement (statement kb s p o)
                    (sesame-context-array)))
  ([kb s p o context] (.add (connection! kb) 
                            ^Statement (statement kb s p o)
                            ;;^Resource s p o 
                            (sesame-context-array kb context))))


(defn sesame-add-statements
  ([kb stmts] (.add (connection! kb)
                    ^Iterable (map (fn [s]
                                     (apply statement kb s))
                                   stmts)
                    (sesame-context-array)))
  ([kb stmts context] (.add (connection! kb) 
                            ^Iterable (map (fn [s]
                                             (apply statement kb s))
                                           stmts)
                            (sesame-context-array kb context))))

(defmulti convert-to-sesame-type identity)
(defmethod convert-to-sesame-type :n3 [sym] RDFFormat/N3)
(defmethod convert-to-sesame-type :ntriple [sym] RDFFormat/NTRIPLES)
(defmethod convert-to-sesame-type :rdfxml [sym] RDFFormat/RDFXML)
(defmethod convert-to-sesame-type :trig [sym] RDFFormat/TRIG)
(defmethod convert-to-sesame-type :trix [sym] RDFFormat/TRIX)
(defmethod convert-to-sesame-type :turtle [sym] RDFFormat/TURTLE)

(defn sesame-load-rdf-file
  ([kb file] 
     (.add (connection! kb) 
           file
           "" ;nil ;""
           (RDFFormat/forFileName (.getName file))
           (sesame-context-array kb *graph*)))
  ([kb file type]
     (.add (connection! kb)
           file
           "" ;nil ;""
           (convert-to-sesame-type type)
           (sesame-context-array kb *graph*))))

(defn sesame-load-rdf-stream
  ([kb stream]
     (throw (IOException. "Unknown RDF format type for stream.")))
  ([kb stream type]
     (.add (connection! kb)
           stream
           "" ;nil ;""
           (convert-to-sesame-type type)
           (sesame-context-array kb *graph*))))


;;; --------------------------------------------------------
;;; querying
;;; --------------------------------------------------------

(defn sesame-ask-statement
  ([kb s p o context] 
     (.hasStatement ^RepositoryConnection (connection! kb) 
                    ^Resource (and s (edu.ucdenver.ccp.kr.rdf/resource kb s))
                    ^URI (and p (property kb p))
                    ^Value (and o (object kb o))
                    *use-inference*
                    (sesame-context-array kb context))))

(defn sesame-query-statement
  ([kb s p o context]
     (clj-ify kb
       (.getStatements ^RepositoryConnection (connection! kb) 
                       ^Resource (and s (edu.ucdenver.ccp.kr.rdf/resource kb s))
                       ^URI (and p (property kb p))
                       ^Value (and o (object kb o))
                       *use-inference*
                       (sesame-context-array kb context)))))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
