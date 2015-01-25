(ns extended-lisp-reader.core
  (:require [clojure.java.io :as io]
            [extended-lisp-reader.parsing :as parsing]
            [instaparse.core :as insta]
            [instaparse.cfg :as cfg]
            [instaparse.reduction :as reduction]
            [instaparse.gll :as gll]
            [instaparse.failure :as instaf]))

(defn grammar-for [grammar-id]
  (let [r (str grammar-id ".bnf")]
    (or (io/resource r)
        (throw (RuntimeException. (format "Grammar file '%s' not found." r))))))

(defn insta-parser-for [grammar-id]
  (let [grammar (grammar-for grammar-id)
        parser (insta/parser grammar :string-ci true :total true)]
    parser))

(defn build-parser [spec]
  (let [rules (gll/parse cfg/cfg :rules spec false)]
    (if (instance? instaparse.gll.Failure rules) nil
      (let [productions (map cfg/build-rule rules)
            start-production (first (first productions))]
        {:grammar (cfg/check-grammar (reduction/apply-standard-reductions :hiccup (into {} productions)))
         :start-production start-production
         :output-format :hiccup}))))

(def cfg-parser :todo)

;; Konsumiert aus dem Reader eine instaparse Grammatik
;; und liefert einen parser.
(defn insta-parser-for! [a-reader]
  (parsing/parse! cfg-parser a-reader))

(defn embeded-dsl-reader [a-reader dispatch-char]
  (let [fn-sym (clojure.lang.LispReader/read a-reader true nil true)
        a-ns (.deref clojure.lang.RT/CURRENT_NS)
        _ (.println System/out (str "*ns* == " a-ns "  fn-sym = " fn-sym))
        fn-var (or (ns-resolve a-ns fn-sym)
                  (throw (RuntimeException. (format "Could not resolve symbol %s (namespace is '%s')" fn-sym a-ns))))
        consuming-fn (or @fn-var
                         (throw (RuntimeException. (format "Symbol %s resolved to var %s which is nil." fn-sym fn-var))))
        _ (.println System/out (format "sym = %s  ns = %s  a-var = %s  fn = %s" fn-sym *ns* fn-var consuming-fn))
        ]
    (consuming-fn a-reader)))

(defn install-embeded-dsl-reader []
  (let [lisp-reader-dispatch-macros
        (.get
         (doto (.getDeclaredField clojure.lang.LispReader "dispatchMacros")
           (.setAccessible true))
         nil)]
    (aset lisp-reader-dispatch-macros \[ embeded-dsl-reader)))

(install-embeded-dsl-reader)

