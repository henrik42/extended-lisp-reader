(ns extended-lisp-reader.parsing
  (:require [clojure.java.io :as io]
            ;;[extended-lisp-reader.core :as core]
            [instaparse.core :as insta]
            [instaparse.cfg :as cfg]
            [instaparse.reduction :as reduction]
            [instaparse.gll :as gll]
            [instaparse.failure :as instaf]))

(defn parse [parser text]
  (let [ast (parser text)
        _ (.println System/out (format "AST is '%s'  parser = %s" ast parser))
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

