(ns edu.ucdenver.ccp.kr.rule-index
  (use edu.ucdenver.ccp.kr.unify
       edu.ucdenver.ccp.kr.assertion
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql))
       
;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------


;; rule standard keys:
;; :name
;; :head
;; :body
;; :direction
;; :preferred-reasoner

;;; --------------------------------------------------------
;;; constants
;;; --------------------------------------------------------

;;(def ^:dynamic *rule-ns* "http://kabob.ucdenver.edu/kr/rule/")
(def ^:dynamic *rule-ns* "rule")
(def ^:dynamic *var-ns* "var")

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn rule-sym [name]
  (symbol *rule-ns* name))

;;; --------------------------------------------------------
;;; rule indexing
;;; --------------------------------------------------------

;;this should probably be rolled into clj-ify ...
(defn var-to-ns-var [var]
  (symbol *var-ns* (name var)))

;; (defn uri-to-var [uri]
;;   (symbol 

(defn var-map [{head :head
                body :body
                :as rule}]
  (let [vars (variables (concat head body))]
    (reduce (fn [hash v]
              (conj hash [v (var-to-ns-var v)]))
            {}
            vars)))

(defn var-triples [var-map]
  (map (fn [v]
         `(~v rdf/type var/Variable))
       (map second var-map)))

(defn reify-rule-triples [prefix triples]
  (map (fn [triple count]
         (let [stmt-sym (rule-sym (str prefix count))]
           [stmt-sym
            (reify-as triple stmt-sym)]))
       triples
       (range)))

(defn head-triples [rule-sym
                    {name :name
                     head :head 
                     :as rule}]
  (mapcat (fn [[stmt triples]]
            (conj triples
                  `(~rule-sym rule/hasHeadTriple ~stmt)))
          (reify-rule-triples (str name "-head-") head)))

(defn body-triples [rule-sym
                    {name :name
                     body :body 
                     :as rule}]
  (mapcat (fn [[stmt triples]]
            (conj triples
                  `(~rule-sym rule/hasBodyTriple ~stmt)))
          (reify-rule-triples (str name "-body-") body)))

(defn rule-base-triples [rule-sym
                         {name :name
                          :as rule}]
  `((~rule-sym rdf/type rule/Rule)
    (~rule-sym rdfs/label ~name)))
    
(defn index-triples [{name :name
                     head :head
                     body :body 
                     :as rule}]
  (let [rule-sym (rule-sym name)
        var-map (var-map rule)]
    ;[rule-sym
     (concat (rule-base-triples rule-sym rule)
             (var-triples var-map)
             (head-triples rule-sym
                           (conj rule [:head (subst-bindings head var-map)]))
             (body-triples rule-sym 
                           (conj rule [:body (subst-bindings body var-map)])))
     ;]
    ))
  
(defn add-to-rule-index [kb rules]
  (dorun
   (map (fn [rule]
          (dorun
           (map (partial add kb) (index-triples rule))))
        rules)))

;; (defn index-rule? [index {name :name
;;                           head :head 
;;                           body :body
;;                           :as rule}]
;;   )

;;; --------------------------------------------------------
;;; rule lookup
;;; --------------------------------------------------------

;;this looks for rules that could generate more specific triples than
;;  the one specified as the lookup triple
;; (defn triple-re-derive-query-body [[s p o :as triple]]
;;   `((?rule rdf/type rule/Rule)
;;     (?rule rule/hasHeadTriple ?head)
;;     ~@(and (not (variable? s))
;;            `((?head rdf/subject ?sub)
;;              (:union ~(list '= '?sub s);(= ?sub ~s))
;;                      ((?sub rdf/type var/Variable))
;;                      ((?sub rdf/type ~s))
;;                      ((?sub rdfs/subClassOf ~s)))))
;;                      ;; ((~s rdf/type ?sub))
;;                      ;; ((~s rdfs/subClassOf ?sub))
;;     ~@(and (not (variable? p))
;;            `((?head rdf/predicate ?pred)
;;              ;;(:union ~(list '= '?pred p);(= ?pred ~p))
;;              (:union ~(list :sameTerm '?pred p);(= ?pred ~p))
;;                      ((?pred rdf/type var/Variable))
;;                      ((?pred rdfs/subPropertyOf ~p)))))
;;     ~@(and (not (variable? o))
;;            `((?head rdf/object ?obj)
;;              (:union ~(list '= '?obj o);(= ?obj ~o))
;;                      ((?obj rdf/type var/Variable))
;;                      ((?obj rdf/type ~o))
;;                      ((?obj rdfs/subClassOf ~o)))))))

(defn triple-re-derive-query-body [[s p o :as triple]]
  `((?rule rdf/type rule/Rule)
    (?rule rule/hasHeadTriple ?head)
    ~@(and (not (variable? s))
           `((?head rdf/subject ?sub)
             (:union ((?head rdf/subject ?sub)
                      (:sameTerm ?sub ~s))
                     ((?sub rdf/type var/Variable))
                     ((?sub rdf/type ~s))
                     ((?sub rdfs/subClassOf ~s)))))
                     ;; ((~s rdf/type ?sub))
                     ;; ((~s rdfs/subClassOf ?sub))
    ~@(and (not (variable? p))
           `((?head rdf/predicate ?pred)
             (:union ((?head rdf/predicate ?pred)
                      (:sameTerm ?pred ~p))
                     ((?pred rdf/type var/Variable))
                     ((?pred rdfs/subPropertyOf ~p)))))
    ~@(and (not (variable? o))
           `((?head rdf/object ?obj)
             (:union ((?head rdf/object ?obj)
                      (:sameTerm ?obj ~o))
                     ((?obj rdf/type var/Variable))
                     ((?obj rdf/type ~o))
                     ((?obj rdfs/subClassOf ~o)))))))


;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


