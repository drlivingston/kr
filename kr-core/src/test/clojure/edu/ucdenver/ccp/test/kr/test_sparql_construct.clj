(ns edu.ucdenver.ccp.test.kr.test-sparql-construct
  (use clojure.test
       edu.ucdenver.ccp.test.kr.test-kb
       edu.ucdenver.ccp.test.kr.test-sparql

       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql))



(kb-test test-construct-pattern test-triples-6-1
  (is (= 2
         (count 
          (construct '((?/person rdf/type ex/Human))
                     '((?/person rdf/type foaf/Person))))))
  (is (= 4
         (count 
          (construct '((?/person rdf/type ex/Human)
                       (?/person ex/live-on ex/Earth))
                     '((?/person rdf/type foaf/Person)))))))


(kb-test test-construct-visit-pattern test-triples-6-1
  (let [results (atom '())]
    (construct-visit (fn [[s p o :as triple]]
                       (is (= 3 (count triple)))
                       (is (= 'rdf/type p))
                       (is (= 'ex/Human o))
                       (swap! results conj triple))
                     '((?/person rdf/type ex/Human))
                     '((?/person rdf/type foaf/Person)))
    (is (= 2 (count @results)))))
