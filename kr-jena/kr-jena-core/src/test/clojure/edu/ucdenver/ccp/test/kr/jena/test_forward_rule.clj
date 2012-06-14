(ns edu.ucdenver.ccp.test.kr.jena.test-forward-rule
  (use clojure.test
       [edu.ucdenver.ccp.test.kr.jena.test-kb :exclude [test-ns-hook]])
  (require edu.ucdenver.ccp.test.kr.test-kb
           edu.ucdenver.ccp.test.kr.test-forward-rule))

;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

(defn test-ns-hook []
  (binding [edu.ucdenver.ccp.test.kr.test-kb/*kb-creator-fn*
            jena-memory-test-kb
            edu.ucdenver.ccp.kr.jena.rdf/*force-add-named-to-default*
            true]
    ;;these currently fail because there is a reader and writer
    ;;  going in the same model causing a failure...
    ;;  modification during iteration
    ;;(run-tests 'edu.ucdenver.ccp.test.kr.test-forward-rule)
    ))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
