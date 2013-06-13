(ns edu.ucdenver.ccp.test.kr.test-sparql-construct
  (use clojure.test
       edu.ucdenver.ccp.test.kr.test-kb
       edu.ucdenver.ccp.test.kr.test-sparql

       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql))



;; (def test-triples-6-1
;;      '((ex/a  rdf/type        foaf/Person )
;;        (ex/a  foaf/name       "Alice" )
;;        (ex/a  foaf/mbox       "<mailto:alice@example.com>" )
;;        (ex/a  foaf/mbox       "<mailto:alice@work.example>" )

;;        (ex/b  rdf/type        foaf/Person )
;;        (ex/b  foaf/name       "Bob" )))

;; (def test-triples-numbers-equality
;;      '((ex/a    foaf/givenname   "Alice" )
;;        (ex/a    foaf/surname "Hacker" )
;;        (ex/a    foaf/age          [40 xsd/integer])

;;        (ex/b    foaf/firstname   "Bob" )
;;        (ex/b    foaf/surname     "Hacker" )
;;        (ex/b    foaf/age          40) ;the default should be xsd/integer

;;        (ex/c    foaf/firstname   "Fred" )
;;        (ex/c    foaf/surname     "Hacker" )
;;        (ex/c    foaf/age          [50 xsd/integer])))


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


(kb-test test-construct-visit-literals test-triples-6-1
  (let [results (atom '())]
    (construct-visit (fn [[s p o :as triple]]
                       (is (= 3 (count triple)))
                       (is (= 'foaf/age p))
                       (is (= 40 o))
                       (swap! results conj triple))
                     '((?/person foaf/age 40))
                     '((?/person rdf/type foaf/Person)))
    (is (= 2 (count @results)))))


(kb-test test-construct-visit-literals-both-sides test-triples-numbers-equality
  (let [results (atom '())]
    (construct-visit (fn [[s p o :as triple]]
                       (is (= 3 (count triple)))
                       (is (= 'ex/age p))
                       (is (or (= 40 o)
                               (= 50 o)))
                       (swap! results conj triple))
                     '((?/person ex/age ?/age))
                     '((?/person foaf/surname ?/name)
                       (?/person foaf/age ?/age)))
    (is (= 3 (count @results)))))
