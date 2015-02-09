(ns extended-lisp-reader.csv
  (:require [clojure.walk :as clj-walk]
            [extended-lisp-reader.core]
            [defun :as defun]
            [extended-lisp-reader.instaparse-adapter :as insta]))

;; See README.md "Bugs/TODO": trying to load a namespace as a side effect via
;; a reader eval form. Does not work though.
;;#=(require '(extended-lisp-reader [core]))
;;#=(require '(extended-lisp-reader [instaparse-adapter :as insta]))

;; A simple CSV grammar.
;; Most grammar rules will be hidden from the result. But we want to show
;; how to tranform the resulting AST via core.match - so we let some of the
;; rules show-up in the AST.
(def csv-data!
  #[insta/insta-cfg!
    file = record (line-break record)*
    <_> = <" "+>
    <CR> = '\u000D'
    <LF> = <'\u000A'>
    <CRLF> = CR LF
    <line-break> = CRLF | CR | LF
    <field-sep> = <#" *, *">
    <field> = unquoted-field | quoted-field
    unquoted-field = #"[a-zA-Z0-9]*"
    quoted-field = <'"'> #"[^\"\n\r]*" <'"'>
    record = _? field (field-sep field)* _?
    ])

;; Some simple CSV data which satisfies the grammar above
(def csv-data
  #[csv-data!
    foo , "bar man ccc", boo
    fred , fox
    ]) ;--> [:file [:record [:unquoted-field "foo"] [:quoted-field "bar man ccc"] [:unquoted-field "boo"]]
                 ; [:record [:unquoted-field "fred"] [:unquoted-field "fox"]]
                 ; [:record [:unquoted-field ""]]]

;; Transformation function for parts of the AST match-produce pairs
(defun/defun xform
  ([[:file & es]] (clj-walk/postwalk xform es))
  ([[:unquoted-field e]] e) 
  ([[:quoted-field e]] (pr-str e))
  ([[:record & es]] (apply str (interpose "," es)))
  ([e] e))

#_ ;; Run transformation
(xform csv-data) ;--> ["foo,\"bar man ccc\",boo" "fred,fox" ""]
