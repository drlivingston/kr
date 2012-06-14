(ns edu.ucdenver.ccp.kr.reify
  (use edu.ucdenver.ccp.utils
       edu.ucdenver.ccp.kr.variable
       edu.ucdenver.ccp.kr.rdf))
       
;;; --------------------------------------------------------
;;; 
;;; --------------------------------------------------------

;;; --------------------------------------------------------
;;; specials
;;; --------------------------------------------------------

(def ^:dynamic *reify-ns* "ex") ; "http://www.example.com/")
(def ^:dynamic *reify-prefix* "G_")
(def ^:dynamic *reify-suffix* "")

(def ^:dynamic *name_separator* "_")

;;; --------------------------------------------------------
;;; helpers
;;; --------------------------------------------------------

(defn reify-local-name [name]
  (str *reify-prefix* name *reify-suffix*))

(defn reify-anon [name]
  (anon (reify-local-name name)))

;;why would you ever use this?
(defn reify-var [name]
  (variable (reify-local-name name)))

(defn reify-sym
  ([name] (symbol *reify-ns* (reify-local-name name)))
  ([ns name] (symbol ns (reify-local-name name)))
  ([ns name prefix] (reify-sym ns name prefix *reify-suffix*))
  ([ns name prefix suffix] 
     (binding [*reify-prefix* prefix
               *reify-suffix* suffix]
       (symbol ns (reify-local-name name)))))

;;; --------------------------------------------------------
;;; anon with type based mnemonics
;;; --------------------------------------------------------

;; should probably use a mnemonic from the types, but isn't this exactly
;;  what blank nodes are for??
;;GUIDs would be unique, but might make for long URIs?

;;TODO should this be blank-node or anon??
(defn reify-instance [kb types]
  (blank-node kb (or (name (first types)) *anon-name*)))


;;; --------------------------------------------------------
;;; unique
;;; --------------------------------------------------------

;; parrot a local name into another ns
(defn reify-unique []
  (reify-sym (uuid)))

;;; --------------------------------------------------------
;;; localname
;;; --------------------------------------------------------

;; parrot a local name into another ns
(defn reify-localname [& syms]
  (reify-sym (apply str (interpose *name_separator* (map name syms)))))

;;; --------------------------------------------------------
;;; md5
;;; --------------------------------------------------------

;;md5 a set of items to produce a sym
(defn reify-md5 [& rest]
  (reify-sym (md5 (apply str rest))))


;;; --------------------------------------------------------
;;; regex
;;; --------------------------------------------------------

;;md5 a set of items to produce a sym
(defn reify-regex [match replace & rest]
  (reify-sym (.replaceAll (apply str rest) match replace)))

;;; --------------------------------------------------------
;;; fn
;;; --------------------------------------------------------

;;apply a function to a set of bindings to create a sym
;; (defn reify-fn [fn bindings]
;;   (reify-sym (fn bindings)))


;;; --------------------------------------------------------
;;; END
;;; --------------------------------------------------------


