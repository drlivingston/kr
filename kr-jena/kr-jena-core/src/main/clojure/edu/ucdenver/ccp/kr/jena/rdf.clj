(ns edu.ucdenver.ccp.kr.jena.rdf
  (use edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       [clojure.java.io :exclude (resource)])
  (import java.io.IOException

          com.hp.hpl.jena.graph.Graph
          com.hp.hpl.jena.graph.Triple
          com.hp.hpl.jena.graph.GraphMaker
          com.hp.hpl.jena.rdf.model.Model
          com.hp.hpl.jena.rdf.model.Property
          com.hp.hpl.jena.rdf.model.RDFNode
          com.hp.hpl.jena.rdf.model.Resource
          com.hp.hpl.jena.rdf.model.Statement
          com.hp.hpl.jena.rdf.model.StmtIterator
          com.hp.hpl.jena.rdf.model.AnonId
          
          com.hp.hpl.jena.util.FileManager
          
          com.hp.hpl.jena.datatypes.TypeMapper))


;;; --------------------------------------------------------
;;; specials and types
;;; --------------------------------------------------------

(def ^:dynamic *force-add-named-to-default* nil)

;;; --------------------------------------------------------
;;; graphs and models
;;; --------------------------------------------------------

(defn default-model [kb]
  (.getDefaultModel (:dataset kb)))

(defn default-kb [kb]
  (dissoc kb :active-model))


;; should we implement the default jena stores?
;;this is a place holder while decisions are made about how to represent models
(defn model [kb]
  (if (instance? com.hp.hpl.jena.rdf.model.ModelCon kb)
    kb
    (or (get kb :active-model)
        (default-model kb))))


(defn default-model-active? [kb]
  (or (= kb (get kb :active-model kb)) ;no active model kb is it's own sentinel
      (= (default-model kb) (model kb))))



;; this seems broken?
(defn model! [kb]
  (model kb))

(defn named-model [kb name]
  (cond
   (instance? com.hp.hpl.jena.rdf.model.ModelCon name) name
   (nil? name) (default-model kb)
   (= "" name) (default-model kb)
   :else (.getNamedModel (:dataset kb)
                         (if (symbol? name)
                           (sym-to-long-name kb name)
                           (str name)))))

(defn set-active-model [kb m]
  (assoc kb :active-model
         (named-model kb m)))

;;; --------------------------------------------------------
;;; namespace registering and binding
;;; --------------------------------------------------------

;; should we implement the default jena stores?

;; (defmethod register-ns :default [kb short long] 
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))

;; (defmethod update-ns-maps :default [kb]
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))

(defn model-ns-map [model]
  (into {} (.getNsPrefixMap model)))

(defn jena-server-ns-map [kb]
  (model-ns-map (model kb)))

(defn jena-register-ns [kb short long]
  (.setNsPrefix (model kb) short long))


;;; --------------------------------------------------------
;;; creating Jena Resources/Properties from clj data
;;; --------------------------------------------------------


(defn jena-anon-id [s]
  (AnonId. s))


;;TODO: these resouce-ify calls should be redundant
(defn jena-create-resource [m x]
  (.createResource (model m) x)) ;(resource-ify m x)))

;;TODO: these resouce-ify calls should be redundant
(defn jena-create-property [m x]
  (.createProperty (model m) x)) ;(resource-ify m x)))


;;how to get literal types for jena from a URI
;; (.getTypeByName (com.hp.hpl.jena.datatypes.TypeMapper/getInstance) 
;;                 (str (resource my-jena-kb 'xsd/int)))

(defn jena-create-literal 
  ([kb x] (if (string? x)
           (.createLiteral (model kb) x)
           (.createTypedLiteral (model kb) x)))
  ([kb x type] 
     (if (string? type)
       (.createLiteral (model kb) x type)
       (.createTypedLiteral (model kb) 
                            x
                            (.getSafeTypeByName (TypeMapper/getInstance)
                                                (str (resource kb type)))))))

(defn jena-create-statement 
  ([kb [s p o]] (jena-create-statement kb s p o))
  ([kb s p o]
     (.createStatement ^com.hp.hpl.jena.rdf.model.impl.ModelCom (model kb)
                       ^Resource (resource kb s)
                       ^Property (property kb p)
                       ^RDFNode (object kb o))))

(defn jena-create-blank-node 
  ([m] (jena-create-blank-node m *anon-name*))
  ([m n] (.createResource (model m) (jena-anon-id n))))

;;; --------------------------------------------------------
;;; creating clj data structures from Jena objects
;;; --------------------------------------------------------

(defn uri-to-sym [kb r]
  (let [ns (long-ns-to-short-ns kb (.getNameSpace r))]
    (if ns
      (symbol ns (.getLocalName r))
      (convert-string-to-sym kb (str (.getNameSpace r) (.getLocalName r))))))

(defn resource-to-sym [kb r]
  (if (.isAnon r)
    (symbol *anon-ns-name* (str (.getLabelString (.getId r))))
    (uri-to-sym kb r)))
    
(defn clj-ify-statement [kb s]
  (list (clj-ify kb (.getSubject s))
        (clj-ify kb (.getPredicate s))
        (clj-ify kb (.getObject s))))

;; statement pieces
(defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.ResourceImpl [kb r] 
  (resource-to-sym kb r))

(defmethod clj-ify com.hp.hpl.jena.graph.Node_URI [kb r] 
  (uri-to-sym kb r))

(defmethod clj-ify com.hp.hpl.jena.graph.Node_Blank [kb b]
  (symbol *anon-ns-name* (str (.getLabelString (.getBlankNodeId b)))))

(defmethod clj-ify com.hp.hpl.jena.graph.Node_Literal [kb l] 
  (.getLiteralValue l))


;; Properties are resources - nothing to do special
;;   unless meta data is of interest
;; (defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.PropertyImpl [p]
;;   (resource-to-sym p))
(defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.LiteralImpl [kb l] 
  (.getValue l))

;; statements
(defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.StatementImpl [kb s] 
  (clj-ify-statement kb s))

(defmethod clj-ify com.hp.hpl.jena.graph.Triple [kb s] 
  (clj-ify-statement kb s))


;; collections of clj-ifiable things
(defmethod clj-ify com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl [kb s]
  (clj-ify kb (or (iterator-seq s)
                  '())))


;;; --------------------------------------------------------
;;; helping
;;; --------------------------------------------------------

(defn statement-parts [s]
  (list (.getSubject s)
        (.getPredicate s)
        (.getObject s)))

;;; --------------------------------------------------------
;;; adding
;;; --------------------------------------------------------

;;implement the contains and other functions?


(defn check-force-add-to-default [kb stmt]
  (when (and *force-add-named-to-default*
             (not (default-model-active? kb)))
    (add (default-kb kb) stmt)))


(defn jena-add-statement
  ([kb stmt]
     (check-force-add-to-default kb stmt)
     (.add ^Model (model kb) 
           ^Statement (jena-create-statement kb stmt))
     nil)
  ([kb stmt context]
     (let [model (named-model kb context)
           active-kb (set-active-model kb model)]
       (check-force-add-to-default active-kb stmt)
       (.add ^Model model
             ^Statement (jena-create-statement active-kb stmt)))
     nil)
  ([kb s p o]
     (check-force-add-to-default kb [s p o])
     (.add ^Model (model kb) 
           ^Statement (jena-create-statement kb (list s p o)))
     nil)
  ([kb s p o context]
     (let [model (named-model kb context)
           active-kb (set-active-model kb model)]
       (check-force-add-to-default active-kb [s p o])
       (.add ^Model model
             ^Statement (jena-create-statement active-kb (list s p o))))
     nil))


(defn jena-add-statements
  ([kb stmts]
     (when (and *force-add-named-to-default*
                (not (default-model-active? kb)))
       (let [target-model (default-model kb)
             active-kb (set-active-model kb target-model)]
         (.add target-model
               ^Iterable (map (partial jena-create-statement active-kb)
                              stmts))))
     (let [target-model (model kb)
           active-kb (set-active-model kb target-model)]
       (.add target-model
             ^Iterable (map (partial jena-create-statement active-kb)
                            stmts)))
     nil)
  ([kb stmts context]
     (let [model (named-model kb context)
           active-kb (set-active-model kb model)]
       (jena-add-statements active-kb stmts))))
     ;;   (when (not (default-model-active? active-kb))
     ;;     (.add (default-model kb)
     ;;        ^Iterable (map (partial jena-create-statement (default-model kb))
     ;;                          stmts)))
     ;;   (.add active-kb ^Iterable stmts))
     ;; nil))



(defmulti convert-to-jena-type identity)
(defmethod convert-to-jena-type :ntriple [sym] "N-TRIPLE")
;;(defmethod convert-to-jena-type :rdfxml [sym] "RDF/XML-ABBREV")
(defmethod convert-to-jena-type :rdfxml [sym] "RDF/XML")
(defmethod convert-to-jena-type :turtle [sym] "TURTLE")
(defmethod convert-to-jena-type :n3 [sym] "N3")
;;unverified:
(defmethod convert-to-jena-type :trig [sym] "TRIG")
(defmethod convert-to-jena-type :trix [sym] "TRIX")


(defn jena-load-rdf-file
  ([kb file] 
     (.readModel (FileManager/get)
                 (model kb)
                 file))
  ([kb file type]
     (.readModel (FileManager/get)
                 (model kb)
                 file
                 (convert-to-jena-type type))))

(defn jena-load-rdf-stream
  ([kb stream]
     (throw (IOException. "Unknown RDF format type for stream.")))
  ([kb stream type]
     (.read (.getReader (model kb) (convert-to-jena-type type))
            (model kb)
            (reader stream)
            ""))) ;URI base

;;; --------------------------------------------------------
;;; querying
;;; --------------------------------------------------------

;; (defn jena-ask-statement [kb s p o g]
;;   (.contains ^Model (if g
;;                       (named-model kb g)
;;                       (model kb))
;;              ^Resource (and s (resource kb s))
;;              (and p (property kb p))
;;              ^RDFNode (and o (object kb o))))

;; the spec says a null third argument should work fine, but in practice
;;   this generates complaints that a null Literal is illegal...

(defn jena-ask-statement [kb s p o g]
  (let [m (if g
            (named-model kb g)
            (model kb))]
    (if o
      (.contains m
                 ^Resource (and s (resource kb s))
                 (and p (property kb p))
                 ^RDFNode (and o (object kb o)))
      (.contains m 
                 ^Resource (and s (resource kb s))
                 (and p (property kb p))))))

(defn jena-query-statement [kb s p o g]
  (clj-ify kb (.listStatements (if g
                                 (named-model kb g)
                                 (model kb))
                               ^Resource (and s (resource kb s))
                               (and p (property kb p))
                               ^RDFNode (and o (object kb o)))))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------

