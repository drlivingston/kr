(ns edu.ucdenver.ccp.kr.unify
  (:use edu.ucdenver.ccp.kr.variable
        edu.ucdenver.ccp.utils
        clojure.walk)
  ;;(:require clojure.core.unify))
  )

;;; based on Norvig PAIP unifier

(declare unify unify-variable occurs-check)

(def fail nil)
(def no-bindings {})

(def ^:dynamic *occurs-check* true) ;"perform the occurs check?"

(defn extend-bindings [var val bindings]
  (assoc bindings var val))

(defn lookup [var bindings]
  (get bindings var))

(defn has-binding? [var bindings]
  (contains? bindings var))

;;; ==============================

(defn unify
  "See if x and y match with given bindings."
  ([x y] (unify x y no-bindings))
  ([x y bindings]
     (cond
      (= bindings fail) fail
      (= x y) bindings
      (variable? x) (unify-variable x y bindings)
      (variable? y) (unify-variable y x bindings)
      (and (nonempty-seq x) (nonempty-seq y))
        (unify (next x) (next y)
               (unify (first x) (first y) bindings))
      true fail)))

(defn unify-variable [var x bindings]
  "Unify var with x, using (and maybe extending) bindings."
  (cond (contains? bindings var)
          (unify (lookup var bindings) x bindings)
        (and (variable? x) (has-binding? bindings x))
          (unify var (lookup x bindings) bindings)
        (and *occurs-check* (occurs-check var x bindings))
          fail
        :else (extend-bindings var x bindings)))

(defn occurs-check [var x bindings]
  "Does var occur anywhere inside x?"
  (cond (= var x) true
        (and (variable? x) (has-binding? bindings x))
          (occurs-check var (lookup x bindings) bindings)
        (nonempty-seq x)
          (or (occurs-check var (first x) bindings)
              (occurs-check var (next x) bindings))
        :else false))

;;; ==============================

;; (def unify-occurs (make-occurs-unify-fn variable?))
;; (def unify-no-occurs (make-unify-fn variable?))
;; (def unify-occurs
;;   (partial #'clojure.core.unify/garner-unifiers
;;            clojure.core.unify/unify-variable
;;            variable?))
;; (def unify-no-occurs
;;   (partial #'clojure.core.unify/garner-unifiers
;;            clojure.core.unify/unify-variable-
;;            variable?))

;; (defn unify
;;   ([x y] (unify x y {}))
;;   ([x y binds]  (if *occurs-check*
;;                   (unify-occurs x y binds)
;;                   (unify-no-occurs x y binds))))


;;; ==============================


(defn recursive-replace 
  ([expr l-bindings] (recursive-replace expr l-bindings #{expr}))
  ([expr l-bindings past-vals]
     (let [applicable-bindings
           (some (fn [b] (if (contains? b expr) b nil)) l-bindings)
           bind-val (get applicable-bindings expr)]
       (if (or (not applicable-bindings)
               (contains? past-vals bind-val)) ;don't enter replace loop
         expr
         (recur bind-val l-bindings (conj past-vals bind-val))))))

(defn subst-bindings-int [x l-bindings]
  (prewalk (fn [expr] (recursive-replace expr l-bindings)) x))

(defn subst-bindings-list [x list-of-bindings]
  (cond (some (partial = fail) list-of-bindings) fail
        (every? (partial = no-bindings) list-of-bindings) x
        :else (subst-bindings-int x list-of-bindings)))

(defn subst-bindings [x & bindings]
  "Substitute the value of variables in bindings into x,
  taking recursively bound variables into account."
  (subst-bindings-list x bindings))

;;; ==============================

(defn unifier [x y]
 "Return something that unifies with both x and y (or fail)."
 (subst-bindings x (unify x y)))