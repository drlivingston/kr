(ns edu.ucdenver.ccp.test.kr.test-rdf
  (use clojure.test
       ;;clojure.test.junit

       edu.ucdenver.ccp.test.kr.test-kb

       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       )
  (import ;java.io.InputStream
          java.io.ByteArrayInputStream)
  )

;;; --------------------------------------------------------
;;; constansts
;;; --------------------------------------------------------

(def ntriple-string 
     (str "<http://www.example.org/a> "
          "<http://www.example.org/p> "
          "<http://www.example.org/x> . \n"
          "<http://www.example.org/a> "
          "<http://www.example.org/p> "
          "<http://www.example.org/y> . \n"))
;;; --------------------------------------------------------
;;; tests
;;; --------------------------------------------------------



(kb-test test-kb-with-triples test-triples
         (is *kb*))

(kb-test test-resources nil
         (is (= "ex" (namespace 'ex/a)))
         (is (= "ex" (namespace (clj-ify *kb*
                                         (resource 'ex/a)))))
         (is (= "http://www.example.org/a"
                (sym-to-long-name *kb* 'ex/a)))
         (is (= 'ex/a
                (convert-string-to-sym *kb* "http://www.example.org/a")))
         (binding [*ns-map-to-short* {"http://www.example.org/" "foo"}]
           (is (= 'foo/a
                  (convert-string-to-sym *kb* "http://www.example.org/a")))))





(kb-test load-stream nil
         (load-rdf *kb* 
                   ;;.getBytes can take a string argument e.g., "UTF-8"
                   (ByteArrayInputStream. (.getBytes ntriple-string))
                   :ntriple)
         (is (ask-rdf *kb* 'ex/a 'ex/p 'ex/x))
         (is (ask-rdf *kb* 'ex/a 'ex/p 'ex/y))
         (is (not (ask-rdf *kb* 'ex/a 'ex/p 'ex/z))))

(kb-test test-blank-nodes nil
         (is (anon? (bnode *kb*)))
         (is (= "_" (namespace (bnode *kb*)))))

(kb-test test-bnode-retrieval nil
         (is (nil? (add! *kb* 'ex/a 'ex/b '_/c)))
         (is (ask-rdf *kb* 'ex/a 'ex/b nil))
         (is (anon? (nth (first (query-rdf *kb* 'ex/a 'ex/b nil)) 2))))
                     

(kb-test test-add-triple nil
         (is (nil? (add *kb* 'ex/a 'ex/b 'ex/c))))

(kb-test test-multiple-ways-to-add nil
         (let [kb *kb*]
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
                             (ex/BobL foaf/mbox "<mailto:bob@example.org>")))))


(kb-test test-one-triple nil
         (is (nil? (add! *kb* 'ex/a 'ex/b 'ex/c)))
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c)))

(kb-test test-ask-triple nil
         (is (nil? (add *kb* 'ex/a 'ex/b 'ex/c)))
         (is (ask-rdf *kb* 'ex/a nil nil))
         (is (ask-rdf *kb* nil 'ex/b nil))
         (is (ask-rdf *kb* 'ex/a 'ex/b nil))
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c))
         (is (ask-rdf *kb* nil nil 'ex/c))

         ;;default graph
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c nil))
         (is (ask-rdf *kb* nil 'ex/b 'ex/c nil))
         (is (ask-rdf *kb* nil nil 'ex/c nil))
         (is (ask-rdf *kb* nil 'ex/b nil nil)))

(kb-test test-ask-graph nil
         (is (nil? (add *kb* 'ex/a 'ex/b 'ex/c 'ex/x)))
         (is (ask-rdf *kb* 'ex/a nil nil))
         (is (ask-rdf *kb* nil 'ex/b nil))
         (is (ask-rdf *kb* 'ex/a 'ex/b nil))
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c))
         (is (ask-rdf *kb* nil nil 'ex/c))

         ;;default graph
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c nil))
         (is (ask-rdf *kb* nil 'ex/b 'ex/c nil))
         (is (ask-rdf *kb* nil nil 'ex/c nil))
         (is (ask-rdf *kb* nil 'ex/b nil nil))

         ;;named graph
         (is (ask-rdf *kb* 'ex/a 'ex/b 'ex/c 'ex/x))
         (is (ask-rdf *kb* nil 'ex/b 'ex/c 'ex/x))
         (is (ask-rdf *kb* nil nil 'ex/c 'ex/x))
         (is (ask-rdf *kb* nil 'ex/b nil 'ex/x))

         ;;wrong graph
         (is (not (ask-rdf *kb* 'ex/a 'ex/b 'ex/c 'ex/z)))
         (is (not (ask-rdf *kb* nil 'ex/b 'ex/c 'ex/z)))
         (is (not (ask-rdf *kb* nil nil 'ex/c 'ex/z)))
         (is (not (ask-rdf *kb* nil 'ex/b nil 'ex/z))))


(kb-test test-query-triple nil
         (is (nil? (add *kb* 'ex/a 'ex/b 'ex/c)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* nil 'ex/b nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b 'ex/c)))

         ;;quads
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a nil nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* nil 'ex/b nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b 'ex/c nil))))

(kb-test test-query-graph nil
         (is (nil? (add *kb* 'ex/a 'ex/b 'ex/c 'ex/x)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* nil 'ex/b nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b 'ex/c)))

         ;;default graph
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a nil nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* nil 'ex/b nil nil)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b 'ex/c nil)))

         ;;named graph
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a nil nil 'ex/x)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b nil 'ex/x)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* nil 'ex/b nil 'ex/x)))
         (is (= '((ex/a ex/b ex/c)) (query-rdf *kb* 'ex/a 'ex/b 'ex/c 'ex/x)))

         ;;wrong graph
         (is (= () (query-rdf *kb* 'ex/a nil nil 'ex/z)))
         (is (= () (query-rdf *kb* 'ex/a 'ex/b nil 'ex/z)))
         (is (= () (query-rdf *kb* nil 'ex/b nil 'ex/z)))
         (is (= () (query-rdf *kb* 'ex/a 'ex/b 'ex/c 'ex/z))))




;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------
