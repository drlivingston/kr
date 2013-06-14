(ns edu.ucdenver.ccp.kr.examples.jena-mem-kb
  (use edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql)
  (require edu.ucdenver.ccp.kr.jena.kb))

;;; --------------------------------------------------------
;;; create kb
;;; --------------------------------------------------------

(defn jena-memory-test-kb []
  (open (kb :jena-mem)))


(defn add-namespaces [kb]
  (register-namespaces kb
                       '(("ex" "http://www.example.org/") 
                         ("rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                         ("rdfs" "http://www.w3.org/2000/01/rdf-schema#")
                         ("owl" "http://www.w3.org/2002/07/owl#")
                         ("foaf" "http://xmlns.com/foaf/0.1/"))))

;;a couple of ways to add triples
(defn add-triples [kb]
  ;;in parts
  (add kb 'ex/KevinL 'rdf/type 'ex/Person)

  ;;as a triple
  (add kb '(ex/KevinL foaf/name "Kevin Livingston"))

  ;;to the 'default' kb
  (binding [*kb* kb]
    (add '(ex/KevinL foaf/mbox "<mailto:kevin@example.org>")))
  
  ;;multiple triples
  (add-statements kb
                  '((ex/BobL rdf/type ex/Person)
                    (ex/BobL foaf/name "Bob Livingston")
                    (ex/BobL foaf/mbox "<mailto:bob@example.org>"))))


;;; --------------------------------------------------------
;;; rdf api
;;; --------------------------------------------------------

;;rdf api ask
(defn ask-person [kb]
  (ask-rdf kb nil nil 'ex/Person))

;;rdf api query
(defn query-person [kb]
  (query-rdf kb nil nil 'ex/Person))


;;; --------------------------------------------------------
;;; sparql api
;;; --------------------------------------------------------

;; symbols in the ? ns are assumed to be capturing variables
;; symbols in the _ ns are blank nodes
;;    they function as non-capturing in sparql
(def names-and-email-pattern
  '((_/person rdf/type ex/Person)
    (_/person foaf/name ?/name)
    (_/person foaf/mbox ?/email)))

;;sparql api ask
(defn ask-names-with-email [kb]
  (ask kb names-and-email-pattern))

;;sparql api ask
(defn query-names-with-email [kb]
  (query kb names-and-email-pattern))

(defn visit-people [kb]
  (query-visit kb
               (fn [bindings]
                 (let [name (get bindings '?/name)
                       email (get bindings '?/email)]
                   (println "emailing " name " at: " email)))
               names-and-email-pattern))

;;; --------------------------------------------------------
;;; REPL trace:
;;; --------------------------------------------------------

;; user> (use 'edu.ucdenver.ccp.kr.examples.sesame-mem-kb)
;; nil

;; user> (def my-kb (add-namespaces
;;                   (sesame-memory-test-kb)))
;; #'user/my-kb

;; user> (add-triples my-kb)
;; nil

;; user> (ask-person my-kb)
;; true

;; user> (query-person my-kb)
;; ((ex/KevinL rdf/type ex/Person) (ex/BobL rdf/type ex/Person))


;; user> (ask-names-with-email my-kb)
;; true

;; user> (query-names-with-email my-kb)
;; ({?/name "Kevin Livingston", ?/email "<mailto:kevin@example.org>"} {?/name "Bob Livingston", ?/email "<mailto:bob@example.org>"})

;; user> (visit-people my-kb)
;; emailing  Kevin Livingston  at:  <mailto:kevin@example.org>
;; emailing  Bob Livingston  at:  <mailto:bob@example.org>
;; nil

;; user> (use 'edu.ucdenver.ccp.kr.kb)
;; user> (close my-kb)

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
