(ns extended-lisp-reader.example
  "Note: you need to :require the namespace
  ```extended-lisp-reader.core```. Otherwise the reader will not
  understand the embedded language forms like #[...] in this file."
  (:require [extended-lisp-reader.core]
            [extended-lisp-reader.stream-parser :as stream-parser]
            [extended-lisp-reader.instaparse-adapter :as insta]))

;; (insta/parser-for "sql") builds a parser, that parses input
;; according to resources/sql.bnf and returns nil if a parse cannot be
;; found. Otherwise the instaparse AST is returned.
;;
;; You can apply this parser like:
;;
;; ((insta/parser-for "sql") "select foo.*, bar.*")
;;
;; If you want to use this parser for embedded language forms like
;; #[sql select foo.*, bar.*] you need a function that consumes a
;; reader/stream. You build such a "stream parsing parser" via
;; stream-parser/parse!
(def sql (partial stream-parser/parse! (insta/parser-for "sql")))

;; insta/cfg-parser-for is a parser that parses instaparse grammar
;; expressions. You can use it like:
;;
;; (insta/cfg-parser-for "s = 'a'* 'b'*")
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
;; So insta/cfg-parser-for is a **parser generator**. You can apply
;; such a *generated parser* like:
;;
;; ((insta/cfg-parser-for "s = 'a'* 'b'*") "ab")
;;
;; Again, if you want to use this kind of generated parser to parse
;; embedded instaparse grammars you have to wrap it with (partial
;; stream-parser/parse!)
;;
;; So #[cfg s = 'a'* 'b'*] returns a parser that parses the specified
;; language.
(def cfg (partial stream-parser/parse! insta/cfg-parser-for))

;; You can build stream parsing parsers by using (insta/cfg-parser-for
;; <grammar-string>) directly -- i.e. without using an embedded
;; language form but just the string equivalent.
(def ab1 (partial stream-parser/parse! (insta/cfg-parser-for "s = 'a'* 'b'*")))

;; And you can build such a parser with an embedded language form.
(def ab2 (partial stream-parser/parse! #[cfg s = 'a'* 'b'*]))

;; run this via ```lein run -m extended-lisp-reader.example```
(defn -main []
  (.println System/out (str "SQL1: " ((insta/parser-for "sql") "select foo.*, bar.*")))
  (.println System/out (str "ABs1 " ((insta/cfg-parser-for "s = 'a'* 'b'*") "ab")))
  (.println System/out (str "SQL2: " #[sql select foo.*, bar.*]))
  (.println System/out (str "ABs2: " #[ab1 aabbbb]))
  (.println System/out (str "ABs3: " #[ab2 aabbbb]))
  ;; Bug: this does not work - why?
  ;;(.println System/out (str "ABs4: " (#[cfg s = 'a'* 'b'*] "aabb")))
  )
  