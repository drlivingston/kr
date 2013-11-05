(ns edu.ucdenver.ccp.test.kr.test-sparql-property-paths
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

(def test-triples-property-paths
     '((ex/a    foaf/firstname   "Alice" )
       (ex/a    foaf/surname "Hacker" )
       (ex/a    foaf/age          [40 xsd/integer])

       (ex/b    foaf/firstname   "Bob" )
       (ex/b    foaf/surname     "Hacker" )
       (ex/b    foaf/age          40) ;the default should be xsd/integer

       (ex/c    foaf/firstname   "Fred" )
       (ex/c    foaf/surname     "Hacker" )
       (ex/c    foaf/age          [50 xsd/integer])

       (ex/a foaf/knows ex/b)
       (ex/b foaf/knows ex/c)
       ))


(kb-test test-basic-symbol-pattern test-triples-property-paths
  (is (= 1
         (count 
          (query '((ex/a foaf/knows ?/person))))))
  (is (= 2
         (count 
          (query '((ex/a [foaf/knows +] ?/person))))))
  (is (= 3 ;can bind to self
         (count 
          (query '((ex/a [foaf/knows *] ?/person))))))
  (is (= 2 ;can bind to self
         (count 
          (query '((ex/a [foaf/knows ?] ?/person)))))))

(kb-test test-basic-sequence-pattern test-triples-property-paths
  (is (= 1
         (count 
          (query '((ex/a (foaf/knows foaf/age) ?/age))))))

  (is (= 2
         (count 
          (query '((ex/a ([foaf/knows +] foaf/age) ?/age))))))
  
  (is (= 3 ;; alice knows alice, alice knows bob, bob knows himself
         (count 
          (query '((?/person ([foaf/knows *] foaf/age) 40))))))

  (is ;; alice knows alice, alice knows bob,
   (ask '((ex/a ([foaf/knows *] foaf/age) 40))))

  (is ;; alice knows bob,
   (ask '((ex/a ([foaf/knows +] foaf/age) 40))))

  (is (= 1
         (count 
          (query '((?/person ([foaf/knows +] foaf/age) 40)))))))


;; (kb-test test-construct-visit-pattern test-triples-6-1
;;   (let [results (atom '())]
;;     (construct-visit (fn [[s p o :as triple]]
;;                        (is (= 3 (count triple)))
;;                        (is (= 'rdf/type p))
;;                        (is (= 'ex/Human o))
;;                        (swap! results conj triple))
;;                      '((?/person rdf/type ex/Human))
;;                      '((?/person rdf/type foaf/Person)))
;;     (is (= 2 (count @results)))))


;; (kb-test test-construct-visit-literals test-triples-6-1
;;   (let [results (atom '())]
;;     (construct-visit (fn [[s p o :as triple]]
;;                        (is (= 3 (count triple)))
;;                        (is (= 'foaf/age p))
;;                        (is (= 40 o))
;;                        (swap! results conj triple))
;;                      '((?/person foaf/age 40))
;;                      '((?/person rdf/type foaf/Person)))
;;     (is (= 2 (count @results)))))


;; (kb-test test-construct-visit-literals-both-sides test-triples-numbers-equality
;;   (let [results (atom '())]
;;     (construct-visit (fn [[s p o :as triple]]
;;                        (is (= 3 (count triple)))
;;                        (is (= 'ex/age p))
;;                        (is (or (= 40 o)
;;                                (= 50 o)))
;;                        (swap! results conj triple))
;;                      '((?/person ex/age ?/age))
;;                      '((?/person foaf/surname ?/name)
;;                        (?/person foaf/age ?/age)))
;;     (is (= 3 (count @results)))))
