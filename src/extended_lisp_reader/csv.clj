(ns extended-lisp-reader.csv
  (:require [extended-lisp-reader.core]
            [extended-lisp-reader.instaparse-adapter :as insta]))
(def csv #[insta/insta-cfg!
           file = [header CRLF] record (CRLF record)* [CRLF]
           header = name (COMMA name)*
           record = field (COMMA field)*
           name = field
           field = (escaped | non-escaped)
           escaped = DQUOTE (TEXTDATA | COMMA | CR | LF | 2DQUOTE)* DQUOTE
           2DQUOTE = DQUOTE DQUOTE
           non-escaped = TEXTDATA*
           COMMA = '\u002C'
           CR = '\u000D'
           DQUOTE = '\u0022'
           LF = '\u000A'
           CRLF = CR LF
           TEXTDATA = #'[\u0020-\u0021\u0023-\u002B\u002D-\u007E]'
           ])
#[csv "foo","bar"]