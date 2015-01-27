(ns extended-lisp-reader.instaparse
  (:require [clojure.java.io :as io]
            ;;[extended-lisp-reader.parsing :as parsing]
            [instaparse.core :as insta]
            [instaparse.cfg :as cfg]
            [instaparse.reduction :as reduction]
            [instaparse.gll :as gll]
            ;;[instaparse.failure :as instaf]
            ))

(defn parse [parser text]
  "Internal parser function generator. Can be used to generate parser
   function for ```extended-lisp-reader.parsing/parse!``` via
   ```partial```.

   Applies the instaparse parser to the text. If the parser returns an
   ```instaparse.failure/failure?``` nil is returned. Otherwise the
   parser's result (AST) is returned.

   Note that the parser should use ```:total true``` in order to be
   able to return an ```instaparse.failure/failure?``` and not throw
   an exception when a complete successfull parse is not possible.
   "
  (let [ast (parser text)
        ;;_ (.println System/out (format "AST is '%s'  parser = %s" ast parser))
        res (when-not (insta/failure? ast) ast)]
    ;;(.println System/out (format "Parsing %s bytes %s returns '%s'" (.length text) text res))
    res))

(defn grammar-for [grammar-id]
  "Internal grammar loading function.
   Loads resource ```<grammar-id>.bnf``` and throws exception if it
   cannot be found.
  "
  (let [r (str grammar-id ".bnf")]
    (or (io/resource r)
        (throw (RuntimeException. (format "Grammar file '%s' not found." r))))))

(defn parser-for [grammar-id]
  "Loads the grammar via ```grammar-for``` and builds an instaparse
   parser for it.

   Returns a parser function based on this parser which can be passed
   to ```extended-lisp-reader.parsing/parse!```.
   "
  (let [grammar (grammar-for grammar-id)
        parser (insta/parser grammar :string-ci true :total true)]
    (partial parse parser)))

(defn cfg-parser-for [spec-string]
  "A parser function that parses instaparse grammars and can be passed
   to ```extended-lisp-reader.parsing/parse!``` (i.e. it returns nil
   if it cannot find a parse).

   It returns a parser function which parses the specified language.

   In contrast to ```parser-for``` this returned parser function does
   not return the AST of the parsed input but - again - **a parser
   function** that can be passed to
   ```extended-lisp-reader.parsing/parse!```.
  "
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




