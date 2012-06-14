(ns edu.ucdenver.ccp.test.kr.jena.test-kb
  (use clojure.test
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.jena.kb)
  (require edu.ucdenver.ccp.test.kr.test-kb
           edu.ucdenver.ccp.kr.jena.rdf))

;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

(defn jena-memory-test-kb []
  ;;(edu.ucdenver.ccp.kr.kb/open
  (kb :jena-mem))
;;(edu.ucdenver.ccp.kr.jena.kb/new-jena-model-kb));)

(defn test-ns-hook []
  (binding [edu.ucdenver.ccp.test.kr.test-kb/*kb-creator-fn*
            jena-memory-test-kb
            edu.ucdenver.ccp.kr.jena.rdf/*force-add-named-to-default*
            true]
    (run-tests 'edu.ucdenver.ccp.test.kr.test-kb)))



;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------



