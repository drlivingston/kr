(ns edu.ucdenver.ccp.utils
  (use [clojure.java.io :exclude (resource)]
       clojure.java.classpath)
  (import java.io.InputStreamReader
          (java.util.jar JarFile JarEntry)
          java.security.MessageDigest
          java.math.BigInteger))
;;org.apache.commons.codec.binary.Base64


(def ^:dynamic *default-file-encoding* "UTF-8")

(def ^:dynamic *dynamic-source-fn* (fn [source]
                                     (.endsWith (str source) ".clj")))


(defmacro time-multiple-value
  "Evaluates expr and returns vector [result time]."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [ret# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]))


(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn md5
  ([x] (.toString
        (BigInteger. 1
                     (.digest (doto (MessageDigest/getInstance "MD5")
                                (.reset)
                                (.update (.getBytes (str x) "UTF-8")))))
        16))
  ([x & rest] (md5 (apply str x rest))))

;;need a set of flags to control encoding:
;; hex, 32, 36, 64 (+/- url safe)
;;

;; (defn sha-1
;;   ([x] (Base64/encodeBase64URLSafeString
;;         ^bytes (.digest (doto (MessageDigest/getInstance "SHA1")
;;                           (.reset)
;;                           (.update (.getBytes (str x) "UTF-8"))))))
;;   ([x & rest] (sha-1 (apply str x rest))))



(defn read-eval-with-sentinel [reader]
  (let [val (read reader nil reader)]
    (if (= val reader)
      reader
      (eval val))))

;;(declare read-eval-with-sentinel)
(def ^:dynamic *dynamic-reader-fn* read-eval-with-sentinel)


(defn nonempty-seq [x]
  "returns x as a seq if it's a non-empty seq otherwise nil/false"
  (and (coll? x) (seq x)))


(defmacro defn-special-binding [fn-name default-specials args & body]
  (let [special-vars (vec (doall (map gensym default-specials)))]
    `(defn ~fn-name
       (~args ~@body)
       (~(vec (concat special-vars args))
        (binding ~(vec (interleave default-specials special-vars))
          ~@body)))))

;; (defn classpath-seq []
;;   (cond
;;    (jar-file? cp-part) (filenames-in-jar (JarFile. cp-part))
;;    ;;call recursively?
;;    (.isDirectory cp-part) (let [children (.listFiles cp-part)]
;;                             (concat children
;;                                     (mapcat sub-directories!
;;                                             children)))
;;    (nil? cp-part) nil
;;    :else (list cp-part)))

;; (defn directory-seq [dir]
;;   (lazy-seq
;;    (when-let [children (seq (.listFiles dir))]
;;      (cons dir (mapcat directory-seq children)))))

(defn directory-seq
  ([dir]
     (lazy-seq
      (let [children (seq (.listFiles (file dir)))]
        (concat children (mapcat directory-seq children)))))
  ([dir filter-re]
     (remove (fn [f]
               (not (re-find filter-re (str f))))
             (directory-seq dir))))
  


(defn classpath-seq []
  (distinct
   (mapcat (fn [cp-part]
             (cond
              ;;(or
               (jar-file? cp-part)
               ;; (let [f (file cp-part)]
               ;;   (and (.isFile f)
               ;;        (.endsWith (.getName f) "jar"))))
                (map (partial str "@")
                     (filenames-in-jar (JarFile. cp-part)))
              (.isDirectory cp-part) (cons cp-part
                                           (directory-seq cp-part))
              (nil? cp-part) nil
              :else (list cp-part)))
           (classpath))))


(defn classpath-matching [path-part]
  (remove (fn [file]
            (not (.contains (str file) path-part)))
          (classpath-seq)))


(defn resource-reader [resource-name]
  (let [path (if (.startsWith resource-name "@")
               (.substring resource-name
                           (if (.startsWith resource-name "@/") 2 1))
               resource-name)]
    (InputStreamReader. 
     (ClassLoader/getSystemResourceAsStream path) 
     *default-file-encoding*)))

(defn reader!
  "makes a reader one way or the other out of it's input.
   if it's a string that starts with @ then it's assumed a resource on CP."
  [input]
  (if (and (string? input)
           (.startsWith input "@"))
    (resource-reader input)
    (reader input)))

(defn read-all-input [source]
  (with-open [r (clojure.lang.LineNumberingPushbackReader. (reader! source))]
    (loop [start (.getLineNumber r)
           form (read r nil r)
           results nil]
      (if (= form r) ; sentinal value stream can't read itself
        results      ; return results
        (let [end (.getLineNumber r)]
          (recur end
                 (read r nil r)
                 (conj results (with-meta form
                                 ;; str-ify source to prevent holding pointer
                                 {:source (str source) 
                                  :start-line start
                                  :end-line end}))))))))

;; (defn read-eval-with-sentinel [reader]
;;   (let [val (read reader nil reader)]
;;     (if (= val reader)
;;       reader
;;       (eval val))))

;; (defn read-eval-all-input [source dynamic-reader-fn]
;;   (with-open [r (clojure.lang.LineNumberingPushbackReader. (reader! source))]
;;     (loop [start (.getLineNumber r)
;;            form (read-eval-with-sentinel r)
;;            results nil]
;;       (if (= form r) ; sentinal value stream can't read itself
;;         results      ; return results
;;         (let [end (.getLineNumber r)]
;;           (recur end
;;                  (read-eval-with-sentinel r)
;;                  (conj results (with-meta form
;;                                  ;; str-ify source to prevent holding pointer
;;                                  {:source (str source) 
;;                                   :start-line start
;;                                   :end-line end}))))))))

(defn read-dynamic-all-input [source dynamic-reader-fn]
  (with-open [r (clojure.lang.LineNumberingPushbackReader. (reader! source))]
    (loop [start (.getLineNumber r)
           form (dynamic-reader-fn r)
           results nil]
      (if (= form r) ; sentinal value stream can't read itself
        results      ; return results
        (let [end (.getLineNumber r)]
          (recur end
                 (dynamic-reader-fn r)
                 (conj results (with-meta form
                                 ;; str-ify source to prevent holding pointer
                                 {:source (str source) 
                                  :start-line start
                                  :end-line end}))))))))

(defn all-input
  ([source] (all-input source *dynamic-reader-fn*))
  ([source modified-dynamic-reader-fn]
     (if (*dynamic-source-fn* source)
       (read-dynamic-all-input source modified-dynamic-reader-fn)
       (read-all-input source))))
