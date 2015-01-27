(ns extended-lisp-reader.example
  (:require [extended-lisp-reader.core]
            [extended-lisp-reader.parsing :as parsing]
            [extended-lisp-reader.instaparse :as insta]))

(def cfg (partial parsing/parse! insta/cfg-parser-for))
                   
;; Die Funktion, die in #[<func> ...] referenziert wird, muss den Reader konsumieren.
;; Daher kann man nicht direkt die Parsing-Fn verwenden, sondern muss die
;; Generator-Fn parse! drumwickeln.
(def sql (partial parsing/parse! (insta/parser-for "sql")))

;; TODO: der Wert von #[sql ...] muss noch via Post-Walk auf eine (...) Form
;; abgebildet werden, damit der Compiler anschließend auch was damit macht.

;; So definiert man die Grammatik über die API
(def ab (partial parsing/parse! (insta/cfg-parser-for "s = ' ' 'a'* 'b'*")))

;; und so definiert man die Grammatik "inline"
(def ab (partial parsing/parse! #[cfg s = ' ' 'a'* 'b'*]))

;; run via
;; lein run -m extended-lisp-reader.example
(defn -main []
  (.println System/out (str "SQL1: " ((insta/parser-for "sql") "select foo.*, bar.*")))
  (.println System/out (str "ABs1 " ((insta/cfg-parser-for "s = 'a'* 'b'*") "ab")))
  (.println System/out (str "SQL2: " #[sql select foo.*, bar.*]))
  (.println System/out (str "ABs2: " #[ab aabbbb])))
  