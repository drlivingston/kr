(ns edu.ucdenver.ccp.kr.clj-ify
   (use edu.ucdenver.ccp.kr.kb))

;;; --------------------------------------------------------
;;; clj-ify - convert data to clj structures
;;; --------------------------------------------------------

;(declare clj-ify)

(defmulti clj-ify (fn [kb obj]
                    ;;(println (type obj))
                    (type obj)))

(defn clj
  ([x] (clj-ify *kb* x))
  ([kb x] (clj-ify kb x)))

;;TODO clj-ify should be made universally to be a 2 argument funtion
;; with one one-argument helper that just calls the 2-arg one with *kb*

;; collections of clj-ifiable things
;; should this be made to capture *kb*? if there is a 2 arg clj-ify
(defmethod clj-ify clojure.lang.Seqable [kb s] 
  (map (partial clj-ify kb) s))

(defmethod clj-ify :default [kb x] 
   (throw (IllegalArgumentException. (str "Unknown Type for: " x))))

;; the above was a fail-fast catch-all for attempting to clj-ify a type
;;   that can't be clj-ified (i.e. has no definition yet) BUT then all
;;   the native types would need a clj-ify definition that is effectively
;;   the identity function (symbols, strings, numbers, etc.)
;;   replacing the default with identity saves implementing that but
;;   passes the buck on identifying in a fail-fast manner things that
;;   don't have a clj-ify implementation yet.

;; (defmethod clj-ify :default [x] 
;;   x)

(defmethod clj-ify java.lang.Number [kb n] n)
(defmethod clj-ify java.lang.String [kb str] str)
(defmethod clj-ify java.lang.Boolean [kb bool] bool)

(defmethod clj-ify clojure.lang.Symbol [kb sym] sym)

;;; --------------------------------------------------------
;;; end
;;; --------------------------------------------------------

