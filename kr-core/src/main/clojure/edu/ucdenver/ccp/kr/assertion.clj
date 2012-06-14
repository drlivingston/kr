(ns edu.ucdenver.ccp.kr.assertion
  (use [clojure.set :only (union)]
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.reify
       edu.ucdenver.ccp.kr.unify
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql))
       
;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------


;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; identify more specific types
;;; --------------------------------------------------------

;;TODO:
;; this is justing type instead of subClassOf?
;;  also need to worry about subClassOf loops...

(defn specific-type? [kb child parent]
  (ask kb `((~child rdf/type ~parent))))  

(def mem-specific-type? (memoize specific-type?))

(defn spec-of?
  ([child parent] (spec-of? *kb* child parent))
  ([kb child parent] (mem-specific-type? kb child parent)))

;;; --------------------------------------------------------
;;; reduce type sets
;;; --------------------------------------------------------

(defn remove-parent-types 
  ([kb child parents] (remove-parent-types kb child parents false))
  ([kb child parents return-set altered]
     (cond
      (not (seq parents)) 
        [return-set altered]
      (spec-of? kb child (first parents))
        (recur kb child (rest parents) return-set true)
      :else 
        (recur kb child (rest parents) 
               (conj return-set (first parents)) 
               altered))))

(defn seperate-parent-child-neither 
  ([kb type types] (seperate-parent-child-neither kb type types #{} #{} #{}))
  ([kb type types parent child neither]
     (cond
      (not (seq types)) 
        [parent child neither]
      (= type (first type)) ;skip it
        (recur kb type (rest types) parent child neither)
      (spec-of? kb type (first types)) ;parent
        (recur kb type (rest types) (conj parent (first types)) child neither)
      (spec-of? kb (first types) type) ;child
        (recur kb type (rest types) parent (conj child (first types)) neither)
      :else
        (recur kb type (rest types) 
               parent child (conj neither (first types))))))

(defn most-specific-types 
  ([kb types] (most-specific-types kb types #{}))
  ([kb types return-types]
     (if (not (seq types))
       return-types
       (let [[parent child neither] 
             (seperate-parent-child-neither (first types) return-types)]
         (if (seq child) ; there's already more specific ones in there
           (recur kb (rest types) (union child neither)) ; cut this one
           ;;else child is nil, who cares about parents, add to neither set
           (recur kb (rest types) (conj neither (first types))))))))

;;; --------------------------------------------------------
;;; infer types
;;; --------------------------------------------------------

(defn infer-subject-types-from-pred [kb [s p o]]
  (query-template kb '?type `((~p rdfs/domain ?type))))

(defn infer-subject-types-from-obj [kb [s p o]]
  (if (= p 'rdf/type)
    (list o)
    '()))

(defn infer-sym-type [kb sym assertion]
  (cond
   (= sym (first assertion))
     (concat (infer-subject-types-from-pred kb assertion)
             (infer-subject-types-from-obj kb assertion))
   ; if pred
   ; if object
   :else '())) ; it's not in this assertion


(defn infer-types
  ([sym assertions] (infer-types *kb* sym assertions))
  ([kb sym assertions]
     (most-specific-types (reduce concat 
                                  (map (partial infer-sym-type kb sym)
                                       assertions)))))

;;; --------------------------------------------------------
;;; assertion cluster
;;; --------------------------------------------------------

(defn assertion-keys [[sub pred obj :as assertion]]
  (list sub obj))

;;list of assertion clusters in
;; returns list of list of assertion clusters that match and those that don't
(defn matching-clusters [clusters keys]
  (let [key-set (set keys)]
    (reduce (fn [[match no-match] cluster]
              (if (some (partial some key-set) (map assertion-keys cluster))
                [(conj match cluster) no-match]
                [match (conj no-match cluster)]))
            [() ()]
            clusters)))
    

;;list of assertions in - list of lists of assertions out
(defn cluster-assertions [assertions]
  (reduce (fn [clusters assertion]
            (let [[matching rest-clusters]
                  (matching-clusters clusters (assertion-keys assertion))]
              (conj rest-clusters
                    (conj (apply concat matching)
                          assertion))))
          '()
          assertions))

(defn disjoint-assertions? [assertions]
  (< 1 (count (cluster-assertions assertions))))

;;; --------------------------------------------------------
;;; reification
;;; --------------------------------------------------------


(defn reify-assertions [kb assertions]
  (let [vars (variables assertions)
        var-instance-map 
        (reduce conj {}
                (map (fn [var]
                       [var (reify-instance kb 
                                            (infer-types kb var assertions))])
                     vars))]
    (subst-bindings assertions var-instance-map)))



;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


