(ns edu.ucdenver.ccp.kr.forward-rule
  (use edu.ucdenver.ccp.utils
       edu.ucdenver.ccp.kr.unify
       edu.ucdenver.ccp.kr.assertion
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.reify
       edu.ucdenver.ccp.kr.kb
       edu.ucdenver.ccp.kr.clj-ify
       edu.ucdenver.ccp.kr.rdf
       edu.ucdenver.ccp.kr.sparql
       edu.ucdenver.ccp.kr.rule
       [clojure.java.io :exclude (resource)]
       clojure.set
       clojure.pprint))
;;  (import java.io.PushbackReader))
       
;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; constants
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn reify-variables [{reify :reify :as rule}]
  (map (fn [entry]
         (if (sequential? entry)
           (first entry)
           entry))
       reify))

;;; --------------------------------------------------------
;;; static rule testing
;;; --------------------------------------------------------

(defn all-head-vars-in-body-sans-reify-vars? [{head :head
                                               body :body
                                               ;reify :reify
                                               :as rule}]
  (let [head-vars (variables head)
        body-vars (variables body)]
    (every? (set body-vars)
            (remove (set (reify-variables rule)) head-vars))))

;;need to add tests - all-reify-vars-in-head
;;  all-head-vars-not-in-body-in-reify

(defn all-reify-vars-in-head? [{head :head :as rule}]
  (let [head-vars (variables head)
        reify-vars (reify-variables rule)]
    (every? (set head-vars) reify-vars)))

(defn all-head-vars-not-in-body-in-reify? [{head :head
                                            body :body
                                            :as rule}]
  (let [head-vars (variables head)
        body-vars (variables body)
        reify-vars (reify-variables rule)]
    (every? (set reify-vars)
            (remove (set body-vars) head-vars))))


(defn forward-safe? [rule]
  (every? (fn [test]
            (test rule))
          (list connected-rule?
                all-head-vars-in-body?)))

(defn forward-safe-with-reification? [rule]
  (every? (fn [test]
            (test rule))
          (list connected-rule?
                all-reify-vars-in-head?
                all-head-vars-not-in-body-in-reify?
                all-head-vars-in-body-sans-reify-vars?)))



;;; --------------------------------------------------------
;;; kb rule testing
;;; --------------------------------------------------------



;;; --------------------------------------------------------
;;; reification
;;; --------------------------------------------------------

;;; reify from rule expression
;;; --------------------------------------------------------

(defmacro with-reify-name-bindings [name-bindings & body]
  `(binding [*reify-ns* (or (:ns ~name-bindings) *reify-ns*)
             *reify-prefix* (or (:prefix ~name-bindings) *reify-prefix*)
             *reify-suffix* (or (:suffix ~name-bindings) *reify-suffix*)]
     ~@body))
  

;;form [var {ns-name :ns
;;           prefix :prefix suffix :suffix
;;           l-name :ln <<one of the following with listed params>>
;;           :unique
;;           :localname   [vars ..]
;;           :md5         [vars ..]
;;           :regex       [match replace vars..]
;;           :fn          (fn [bindings] ..)
;;           :as reify-opts}

;; this will take the above type of form and return a function
;; that will function to refify symbols for that form when passed the bindings

;; (defmulti reify-rule-form-fn (fn [[var {type :type :as reify-opts}]]
;;                                type))
(defmulti reify-rule-form-fn
  (fn [reify-form]
    (cond 
     (not (sequential? reify-form)) :default
     (not (map? (second reify-form))) :default ;is this actually an error?
     (keyword? (:ln (second reify-form))) (:ln (second reify-form))
     :else (first (:ln (second reify-form))))))


(defmethod reify-rule-form-fn :default [[var reify-opts]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (reify-unique))))

(defmethod reify-rule-form-fn :unique [[var reify-opts]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (reify-unique))))
  
(defmethod reify-rule-form-fn :localname [[var {[fn-name & params] :ln
                                                :as reify-opts}]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (apply reify-localname (map bindings params)))))

(defmethod reify-rule-form-fn :md5 [[var {[fn-name & params] :ln
                                          :as reify-opts}]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (apply reify-md5 (map bindings params)))))

(defmethod reify-rule-form-fn :regex [[var {[fn-name match replace & vars] :ln
                                            :as reify-opts}]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (apply reify-regex match replace (map bindings vars)))))


(defmethod reify-rule-form-fn :fn [[var {[fn-name fcn] :ln
                                         :as reify-opts}]]
  (fn [bindings]
    (with-reify-name-bindings reify-opts
      (reify-sym (fcn bindings))))) ;params is a function in this case


(defn default-reify-rule-form-fn []
  (fn [bindings]
    (reify-unique)))


(defn add-reify-fns [{reify :reify :as rule}]
  (assoc rule
    :reify
    (map (fn [entry]
           (if (sequential? entry)
             (let [[var opts :as form] entry]
               ;; (pprint var)
               ;; (pprint opts)
               ;; (pprint form)
               [var (assoc opts
                      :reify-fn
                      (reify-rule-form-fn form))])
               (vector entry {:reify-fn (default-reify-rule-form-fn)})))
           reify)))



;;; --------------------------------------------------------
;;; forward chaining
;;; --------------------------------------------------------

(defn reify-bindings [reify-with-fns bindings]
  (reduce (fn [new-bindings [var {reify-fn :reify-fn}]]
            ;; (pprint new-bindings)
            ;; (pprint var)
            (assoc new-bindings var (reify-fn bindings)))
          {}
          reify-with-fns))

;;instantiates a rule and puts the triples in the target kb
(defn run-forward-rule [source-kb target-kb rule]
  ;; {head :head
  ;;                                            body :body
  ;;                                            reify :reify
  ;;                                            :as rule}]
  (let [{head :head
         body :body
         reify :reify
         :as rule}    (add-reify-fns rule)]
  ;; (let [reify-with-fns (map (fn [[var opts :as form]]
  ;;                             [var (assoc opts
  ;;                                    :reify-fn
  ;;                                    (reify-rule-form-fn form))])
  ;;                           reify)]
    (query-visit source-kb
                 (fn [bindings]
                   ;;(pprint bindings)
                   (dorun
                    (map (partial add! target-kb)
                         ;; (do 
                         ;;  (pprint head)
                         ;;  (pprint bindings)
                         ;;  (pprint (reify-bindings reify ;;-with-fns
                         ;;                          bindings))
                         (doall
                          ;; set this to false temporarily --
                          ;; trying to run with BigData
                          ;; which re-uses the same connection
                          ;; (reify-assertions (connection source-kb false) 
                          ;;                (subst-bindings head bindings))))))
                          ;;don't need to call reify-assertions nothing should
                          ;;  there should be no free variables
                          (subst-bindings head
                                          bindings
                                          (reify-bindings reify ;;-with-fns
                                                          bindings))))));)
                 body ;need to add reify find clauses on optionally
                 {:select-vars (concat (variables head)
                                       (variables reify))})))


(defn ask-forward-rule [source-kb {head :head
                                   body :body
                                   :as rule}]
  (ask source-kb body))

(defn count-forward-rule [source-kb {head :head
                                     body :body
                                     :as rule}]
  (query-count source-kb
               body
               {:select-vars (variables head)}))



  ;; (query-visit source-kb
  ;;              (fn [bindings]
  ;;                (prn bindings)
  ;;                (dorun (map (fn [triple]
  ;;                              (prn triple)
  ;;                              (add! target-kb triple))
  ;;                              ;;(partial add! target-kb)
  ;;                            (dorun (map (fn [triple]
  ;;                                          (prn triple)
  ;;                                          (statement kb triple))
  ;;                                          ;;(partial statement kb)
  ;;                                        (reify-assertions
  ;;                                         (subst-bindings head bindings)))))))
  ;;              body))



;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------



;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


