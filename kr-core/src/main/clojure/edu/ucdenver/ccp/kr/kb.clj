(ns edu.ucdenver.ccp.kr.kb
  ;; (use edu.ucdenver.ccp.kr.variable
  ;;      [clojure.contrib.string :only (dochars)])
  )


;;; --------------------------------------------------------
;;; specials and types
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; basic KB protocol
;;; --------------------------------------------------------

(defprotocol KB
  ;;(root [kb] "gets the actual underlying api object")
  (native [kb] "gets the actual underlying native object")
  (initialize [kb] "initializes the kb")
  (open [kb] "establishes a connection to the kb")
  (close [kb] "closes and invalidates the connection")
  (features [kb]))


;;; --------------------------------------------------------
;;; kb creator helper
;;; --------------------------------------------------------

;;(defmulti kb (fn [& args] (and args (first args))))

;; this should either be a class name or a object where it returns it's type

(defmulti kb 
  (fn [& args] 
    (if (and args 
             (first args))
      (let [a1 (first args)]
        (cond
         (keyword? a1) a1
         (class? a1) a1
         :else (type a1))))))

;;; --------------------------------------------------------
;;; default KB instance
;;; --------------------------------------------------------

(defonce ^:dynamic *kb* nil)

(defn set!-kb
  "DONOT USE THIS FUNCTION for testing only.  sets the root *kb* binding."
    [new-kb] 
    (alter-var-root (var *kb*)
                    (fn [orig new-kb] new-kb)
                   new-kb))

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn features [kb]
  (:kb-features kb))

(defn has-feature? [kb feature]
  ;; contains doesn't work on a list, which is completely stupid
  ;; so we must work around it
  ;;(contains? (features kb) feature))
  (some (partial = feature) (features kb)))

(defn add-feature [kb feature & more-features]
  (assoc kb :kb-features (apply conj (features kb) feature more-features)))

;;; --------------------------------------------------------
;;; connection
;;; --------------------------------------------------------

(defn connection 
  ([kb] (or (and (:connection kb) kb)
            (connection kb true)))
  ([kb force-new] (if (or force-new (not (:connection kb)))
                    (open ^KB kb)
                    kb)))

(defmacro with-new-connection [kb conn & body]
  `(let [~conn (connection ~kb true)]
     (try 
      ~@body
      (finally (close ~conn)))))
;;(finally (close (connection ~conn))))))
;;(finally (.close (:connection ~conn))))))

;;; --------------------------------------------------------
;;; copy / cloning
;;; --------------------------------------------------------

(defn copy-kb-slots [target-kb source-kb]
  (assoc target-kb
    :kb-features (features source-kb)))

;;; --------------------------------------------------------
;;; end
;;; --------------------------------------------------------

