(ns extended-lisp-reader.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [instaparse.core :as insta]
            [instaparse.failure :as instaf]))

(defn parse [parser text]
  (let [ast (parser text)
        res (when-not (insta/failure? ast) ast)]
    (.println System/out (format "Parsing %s bytes %s returns '%s'" (.length text) text res))
    res))

(defn parse! [parser a-reader]
  (let [sb (StringBuilder.)]
    (loop []
      (let [c (.read a-reader) ;; consume reader!
            ;; EOF is a FAIL
            _ (when (= -1 c) (throw (RuntimeException. (format "Cannot parse '%s'" sb))))
            c (char c)
            sb (.append sb c)] ;; build-up head
        ;;(.println System/out (format "char = '%s'  head = '%s'" c (str sb)))
        (if-not (= c \]) (recur) ;; try parse on "]" only; DSL should have balanced "[...]"
                (if-let [p (parse parser (.substring sb 0 (dec (.length sb))))] p ;; try parse and return on success
                        (recur))))))) ;; else keep going

(defn grammar-for [grammar-id]
  (let [r (str grammar-id ".bnf")]
    (or (io/resource r)
        (throw (RuntimeException. (format "Grammar file '%s' not found." r))))))

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

