(ns extended-lisp-reader.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [extended-lisp-reader.core :as core]
            [extended-lisp-reader.stream-parser :as stream-parser]
            [extended-lisp-reader.instaparse-adapter :as insta]))

(defn- consume-string [s]
  (let [r (java.io.PushbackReader. (io/reader (.getBytes s)))
        ast (core/embeded-lang-reader! r \[)]
    [ast (slurp r)]))

(def sql (partial stream-parser/parse! (insta/parser-for "sql")))

(def my-ns *ns*)

(use-fixtures
 :each
 (fn [f]
   (let [n *ns*]
     (try 
       (in-ns (symbol (str my-ns)))
       #_ (.println System/out (format "Setting namespace to %s" my-ns))
       (f)
       (finally
         (in-ns (symbol (str n)))
         #_ (.println System/out (format "Setting namespace to %s" n)))))))

(deftest test-embeded-dsl-reader
  (testing "Parse SQL DSL"
    ;; compares just the parse
    (is (= [:sql "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"]
           #[sql select foo.*, bar.*])))
  #_ (testing "Parse SQL string as input"
    ;; compares parse and tail 
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "extended-lisp-reader.core-test/sql select foo.*, bar.*] (foo bar)"))))
  #_ (testing "*ns*"
    ;; compares parse and tail 
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "sql select foo.*, bar.*] (foo bar)")))))
  
;; obacht!!! Man kann diesen Ausdruck nicht so ohne
;; weiteres "auskommentieren", weil der #[..] Ausdruck auch
;; "ausgeführt" wird, wenn #_ davor steht!!!! GOTCHA!
;; #_ (deftest test-embeded-grammar-def
;;  (testing "Define a parser from embeded grammar"
;;    (is (= "foo"
;;           #[make-parser select foo.*, bar.*]))))

