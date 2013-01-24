(ns edu.ucdenver.ccp.kr.rdf
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.variable)
  (import java.util.UUID
          java.io.File))

;;forward declarations
(declare update-ns-maps
         update-root-ns-map
         bnode
         blank-node)

;;; --------------------------------------------------------
;;; specials
;;; --------------------------------------------------------

(def ^:dynamic *use-inference* true)

(def ^:dynamic *graph* nil)

;; this will infer xsd:types for literals
;; it's unlikely you will want to turn this off
(def ^:dynamic *infer-literal-type* true)

;; when there are plain strings "foo" this determines whether they
;;   get an inferred language and what language (if the above is on)
(def ^:dynamic *use-default-language* true)
(def ^:dynamic *string-literal-language* "en")

;; when there are literals in results this determines whether or not
;;   they are boxed with their type / language
;; funtion that takes a boxed literal and determines what to do with it
(def ^:dynamic *box-literal?* (constantly false))
;; a function that tested for lang string (or just string) could be used to
;;   only box the strings



;;these can be used as overrides / extensions - with a performance hit
;;  or maybe they should be the only things used and factor it out of the KB
(defonce ^:dynamic *ns-map-to-short* nil)
(defonce ^:dynamic *ns-map-to-long* nil)


;;for now anon / blank nodes will be put in no ns
;;  those symbols with an ns are assumed to be URI qualified
(def ^:dynamic *anon-name* "anon")
;;(def *anon-ns* (create-ns (symbol *anon-ns-name*)))

(def ^:dynamic *anon-ns-name* "_")


;;; --------------------------------------------------------
;;; protocols
;;; --------------------------------------------------------

(defprotocol rdfKB
 
  (root-ns-map [kb] "gets the servers version of the namesapce map")
  ;; (ns-maps [kb] "gets the pair of namespace mappings")
  ;; (ns-map-to-short [kb] "gets the namespace mappings from long to short")
  ;; (ns-map-to-long [kb] "gets the namespace mappings from short to long")
  (register-ns [kb short long] "adds an namespace mapping")
  
  (create-resource [kb name] "creates a resource object")
  (create-property [kb name] "creates a property object")
  (create-literal [kb val] ;"creates a literal, infers type")
                  [kb val type] "creates a literal object, forces type")
  (create-string-literal [kb str] [kb str lang]
                         "creates a string literal with optional language")
  (create-blank-node [kb name] "creates a blank-node object")
  (create-statement [kb s p o] "creates a statement")
  

  (add-statement [kb stmt] [kb stmt context] [kb s p o] [kb s p o context]
                 "adds a statement to the kb")
  (add-statements [kb stmts] [kb stmts context] "adds a statement to the kb")

  (ask-statement ;;[kb stmt] [kb stmt context] [kb s p o] 
                 [kb s p o context]
                 ;;[kb stmt] [kb s p o]
                 "boolean asks for a statement or pattern in the kb")

  (query-statement ;[kb stmt] [kb stmt context] [kb s p o]
                   [kb s p o context]
                   ;;[kb stmt] [kb s p o]
                   "gets bindings for statement pattern")

  (load-rdf-file [kb file] [kb file type] "loads an rdf file")
  (load-rdf-stream [kb stream] [kb stream type] "loads rdf from stream")
  )

;;pays attention to *graph*
;; (load [kb source type]
;; (load-to [kb source type]

;;; --------------------------------------------------------
;;; triple store connection and setup
;;; --------------------------------------------------------

;; (defn reset-globals []
;;   (alter-var-root #'*kb* (fn [old] nil))
;;   (alter-var-root #'*ns-map-to-short* (fn [old] nil))
;;   (alter-var-root #'*ns-map-to-long* (fn [old] nil)))

;;; --------------------------------------------------------
;;; namespace registering and binding
;;; --------------------------------------------------------


;; (defn initial-ns-mappings []
;;   (atom {:ns-map-to-short {} :ns-map-to-long {}}))

(defn initialize-ns-mappings [kb & [{to-short :ns-map-to-short
                                     to-long  :ns-map-to-long}]]
  (assoc kb
    :ns-map-to-short (or to-short {})
    :ns-map-to-long (or to-long {})))

(defn update-ns-mapping [kb short long]
  (assoc kb
    :ns-map-to-short (assoc (:ns-map-to-short kb) long  short)
    :ns-map-to-long  (assoc (:ns-map-to-long kb)  short long)))
  ;; (register-ns kb short long)
  ;; (swap! (ns-maps kb)
  ;;        (fn [{to-short :ns-map-to-short to-long :ns-map-to-long}]
  ;;          {:ns-map-to-short (assoc to-short long short)
  ;;           :ns-map-to-long (assoc to-long short long)})))

    ;; (swap! (ns-maps kb)
    ;;      (fn [{to-short :ns-map-to-short to-long :ns-map-to-long}]
    ;;        {:ns-map-to-short long-to-short
    ;;         :ns-map-to-long short-to-long}))))    

(defn update-namespaces [kb short-to-long-pairs]
  (reduce (fn [kb [short long]]
            (update-ns-mapping kb short long))
          kb
          short-to-long-pairs))
  ;;(synch-ns-mappings kb)
  ;; (dorun
  ;;  (map (fn [[short long]]
  ;;         (update-ns-mapping kb short long))
  ;;       short-to-long-pairs))
  ;; kb
  ;; (synch-ns-mappings kb))

(defn synch-ns-mappings [kb]
  (let [short-to-long (root-ns-map kb)
        long-to-short (reduce (fn [m [k v]]
                                (assoc m v k))
                              {}
                              short-to-long)]
    (assoc kb
      :ns-map-to-short long-to-short
      :ns-map-to-long  short-to-long)))

(defn register-namespace [kb short long]
  (register-ns kb short long)
  (update-ns-mapping kb short long))

(defn register-namespaces [kb short-to-long-pairs]
  (reduce (fn [kb [short long]]
            (register-namespace kb short long))
          kb
          short-to-long-pairs))


(defn ns-map-to-short [kb]
  (get kb :ns-map-to-short {}))

(defn ns-map-to-long [kb]
  (get kb :ns-map-to-long {}))


;;; convert long and short names
;;; --------------------------------------------------------

(defn attempt-long-name-to-sym [uri-string [long short]]
  (let [name (second (.split uri-string long))]
    (and name
         (symbol short name))))

(defn infer-first-ns [uri-string]
  (or (some (fn [mapping] (attempt-long-name-to-sym uri-string mapping))
            *ns-map-to-short*)
      (some (fn [mapping] (attempt-long-name-to-sym uri-string mapping))
            (and *kb*
                 (ns-map-to-short *kb*)))
      ;;couldn't find an ns - just make a symbol out of it?
      (symbol uri-string)))

(defn shortest-name
  ([] nil)
  ([x] x)
  ([x y] (cond
          (nil? x) y
          (nil? y) x
          (< (.length (name y)) (.length (name x))) y
          :else x)))

;; TODO
;; is there anyway to speed this up?  this could get called once per sym
(defn infer-longest-ns 
  ([uri-string] (infer-longest-ns *kb* *ns-map-to-short* uri-string))
  ([kb uri-string] (infer-longest-ns kb *ns-map-to-short* uri-string))
  ([kb to-short-map uri-string]
     (or
      (try (or (reduce shortest-name
                       (map (partial attempt-long-name-to-sym uri-string)
                            to-short-map))
               (reduce shortest-name
                       (map (partial attempt-long-name-to-sym uri-string)
                            (and kb
                                 (ns-map-to-short kb)))))
           (catch NullPointerException npe
             (prn "rdf/infer-longest-ns caught npe" uri-string)))
      ;;couldn't find an ns - just make a symbol out of it?
      ;;TODO this is kinda broken - will infer a break at at / and eat one?
      (symbol uri-string))))


;; (defn infer-first-ns [uri-string]
;;   (or (some (fn [[long short]]
;;               (let [name (second (.split uri-string long))]
;;                 (and name
;;                      (symbol short name))))
;;             *ns-map-to-short*)
;;       ;;couldn't find an ns - just make a symbol out of it?
;;       (symbol uri-string)))
;;           ;; (if (.startsWith uri-string long)
;;           ;;   (symbol short (s(


(defn long-ns-to-short-ns 
  ([long] (long-ns-to-short-ns *kb* *ns-map-to-short* long))
  ([kb long] (long-ns-to-short-ns kb *ns-map-to-short* long))
  ([kb ns-map long]
     (or (and ns-map
              (get ns-map long))
         (and kb
              (get (ns-map-to-short kb) long)))))

(defn short-ns-to-long-ns 
  ([short] (short-ns-to-long-ns *kb* *ns-map-to-long* short))
  ([kb short] (short-ns-to-long-ns kb *ns-map-to-long* short))
  ([kb ns-map short]
     (or (and ns-map
              (get ns-map short))
         (and kb
              (get (ns-map-to-long kb) short)))))


(defn sym-to-long-name 
  ([sym] (sym-to-long-name *kb* *ns-map-to-long* sym))
  ([kb sym] (sym-to-long-name kb *ns-map-to-long* sym))
  ([kb ns-map sym] 
     (str (or (short-ns-to-long-ns kb ns-map (namespace sym))
              (namespace sym))
          (name sym))))

;; (defn find-maximal-namespace [namespaces uri-str]
;;   (infer-longest-ns kb uri-str))


;;TODO update to take a KB parameter
;;  and find out where it's called because the args may need updating
(defn convert-string-to-sym
  ([full-name] (convert-string-to-sym *kb* full-name))
  ([kb full-name] (infer-longest-ns kb full-name))
  ([kb full-name preferred-namespace preferred-local]
     (or
      (let [preferred-short (long-ns-to-short-ns kb preferred-namespace)]
        (and preferred-short
             (attempt-long-name-to-sym full-name [preferred-namespace 
                                                  preferred-short])))
      (convert-string-to-sym kb full-name))))


;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn anon? [s]
  (and (symbol? s)
       (or (not (namespace s))
           ;;(= "_" (namespace s))))
           (= *anon-ns-name* (namespace s)))))

;; this doesn't really test literal it tests non-symbol
(defn literal? [x]
  (not (= (type x) clojure.lang.Symbol)))


(defn anon [a]
  (cond
   (string? a) (anon (symbol a))
   ;;(anon? a) a
   (symbol? a) (symbol *anon-ns-name* (name a))))

;;; --------------------------------------------------------
;;; creating Resources/Properties from clj data
;;; --------------------------------------------------------
  
;; (defn anon-str-to-id-str [anon-str]
;;   (subs anon-str (.length *anon-name*)))

;;annon needs to dispatch on the kb type
;; (defn resource-ify-sym [s]
;;   (if (anon? s)
;;     (blank-node (anon-str-to-id-str (name s)))
;;     (str (or (get *ns-map-to-long* (namespace s)) (namespace s))
;;          (name s))))

;;TODO annon needs to dispatch on the kb type ??
;;TODO ERROR this returns a string or a bnode - not right
(defn resource-ify-sym [kb s]
  (if (anon? s)
    (blank-node kb (name s))  ;(anon-str-to-id-str (name s)))
    (sym-to-long-name kb s)))


;;(defmulti resource-ify type)
(defmulti resource-ify (fn [kb thing] (type thing)))

(defmethod resource-ify clojure.lang.Symbol [kb s] (resource-ify-sym kb s))
(defmethod resource-ify String [_ s] s)
(defmethod resource-ify :default [_ s] (str s))

;;; creating resources from clj data
;;; --------------------------------------------------------

;; (defmulti create-resource #'server-type-dispatch)
;; (defmethod create-resource :default [kb & _] 
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))

(defn resource
  ([x] (resource *kb* x))
  ([kb x] (if (anon? x)
            (blank-node kb (name x))
            (create-resource kb (resource-ify kb x)))))

;;; creating properties from clj data
;;; --------------------------------------------------------

;; (defmulti create-property #'server-type-dispatch)
;; (defmethod create-property :default [kb & _] 
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))

(defn property
  ([x] (property *kb* x))
  ([kb x] (create-property kb (resource-ify kb x))))


;;; creating blank-nodes from clj data
;;; --------------------------------------------------------
;; (defmulti create-blank-node #'server-type-dispatch)
;; (defmethod create-blank-node :default [kb & _] 
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))


;;; creating literals from clj data
;;; --------------------------------------------------------

;; (defmulti create-literal #'server-type-dispatch)
;; (defmethod create-literal :default [kb & _] 
;;   (throw (IllegalArgumentException. (str "Unknown KB Type: " kb))))

(defn literal
  ([x] (literal *kb* x))
  ([kb x] (cond
           ;;not what you want
           (not *infer-literal-type*) (create-literal kb (str x))

           ;; integers are getting coerced wrong - force it
           ;; integer? tests for all integer types (int, long, bigint,..)
           (integer? x) (create-literal kb (str x) 'xsd/integer)

           ;;boxed value - just use it
           (sequential? x) (apply create-literal kb (str (first x)) (rest x))
           ;; (and (sequential? x)
           ;;      (rest x))

           ;;special strings
           ;;(and (= java.lang.String (type x))
           (and (string? x)
                *use-default-language*
                *string-literal-language*)
           (create-literal kb x *string-literal-language*)

           ;;everything else including strings when there is no default lang
           :else (create-literal kb x))))


(defn object 
  ([x] (object *kb* x))
  ([kb x] (if (= (type x) clojure.lang.Symbol)
           (resource kb x)
           (literal kb x))))


;;; creating properties from clj data
;;; --------------------------------------------------------

(defn statement
  ([[s p o]] (statement *kb* s p o))
  ([kb [s p o]] (statement kb s p o))
  ([s p o] (statement *kb* s p o))
  ([kb s p o] (create-statement kb
                                (resource kb s)
                                (property kb p)
                                (object kb o))))

(defn blank-node
  ([] (blank-node *kb* *anon-name*))
  ([kb] (create-blank-node kb *anon-name*))
  ([kb name] (create-blank-node kb name)))

(defn unique-node
  ([ns] (unique-node ns "" ""))
  ([ns prefix] (unique-node ns prefix ""))
  ([ns prefix suffix] 
     (symbol ns (str prefix (UUID/randomUUID) suffix))))



;;; --------------------------------------------------------
;;; helping
;;; --------------------------------------------------------

;; (defn statement-parts [s]
;;   (list (.getSubject s)
;;         (.getPredicate s)
;;         (.getObject s)))

(defn bnode
  ([] (bnode *kb*))
  ([kb] (clj-ify kb (blank-node kb))))

;;; reify
;;; --------------------------------------------------------

;; (S, P, O), reified by resource R, is represented by:
;; R rdf:type rdf:Statement
;; R rdf:subject S
;; R rdf:predicate P
;; R rdf:object O

;; would this function better with the Jena native .createReifiedStatement
;;   or .getAnyReifiedStatement ?
;; yes because the .getAnyReifiedStatement prevents over reification
;;   but sometimes that is what is wanted?

;; (defn reify-in-model
;;   "gets existing reification if available otherwise reifies"

(defn reify-as
  ([[s p o] r] (reify-as s p o r))
  ([s p o r]
  `((~r rdf/type rdf/Statement)
    (~r rdf/subject ~s)
    (~r rdf/predicate ~p)
    (~r rdf/object ~o))))

(defn reify!
  "forces new reification"
  ([s] (reify! s (bnode)))
  ([[s p o] r]   
     `((~r rdf:type rdf:Statement)
       (~r rdf:subject ~s)
       (~r rdf:predicate ~p)
       (~r rdf:object ~o))))


(defn reify-query-statement
  ([[s p o]] (reify-query-statement s p o))
  ([s p o] (reify-as s p o (temp-variable))))


;;; --------------------------------------------------------
;;; querying
;;; --------------------------------------------------------


;;; --------------------------------------------------------
;;; adding
;;; --------------------------------------------------------

;;TODO checked add doesn't work with bnodes?
(defn checked-add [kb s p o g]
  (if (not (ask-statement kb s p o g))
    (add-statement kb s p o g)
    nil))



(defn add
  "adds a statement to the triple store
   if it doesn't already exist
   returns the model if it goes in, nil if it's already there"
  ([quad] (add *kb* quad))
  ([kb quad] (let [[s p o g] quad]
               (if (= 4 (count quad))
                 (add kb s p o g)
                 (add kb s p o *graph*))))
  ([s p o] (add *kb* s p o))
  ([kb s p o] (checked-add kb s p o *graph*))
  ([kb s p o g] (checked-add kb s p o g)))

(defn add!
  "adds a statement to the triple store
   if it doesn't already exist
   returns the model if it goes in, nil if it's already there"
  ([quad] (add! *kb* quad))
  ([kb quad] (let [[s p o g] quad]
               (if (= 4 (count quad))
                 (add! kb s p o g)
                 (add! kb s p o *graph*))))
  ([s p o] (add! *kb* s p o))
  ([kb s p o] (add-statement kb s p o *graph*))
  ([kb s p o g] (add-statement kb s p o g)))


;;pays attention to *graph*
(defn load-rdf 
  ([kb source]
     (if (instance? File source)
       (load-rdf-file kb source)
       (load-rdf-stream kb source)))
  ([kb source type]
     (if (instance? File source)
       (load-rdf-file kb source type)
       (load-rdf-stream kb source type))))



;;; --------------------------------------------------------
;;; other interactions
;;; --------------------------------------------------------

(defn ask-rdf
  "retrieves statements matching pattern
   nil in a slot matches anything"
  ([quad] (ask-rdf *kb* quad))
  ([kb quad] (let [[s p o g] quad]
               (if (= 4 (count quad))
                 (ask-statement kb s p o g)
                 (ask-statement kb s p o *graph*))))
  ([s p o] (ask-statement *kb* s p o *graph*))
  ([kb s p o] (ask-statement kb s p o *graph*))
  ([kb s p o g] (ask-statement kb s p o g)))


(defn query-rdf
  "retrieves statements matching pattern
   nil in a slot matches anything"
  ([quad] (query-rdf *kb* quad))
  ([kb quad] (let [[s p o g] quad]
               (if (= 4 (count quad))
                 (query-statement kb s p o g)
                 (query-statement kb s p o *graph*))))
  ([s p o] (query-statement *kb* s p o *graph*))
  ([kb s p o] (query-statement kb s p o *graph*))
  ([kb s p o g] (query-statement kb s p o g)))
;;                                  (and s (resource kb s))
;;                                  (and p (property kb p))
;;                                  (and o (object kb o)))

;; (list-statements kb s p o g)))


;;; --------------------------------------------------------
;;; copy / cloning 
;;; --------------------------------------------------------

(defn copy-rdf-slots [target-kb source-kb]
  (initialize-ns-mappings target-kb source-kb))

;;; --------------------------------------------------------
;;; end
;;; --------------------------------------------------------

