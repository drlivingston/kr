(ns edu.ucdenver.ccp.test.kr.jena.test-sparql
  (use clojure.test
       [edu.ucdenver.ccp.test.kr.jena.test-kb :exclude [test-ns-hook]])
  (require edu.ucdenver.ccp.test.kr.test-kb
           edu.ucdenver.ccp.test.kr.test-sparql))

;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

(defn test-ns-hook []
  (binding [edu.ucdenver.ccp.test.kr.test-kb/*kb-creator-fn*
            jena-memory-test-kb
            edu.ucdenver.ccp.kr.jena.rdf/*force-add-named-to-default*
            true]
    (run-tests 'edu.ucdenver.ccp.test.kr.test-sparql)
    ))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
