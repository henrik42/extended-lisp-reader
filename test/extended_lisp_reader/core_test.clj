(ns extended-lisp-reader.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [instaparse.core :as insta]
            [extended-lisp-reader.core :refer :all]))

(defn- consume-string [s]
  (let [r (java.io.PushbackReader. (io/reader (.getBytes s)))
        ast (embeded-dsl-reader r \[)]
    [ast (slurp r)]))

(def sql-parser (insta-parser-for "sql"))
  
(defn- sql [a-reader]
  (let [ast (parse! sql-parser a-reader)]
    ast))

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

(deftest test-embedded-dsl-reader
  (testing "Parse SQL DSL"
    ;; compares just the parse
    (is (= [:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"]
           #[sql select foo.*, bar.*])))
  (testing "Parse SQL string as input"
    ;; compares parse and tail 
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "extended-lisp-reader.core-test/sql select foo.*, bar.*] (foo bar)"))))
  (testing "*ns*"
    ;; compares parse and tail 
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "sql select foo.*, bar.*] (foo bar)")))))
  
