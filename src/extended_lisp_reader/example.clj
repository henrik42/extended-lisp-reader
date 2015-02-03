(ns extended-lisp-reader.example
  "Note: you need to :require the namespace
  ```extended-lisp-reader.core```. Otherwise the reader will not
  understand the embedded language forms like #[...] in this file."
  (:require [extended-lisp-reader.core]
            [clojure.java.io :as io]
            [extended-lisp-reader.stream-parser :as stream-parser]
            [instaparse.core :as instaparse]
            [extended-lisp-reader.instaparse-adapter :as insta]))

(defn grammar-for [grammar-id]
  "Internal grammar loading function.

  Loads resource ```<grammar-id>.bnf``` and throws exception if it
  cannot be found."
  (let [r (str grammar-id ".bnf")]
    (or (io/resource r)
        (throw (RuntimeException. (format "Grammar file '%s' not found." r))))))

(defn parser-for [grammar-id]
  "Loads the grammar via ```grammar-for``` and builds an instaparse
  parser for it.

  Returns a parser function based on this parser which can be passed
  to ```extended-lisp-reader.stream-parser/parse!```."
  (let [grammar (grammar-for grammar-id)
        parser (instaparse/parser grammar :string-ci true :total true)]
    (partial insta/parse parser)))

;; (parser-for "sql") builds a parser, that parses input
;; according to resources/sql.bnf and returns nil if a parse cannot be
;; found. Otherwise the instaparse AST is returned.
;;
;; You can apply this parser like:
;;
;; ((parser-for "sql") "select foo.*, bar.*")
;;
;; If you want to use this parser for embedded language forms like
;; #[sql select foo.*, bar.*] you need a function that consumes a
;; reader/stream. You build such a "stream parsing parser" via
;; stream-parser/parse!
(def sql (partial stream-parser/parse! (parser-for "sql")))

;; Das ist ein Parser, wie man ihn hat, wenn man mit instaparse arbeitet.
(def partest-parser (instaparse/parser (io/resource "partest.bnf")))
;;(instaparse/parse partest-parser "[a]d" :total true)

(def partest! (insta/parser-for partest-parser))
;;#[partest! [a]]

;; You can build stream parsing parsers by using (insta/cfg-parser-for
;; <grammar-string>) directly -- i.e. without using an embedded
;; language form but just the string equivalent.
(def ab1 (partial stream-parser/parse! (insta/cfg-parser-for "s = 'a'* 'b'*")))

;; And you can build such a parser with an embedded language form.
(def ab2 (partial stream-parser/parse! #[insta/cfg-parser! s = 'a'* 'b'*]))

;; insta-cfg! includes the (partial .... ) around cfg-parser! so its usage
;; is even shorter
(def ab3 #[insta/insta-cfg! s = 'a'* 'b'*])

;; run this via ```lein run -m extended-lisp-reader.example```
(defn -main []
  (.println System/out (str "SQL1: " ((parser-for "sql") "select foo.*, bar.*")))
  (.println System/out (str "ABs1 " ((insta/cfg-parser-for "s = 'a'* 'b'*") "ab")))
  (.println System/out (str "SQL2: " #[sql select foo.*, bar.*]))
  (.println System/out (str "ABs2: " #[ab1 aabbbb]))
  (.println System/out (str "ABs3: " #[ab2 aabbbb]))
  (.println System/out (str "ABs4: " #[ab3 aabbbb]))
  (.println System/out (str "par1: " #[partest! [a]]))
  )

;; Why does (#[insta/cfg.parser! s = 'a'* 'b'*] "aabb") not work?
;(def ccc #[insta/cfg-parser! s = 'a'* 'b'*])
;(ccc "aabb") ;-> [:s "a" "a" "b" "b"]
;(#[insta/cfg-parser! s = 'a'* 'b'*] "aabb") ;-> java.lang.IllegalArgumentException: No matching ctor found for class clojure.core$partial$fn__4190

