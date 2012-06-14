(ns edu.ucdenver.ccp.test.kr.sesame.test-kb
  (use clojure.test
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.sesame.kb
       edu.ucdenver.ccp.kr.sesame.writer-kb)
  (require edu.ucdenver.ccp.test.kr.test-kb)
  (import java.io.ByteArrayOutputStream))

;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

(defn sesame-memory-test-kb []
  (edu.ucdenver.ccp.kr.kb/open
   (edu.ucdenver.ccp.kr.sesame.kb/new-sesame-memory-kb)))

(defn test-ns-hook []
  (binding [edu.ucdenver.ccp.test.kr.test-kb/*kb-creator-fn*
            sesame-memory-test-kb]
    (run-tests 'edu.ucdenver.ccp.test.kr.test-kb)))


;;run the kb tests for the writer-kb too
(defn sesame-writer-test-kb []
  (new-sesame-writer-kb (ByteArrayOutputStream.)))

(defn test-ns-hook []
  (binding [edu.ucdenver.ccp.test.kr.test-kb/*kb-creator-fn*
            sesame-writer-test-kb]
    (run-tests 'edu.ucdenver.ccp.test.kr.test-kb)))

;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
