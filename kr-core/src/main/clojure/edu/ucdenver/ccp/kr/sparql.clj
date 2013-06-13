(ns edu.ucdenver.ccp.kr.sparql
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.unify)
  (import java.util.Map)) ;is this actaully used?

;;; --------------------------------------------------------
;;; constants and variables
;;; --------------------------------------------------------

;;; constants

(def select-default "")
(def select-distinct "DISTINCT ")
(def select-reduced "REDUCED ")

(def sparql-1-0 :sparql-1-0)
(def sparql-1-1 :sparql-1-1)

;;; dynamic variables to control function

(defonce ^:dynamic *select-type* select-default)

(defonce ^:dynamic *select-limit* nil)

(defonce ^:dynamic *count-var-name* "sp11countvar")

(defonce ^:dynamic *use-query-optimization* true)

(defonce ^:dynamic *force-prefixes* nil)

;;; --------------------------------------------------------
;;; protocol
;;; --------------------------------------------------------

(defprotocol sparqlKB
  (ask-pattern [kb pattern] [kb pattern options]
    "boolean asks for a pattern in the kb")
  (query-pattern [kb pattern] [kb pattern options] "gets bindings for pattern")
  (count-pattern [kb pattern] [kb pattern options])
  (visit-pattern [kb visitor pattern] [kb visitor pattern options])

  (construct-pattern [kb create-pattern pattern]
                     [kb create-pattern pattern options]
                     "gets bindings for pattern")
  (construct-visit-pattern [kb visitor create-pattern pattern]
                           [kb visitor create-pattern pattern options])

  (ask-sparql [kb sparql-string] "boolean asks a sparql query")
  (query-sparql [kb sparql-string] "gets bindings for sparql query")
  (count-sparql [kb sparql-string])
  (visit-sparql [kb visitor sparql-string]
    "calls visitor function on bindings for side effects")
  (construct-sparql [kb sparql-string])
  (construct-visit-sparql [kb visitor sparql-string]))


;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

;; shouldn't these be calling the RDF based ones?

(defn find-mapping-from-short [short]
  (or (find *ns-map-to-long* short)
      (find (and *kb* (ns-map-to-long *kb*)) short)))

(defn sym-to-sparql [s]
  (cond
   (variable? s) (str "?" (name s))
   (anon? s) (str "_:" (name s))
   :else (str "<" (sym-to-long-name s) ">")))

(defn sparql-str-lang [s l]
  (str (pr-str s) "@" l))

;; it's possible this could be done faster with a call to rdf/object
;;   and then re-serializing that
(defn item-to-sparql [s]
  (cond
   ;; symbol
   (symbol? s) (sym-to-sparql s)
   ;; no inference allowd
   (not *infer-literal-type*) (pr-str s)
   ;; boxed
   (sequential? s) (let [[x type] s]
                     (cond
                      ;; boxed no type
                      (nil? type) (pr-str (str x)) ; why is a non-string boxed?
                      ;; lang tagged
                      (and (string? x)
                           (string? type)) (sparql-str-lang x type)
                      ;; typed
                      :else (str (pr-str (str x)) "^^" 
                                 (str (sym-to-sparql type)))))
   ;; plain string default language
   (and (string? s) 
        *use-default-language*
        *string-literal-language*) (sparql-str-lang s *string-literal-language*)
   ;;plain literal including plain string 
   :else (pr-str s)))
  


;;; --------------------------------------------------------
;;; query text
;;; --------------------------------------------------------

;;; query component construction
;;; --------------------------------------------------------

(declare sparql-query-body)

(defn get-prefixes-from-namespaces [namespaces]
  (keep find-mapping-from-short namespaces)) ;keep = (remove nil? (map

(defn prefix-block [prefixes]
  (apply str (map (fn [[short long]]
                    (str "PREFIX " short ": <" long "> \n"))
                  (concat *force-prefixes*
                          prefixes))))

(defn sparql-statement [statement]
  (let [[s p o g] (map item-to-sparql statement)]
    (str (if g
           (str " GRAPH " g " { ")
           "")
         " " s " " p " " o " "
         (if g
           " } "
           ". "))))

(defn union-body [union-clauses]
  (str " { "
       (apply str (interpose " \n} UNION {\n "
                             (map sparql-query-body union-clauses)))
       " }\n "))

(defn optional-body [optional-clauses]
  (str " OPTIONAL { "
       (sparql-query-body optional-clauses)
       " }\n "))

;; (def operator-bound [args]
;;      (str " bound(" (filter-body (first args))  ") "))

(declare operator-body)

(defn unary-operator [op-name]
  (fn [args]
    (str " " op-name "(" (operator-body (first args))  ") ")))

(defn binary-operator [op-name]
  (fn [args]
    (str " ( " (operator-body (first args)) " " op-name
         " " (operator-body (second args)) " ) ")))

(defn binary-prefix-operator [op-name]
  (fn [args]
    (str " " op-name "(" (operator-body (first args)) ", "
         (operator-body (second args)) ") ")))

(defn n-ary-operator [op-name]
  (fn [args]
    (str " ( "
         (apply str (interpose (str " \n " op-name " ")
                               (map operator-body args)))
         " ) \n ")))


(defn not-operator [args]
  (str "!" (operator-body (first args))))

(defn regex-operator [args]
  (str " regex(" (operator-body(first args))
       ", " (operator-body(second args))
       (if (first (rest (rest args)))
         (str ", " (operator-body (first (rest (rest args)))) ")")
         ") ")))

(def sparql-operators
     {:bound (unary-operator "bound")
      :isIRI (unary-operator "isIRI")
      :isURI (unary-operator "isIRI")
      :isBlank (unary-operator "isBlank")
      :isLiteral (unary-operator "isLiteral")
      :str (unary-operator "str")
      :lang (unary-operator "lang")
      :datatype (unary-operator "datatype")
      ;;:isBLANK (unary-operator "isBlank")
      ;;:isLITERAL (unary-operator "isLiteral")

      ;; use these:
      ;; :or (binary-operator "||")
      ;; :and (binary-operator "&&")
      :or (n-ary-operator "||")
      :and (n-ary-operator "&&")
      :not not-operator
      ;; not these:
      '|| (n-ary-operator "||")
      '&& (n-ary-operator "&&")
      '! not-operator

      '= (binary-operator "=")
      '!= (binary-operator "!=")
      '< (binary-operator "<")
      '> (binary-operator ">")
      '<= (binary-operator "<=")
      '>= (binary-operator ">=")
      '* (binary-operator "*")
      '/ (binary-operator "/")
      '+ (binary-operator "+")
      '- (binary-operator "-")

      :sameTerm (binary-prefix-operator "sameTerm")
      :langMatches (binary-prefix-operator "langMatches")

      :regex regex-operator})
      
(defn sparql-operator? [op]
  (get sparql-operators op))

(defn operator-body [operator-expression]
  ;;(if (not (seq? operator-expression))
  (if (not (sequential? operator-expression))
    ;;(str operator-expression)
    (item-to-sparql operator-expression)
    (let [op (get sparql-operators (first operator-expression))]
      (if op
        (op (rest operator-expression))
        (item-to-sparql operator-expression)))))
        ;;(str operator-expression)))))

(defn filter-body [filter-clause]
  (str " FILTER ( " (operator-body filter-clause) ") "))


(defn sparql-query-body [triple-pattern]
  (cond 
   ;; (not (seq? triple-pattern)) ""
   ;; (seq? (first triple-pattern))
   (not (sequential? triple-pattern)) ""
   (sequential? (first triple-pattern))
     (apply str (interleave (map sparql-query-body triple-pattern)
                            ;;(repeat ". \n")))
                            (repeat " \n")))
   (sparql-operator? (first triple-pattern)) (filter-body triple-pattern)
   (= :union (first triple-pattern)) (union-body (rest triple-pattern))
   (= :optional (first triple-pattern)) (optional-body (rest triple-pattern))
   :default (sparql-statement triple-pattern)))

;;; full query bodies
;;; --------------------------------------------------------

(defn sparql-ask-query [triple-pattern & [options]]
     (let [vars (variables triple-pattern)
           non-vars (symbols-no-vars triple-pattern)
           namespaces (distinct (map namespace non-vars))
           prefixes (get-prefixes-from-namespaces namespaces)]
       (str
        (prefix-block prefixes)
        "ASK { "
        (sparql-query-body triple-pattern)
        "}")))

(defn sparql-select-query [triple-pattern & [options]]
  (let [vars (or (and options
                      (:select-vars options))
                 (variables triple-pattern))
        non-vars (symbols-no-vars triple-pattern)
        namespaces (distinct (map namespace non-vars))
        prefixes (get-prefixes-from-namespaces namespaces)]
    (str
     (prefix-block prefixes)
     (apply str "SELECT " *select-type*
            (interleave (map sym-to-sparql vars) (repeat " ")))
     "\n"
     "WHERE { "
     (sparql-query-body triple-pattern)
     "}"
     (if *select-limit*
       (str " LIMIT " *select-limit* " ")
       "")
     )))


(defn sparql-construct-query [create-pattern triple-pattern & [options]]
  (let [;;vars (or (and options
        ;;              (:select-vars options))
        ;;         (variables triple-pattern))
        non-vars (symbols-no-vars triple-pattern)
        namespaces (distinct (map namespace non-vars))
        prefixes (get-prefixes-from-namespaces namespaces)]
    (str
     (prefix-block prefixes)
     "CONSTRUCT { "
     (sparql-query-body create-pattern)
     "}"
     "\n"
     "WHERE { "
     (sparql-query-body triple-pattern)
     "}"
     (if *select-limit*
       (str " LIMIT " *select-limit* " ")
       "")
     )))

(defn unique-count-var-internal [var-names-to-avoid candidate-name]
  ;;note contains? only works on sets because of stupid design decisions
  (if (not (contains? var-names-to-avoid candidate-name))
    (variable candidate-name)
    (recur var-names-to-avoid (str candidate-name *count-var-name*))))

(defn unique-count-var [vars-to-avoid]
  (unique-count-var-internal (set (map name vars-to-avoid))
                             *count-var-name*))

(defn sparql-1-1-count-query [triple-pattern & [options]]
  (let [vars (or (and options
                      (:select-vars options))
                 (variables triple-pattern))
        count-var (unique-count-var vars)
        non-vars (symbols-no-vars triple-pattern)
        namespaces (distinct (map namespace non-vars))
        prefixes (get-prefixes-from-namespaces namespaces)]
    (str
     (prefix-block prefixes)
     "SELECT (COUNT (*) AS " (sym-to-sparql count-var) ")"
     "\n"
     "WHERE { "
     (sparql-query-body triple-pattern)
     "}"
     (if *select-limit*
       (str " LIMIT " *select-limit* " ")
       "")
     )))

;;; --------------------------------------------------------
;;; --------------------------------------------------------
;;; query helpers
;;; --------------------------------------------------------
;;; --------------------------------------------------------

(defn ask
  ([pattern] (ask *kb* pattern))
  ([kb pattern & [options]] (binding [*kb* kb]
                              (ask-pattern *kb* pattern options))))

(defn query
  ([pattern] (query-pattern *kb* pattern))
  ([kb pattern & [options]] (binding [*kb* kb]
                              (query-pattern *kb* pattern options))))

(defn query-template
  ([result-template pattern]
     (query-template *kb* result-template pattern))
  ([kb result-template pattern & [options]]
     (binding [*kb* kb]
       (map (fn [result]
              (subst-bindings result-template result))
            (query kb pattern
                   (assoc options :select-vars (variables result-template)))))))

(defn query-visit
  ([visitor pattern] (query-visit *kb* visitor pattern))
  ([kb visitor pattern & [options]]
     (binding [*kb* kb]
       (visit-pattern kb visitor pattern options))))


(defn count-query
;;(defn query-count
  ([pattern] (count-query *kb* pattern))
  ([kb pattern & [options]] (binding [*kb* kb]
                              (count-pattern kb pattern options))))

(def query-count count-query)


(defn construct
  ([create-pattern pattern]
     (construct-pattern *kb* create-pattern pattern))
  ([kb create-pattern pattern & [options]]
     (binding [*kb* kb]
       (construct-pattern *kb* create-pattern pattern options))))

(defn construct-visit
  ([visitor create-pattern pattern]
     (construct-visit-pattern *kb* visitor create-pattern pattern))
  ([kb visitor create-pattern pattern & [options]]
     (binding [*kb* kb]
       (construct-visit-pattern *kb*
                                visitor
                                create-pattern
                                pattern
                                options))))



(defn sparql-ask
  ([sparql-string] (ask *kb* sparql-string))
  ([kb sparql-string] (binding [*kb* kb]
                        (ask-sparql kb sparql-string))))

(defn sparql-query
  ([sparql-string] (query *kb* sparql-string))
  ([kb sparql-string] (binding [*kb* kb]
                        (query-sparql kb sparql-string))))

(defn sparql-query-template
  ([result-template sparql-string] 
     (query-template *kb* result-template sparql-string))
  ([kb result-template sparql-string]
     (binding [*kb* kb]
       (map (fn [result]
              (subst-bindings result-template result))
            (query-sparql kb sparql-string)))))

(defn sparql-count-query
  ([sparql-string] (count-query *kb* sparql-string))
  ([kb sparql-string] (binding [*kb* kb]
                        (count-sparql kb sparql-string))))

(defn sparql-visit
  ([visitor sparql-string] (sparql-visit *kb* visitor sparql-string))
  ([kb visitor sparql-string]
     (binding [*kb* kb]
       (visit-sparql kb visitor sparql-string))))


(defn sparql-construct
  ([sparql-string]
     (construct-sparql *kb* sparql-string))
  ([kb sparql-string]
     (binding [*kb* kb]
       (construct-sparql *kb* sparql-string))))

(defn sparql-construct-visit
  ([visitor sparql-string]
     (construct-visit-sparql *kb* visitor sparql-string))
  ([kb visitor sparql-string]
     (binding [*kb* kb]
       (construct-visit-sparql *kb* visitor sparql-string))))


;;; --------------------------------------------------------
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; these are some crazy helpers that need to be revisited
;;; --------------------------------------------------------

(defn pmap-query [kb fcn l]
  (pmap (fn [x num]
          (time 
           (with-new-connection kb conn
             (println num)
             (query conn (fcn x)))))
        l
        (range)))

(defn pmap-count [kb fcn l]
  (pmap (fn [x num]
          (time 
           (with-new-connection kb conn
             (println num)
             (ask conn (fcn x)))))
        l
        (range)))


(defn pmap-some [kb fcn list-list]
  (pmap (fn [l num]
          (with-new-connection kb conn
            (if (= 0 (mod num 100))
              (println num))
            (some (fn [x] (ask conn (fcn x)))
                  l)))
        list-list
        (range)))



;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


