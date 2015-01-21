(ns extended-lisp-reader.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [instaparse.core :as insta]
            [instaparse.failure :as instaf]))

;;  (:require [extended-lisp-reader use-dsl define-dsl]))

(.println System/out "(ns extended-lisp-reader.core)")

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
            sb (.append sb c)] ;; build head
        ;;(.println System/out (format "char = '%s'  head = '%s'" c (str sb)))
        (if-not (= c \]) (recur) ;; try parse on "]" only; DSL should have balanced "[...]"
                (if-let [p (parse parser (.substring sb 0 (dec (.length sb))))] p ;; try parse
                        (recur))))))) ;; else keep going

(defn grammar-for [grammar-id]
  (let [r (str grammar-id ".bnf")]
    (or (io/resource r)
        (throw (RuntimeException. (format "Grammarfile '%s' not found." r))))))

(defn embeded-dsl-reader [a-reader dispatch-char]
  (let [grammar-id (clojure.lang.LispReader/read a-reader true nil true)
        grammar (grammar-for grammar-id)
        parser (insta/parser grammar :string-ci true :total true)
        ast (parse! parser a-reader)]
    ast))

(defn install-embeded-dsl-reader! []
  (let [lisp-reader-dispatch-macros
        (.get
         (doto (.getDeclaredField clojure.lang.LispReader "dispatchMacros")
           (.setAccessible true))
         nil)]
    (aset lisp-reader-dispatch-macros \[ embeded-dsl-reader)))

