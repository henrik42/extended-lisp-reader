(ns extended-lisp-reader.csv
  (:require [extended-lisp-reader.core]
            [extended-lisp-reader.instaparse-adapter :as insta]))

;; See README.md "Bugs/TODO": trying to load a namespace as a side effect via
;; a reader eval form. Does not work though.
;;#=(require '(extended-lisp-reader [core]))
;;#=(require '(extended-lisp-reader [instaparse-adapter :as insta]))

(def csv-file
  #[insta/insta-cfg!
    file = record (line-break record)*
    <_> = <" "+>
    CR = '\u000D'
    <LF> = <'\u000A'>
    CRLF = CR LF
    <line-break> = CRLF | CR | LF
    <field-sep> = <#" *, *">
    <field> = unquoted-field | quoted-field
    <unquoted-field> = #"[a-zA-Z0-9]*"
    quoted-field = <'"'> #"[^\"\n\r]*" <'"'>
    record = _? field (field-sep field)* _?
    ])

#[csv-file
  foo , "bar man ccc", boo
  fred , fox
  ] ; --> [:file [:record "foo" [:quoted-field "bar man ccc"] "boo"] [:record "fred" "fox"] [:record ""]]
