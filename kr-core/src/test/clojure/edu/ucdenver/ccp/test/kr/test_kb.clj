(ns edu.ucdenver.ccp.test.kr.test-kb
  (use clojure.test
       ;;clojure.test.junit
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       ))

;;; --------------------------------------------------------
;;; constansts
;;; --------------------------------------------------------

(defonce ^:dynamic *rcon* nil)

(def ^:dynamic *namespaces*
     '(;;standard namespaces
       ("rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
       ("rdfs" "http://www.w3.org/2000/01/rdf-schema#")
       ("owl" "http://www.w3.org/2002/07/owl#") 
       ("xsd" "http://www.w3.org/2001/XMLSchema#")
       
       ;;a "regular" namespaces
       ("ex" "http://www.example.org/") 
       ("foaf" "http://xmlns.com/foaf/0.1/")
       ("vcard" "http://www.w3.org/2001/vcard-rdf/3.0#") 
       ("rss" "http://purl.org/rss/1.0/") 
       ("daml" "http://www.daml.org/2001/03/daml+oil#")

       ("dc" "http://purl.org/dc/elements/1.1/")
       ("dc10"  "http://purl.org/dc/elements/1.0/")
       ("dc11"  "http://purl.org/dc/elements/1.1/")

       ("ja" "http://jena.hpl.hp.com/2005/11/Assembler#")

       ))

(def test-triples
     '((ex/a  foaf/name   "Johnny Lee Outlaw")
       (ex/a  foaf/mbox   "<mailto:jlow@example.com>")
       (ex/b  foaf/name   "Peter Goodguy")
       (ex/b  foaf/mbox   "<mailto:peter@example.org>")
       (ex/c  foaf/mbox   "<mailto:carol@example.org>")))

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(def ^:dynamic *kb-creator-fn* nil)

;; (defn register-namespaces [kb]
;;   (synch-ns-mappings kb)
;;   (dorun
;;    (map (fn [[short long]]
;;           (update-ns-mapping kb short long))
;;         *namespaces*))
;;   (synch-ns-mappings kb))

(defn load-test-triples [kb triples]
  (dorun
   (map (fn [stmt] (add! kb stmt)) triples)))

(defn new-test-kb 
  ([triples] (new-test-kb *kb-creator-fn* triples))
  ([kb-creator triples]
     (let [kb (register-namespaces (kb-creator) *namespaces*)]
           (binding [*kb* kb]
             (load-test-triples kb triples))
           kb)))
  ;; ([kb-creator triples] (let [kb (kb-creator)]
  ;;                         (register-namespaces kb *namespaces*)
  ;;                         (binding [*kb* kb]
  ;;                           (load-test-triples kb triples))
  ;;                         kb)))

(defmacro kb-test [name triples & body]
  `(deftest ~name
     (when *kb-creator-fn*
       (binding [*kb* (new-test-kb ~triples)]
         ~@body))))

;; (defmacro kb-test [name triples & body]
;;   `(deftest ~name
;;      (binding [*kb* (new-test-kb ~triples)]
;;        ~@body)))

;;; --------------------------------------------------------
;;; kb tests
;;; --------------------------------------------------------

(kb-test test-kb-up nil
         (is *kb*))

(kb-test test-kb-features nil
         (let [kb (add-feature *kb* :foo)]
           (is (has-feature? kb :foo))
           (is (not (has-feature? kb :bar)))))

(kb-test test-kb-features-preserve nil
         (let [kb (add-feature *kb* :foo)]
           (let [open-kb (open kb)]
             (is (has-feature? open-kb :foo))
             (let [close-kb (close open-kb)]
               (is (has-feature? close-kb :foo))))))

(kb-test test-kb-connection nil
         (let [kb (add-feature *kb* :foo)]
           (let [conn (connection kb true)]
             (is (has-feature? conn :foo)))))

(kb-test test-kb-with-connection nil
         (let [kb (add-feature *kb* :foo)]
           (with-new-connection kb conn
             (is (has-feature? conn :foo)))))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
