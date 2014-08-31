(ns edu.ucdenver.ccp.kr.clj-ify
   (use edu.ucdenver.ccp.kr.kb))

;;; --------------------------------------------------------
;;; clj-ify - convert data to clj structures
;;; --------------------------------------------------------

;(declare clj-ify)

(defmulti clj-ify (fn [kb obj]
                    ;;(println (type obj))
                    (class obj)))
                    ;;(type obj)))

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
;;; string-literal
;;; --------------------------------------------------------

(defmulti string-ify-literal (fn [kb obj]
                               (type obj)))

(defmethod string-ify-literal :default [kb x] 
  (str x))
;;(throw (IllegalArgumentException. (str "Unknown Type for: " x))))


;;; --------------------------------------------------------
;;; clj-ify-literal
;;; --------------------------------------------------------

;;takes values:
;; nil or :clj for converting to clj objects
;; :clj-type to return a vector of [clj-object type-or-language]
;; :string to return a vector of [string-serialization type-or-language]
;; :native to return a vector of [native-literal type-or-language]
;; function that takes [clj-object type-or-language] and
;;    returns one of the above values

(def ^:dynamic *literal-mode* nil)

;; (defn literal-mode [literal type-or-langauge]
;;   (cond (nil? *literal-mode*) nil
;;         (keyword? *literal-mode*) *literal-mode*
;;         :else (*literal-mode* literal type-or-language)))

;; (defn clj-ify-literal [literal type-or-langauge]
;;   (case (literal-mode literal type-or-langauge)
;;     nil       (clj-ify literal)
;;     :clj      (clj-ify literal)
;;     :clj-type (vector (clj-ify literal) type-or-language)
;;     :string   (vector (string-ify-literal literal) type-or-language)
;;     :native   (vector literal type-or-language)))

(defn nil-symbol-string? [mode]
  (or (nil? mode)
      (keyword? mode)
      (symbol? mode)
      (string? mode)))

(defn type-or-language-value [kb literal type-or-language]
  (if (nil-symbol-string? type-or-language)
    type-or-language
    (type-or-language kb literal)))
  ;; (if (function? type-or-language)
  ;;   (type-or-langague kb literal)
  ;;   type-or-language))

(defn clj-ify-literal-key [kb mode literal to-value-fn to-string-fn type-or-language]
  (case mode
    nil       (to-value-fn kb literal)
    :clj      (to-value-fn kb literal)
    :clj-type (vector (to-value-fn kb literal) 
                      (type-or-language-value kb literal type-or-language))
    :string   (vector (to-string-fn kb literal)
                      (type-or-language-value kb literal type-or-language))
    :native   (vector literal
                      (type-or-language-value kb literal type-or-language))))


(defn clj-ify-literal [kb literal to-value-fn to-string-fn type-or-language-fn]
  (if (nil-symbol-string? *literal-mode*)
    (clj-ify-literal-key kb *literal-mode* literal 
                         to-value-fn to-string-fn type-or-language-fn)
    (let [type-or-language (type-or-language-fn kb literal)
          mode (*literal-mode* literal type-or-language)]
      (clj-ify-literal-key kb mode literal 
                           to-value-fn to-string-fn type-or-language))))




  ;; (cond (nil? *literal-mode*) (to-value-fn literal)
  ;;   nil       (clj-ify literal)
  ;;   :clj      (clj-ify literal)
  ;;   :clj-type (vector (clj-ify literal) type-or-language)
  ;;   :string   (vector (string-ify-literal literal) type-or-language)
  ;;   :native   (vector literal type-or-language)))


;;; --------------------------------------------------------
;;; end
;;; --------------------------------------------------------

