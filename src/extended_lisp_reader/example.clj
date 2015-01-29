(ns extended-lisp-reader.example
  (:require [extended-lisp-reader.core]
            [extended-lisp-reader.stream-parser :as stream-parser]
            [extended-lisp-reader.instaparse-adapter :as insta]))

(def cfg (partial stream-parser/parse! insta/cfg-parser-for))
                   
;; Die Funktion, die in #[<func> ...] referenziert wird, muss den Reader konsumieren.
;; Daher kann man nicht direkt die Stream-Parser-Fn verwenden, sondern muss die
;; Generator-Fn parse! drumwickeln.
(def sql (partial stream-parser/parse! (insta/parser-for "sql")))

;; TODO: der Wert von #[sql ...] muss noch via Post-Walk auf eine (...) Form
;; abgebildet werden, damit der Compiler anschließend auch was damit macht.

;; So definiert man die Grammatik über die API
(def ab1 (partial stream-parser/parse! (insta/cfg-parser-for "s = 'a'* 'b'*")))

;; und so definiert man die Grammatik "inline"
(def ab2 (partial stream-parser/parse! #[cfg s = 'a'* 'b'*]))

;; run via
;; lein run -m extended-lisp-reader.example
(defn -main []
  (.println System/out (str "SQL1: " ((insta/parser-for "sql") "select foo.*, bar.*")))
  (.println System/out (str "ABs1 " ((insta/cfg-parser-for "s = 'a'* 'b'*") "ab")))
  (.println System/out (str "SQL2: " #[sql select foo.*, bar.*]))
  (.println System/out (str "ABs2: " #[ab1 aabbbb]))
  (.println System/out (str "ABs3: " #[ab2 aabbbb]))
  )
  