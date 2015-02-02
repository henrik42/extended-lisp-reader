(ns extended-lisp-reader.instaparse-adapter
  (:require [clojure.java.io :as io]
            [extended-lisp-reader.stream-parser :as stream-parser]
            [instaparse.core :as insta]
            [instaparse.cfg :as cfg]
            [instaparse.reduction :as reduction]
            [instaparse.gll :as gll]))

(defn parse [parser text]
  "Internal parser function generator. Can be used to generate parser
  function for ```extended-lisp-reader.stream-parser/parse!``` via
  ```partial```.

  Applies the instaparse parser to the text. If the parser returns an
  ```instaparse.failure/failure?``` nil is returned. Otherwise the
  parser's result (AST) is returned.

  Note that the parser should use ```:total true``` in order to be
  able to return an ```instaparse.failure/failure?``` and not throw an
  exception when a complete successfull parse is not possible."
  (let [ast (parser text)
        res (when-not (insta/failure? ast) ast)]
    res))

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
        parser (insta/parser grammar :string-ci true :total true)]
    (partial parse parser)))

(defn cfg-parser-for [spec-string]
  "A parser function that parses instaparse grammars and can be passed
  to ```extended-lisp-reader.stream-parser/parse!``` (i.e. it returns
  nil if it cannot find a parse).

  Note: if a parse can be found but ```check-grammar``` fails, this
  function throws the exception from ```check-grammar```. This is
  consistent with ```extended-lisp-reader.stream-parser/parse!```
  since trying to parse an even *longer head of the input text* will
  not possibly yield any sucessfull parse.

  It returns a parser function which parses the specified language.

  In contrast to ```parser-for``` this returned parser function does
  not return the AST of the parsed input but -- again -- **a parser
  function** that can be passed to
  ```extended-lisp-reader.stream-parser/parse!```."
  ;; Original code taken from instaparse/build-parser
  (let [rules (gll/parse cfg/cfg :rules spec-string false)]
    (if (instance? instaparse.gll.Failure rules) nil
        (let [productions (map cfg/build-rule rules)
              start-production (first (first productions))
              parser (insta/map->Parser
                      {:grammar (cfg/check-grammar
                                 (reduction/apply-standard-reductions
                                  :hiccup
                                  (into {} productions)))
                       :start-production start-production
                       :output-format :hiccup})]
          (partial parse parser)))))

;; insta/cfg-parser-for is a parser that parses instaparse grammar
;; expressions. You can use it like:
;;
;; (cfg-parser-for "s = 'a'* 'b'*")
;;
;; This will return nil if a parse cannot be found (i.e. if the
;; grammar is ill-formed). Otherwise it returns **a parser** that
;; parses the given grammar.
;;
;; Note: if the grammar is *syntactically correct* but sematically not
;; (e.g. you have a symbol only on the right hand side of a grammar
;; rule but not on the left side), an exception is thrown. See doc for
;; ```extended-lisp-reader.instaparse-adapter/cfg-parser-for``` for
;; details.
;;
;; So cfg-parser-for is a **parser generator**. You can apply
;; such a *generated parser* like:
;;
;; ((cfg-parser-for "s = 'a'* 'b'*") "ab")
;;
;; Again, if you want to use this kind of generated parser to parse
;; embedded instaparse grammars you have to wrap it with (partial
;; stream-parser/parse!)
;;
;; So #[cfg-parser! s = 'a'* 'b'*] returns a parser that parses the specified
;; language.

;; cfg-parser!: <fn: <reader> -> <fn: <text> -> <AST>>>
(def cfg-parser! (partial stream-parser/parse! cfg-parser-for))
;; insta-cfg!: <fn: <reader> -> <fn: <reader> -> <AST>>>
(def insta-cfg! #(partial stream-parser/parse! (cfg-parser! %)))

;;((cfg-parser! (java.io.StringReader. "s = 'a'* 'b'*]...")) "aa") ; -> [:s "a" "a"]

;; ab2: <fn: <reader> -> <AST>>
;;(def ab2 (partial stream-parser/parse! #[cfg-parser! s = 'a'* 'b'*]))
;;(def ab2 #[insta-cfg! s = 'a'* 'b'*])
