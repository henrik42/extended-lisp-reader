(ns extended-lisp-reader.stream-parser)

;; TODO/bug: successfull parse will miss boolean/false!! -> use constant instead!
;; TODO: when instaparse supports character sequence change the contract so that
;; the parser receives a character sequence instead of a String.
(defn parse! [parser a-reader]
  "Consumes the reader until ```]``` is found. Then calls the parser
   on the input string excluding the ```]```.

   The parser must return ```nil``` if it cannot find a complete
   successfull parse for the input string (it must not throw an
   exception in this case). In this case the function recurs,
   appending further input from the reader to the input that has been
   read so far.

   Otherwise the parser returns the AST for the given input string.

   Returns the AST or throws an exception if EOT is reached. This
   function implements a non-greedy parsing strategy: when an AST is
   found, it is returned right away. The function will not try to find
   further parses. So the language the parser parses should use
   balanced [...]. Otherwise the non-greedy parsing strategy may find
   a parse but leave input un-consumed in the reader which could be
   parsed to give a longer successfull parse.
  "
  (let [sb (StringBuilder.)]
    (loop []
      (let [c (.read a-reader) 
            _ (when (= -1 c) (throw (RuntimeException.
                                     (format "EOF reached. Cannot parse '%s'"
                                             sb))))
            c (char c)
            sb (.append sb c)]
        (if-not (= c \]) (recur)
                ;; TODO: fix as soon as instaparse supports character
                ;; sequence.
                (if-let [ast (parser (.substring sb 0 (dec (.length sb))))]
                  ast
                  (recur))))))) 
