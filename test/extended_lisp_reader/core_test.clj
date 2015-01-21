(ns extended-lisp-reader.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [instaparse.core :as insta]
            [extended-lisp-reader.core :refer :all]))

(defn consume-string [s]
  (let [r (java.io.PushbackReader. (io/reader (.getBytes s)))
        ast (embeded-dsl-reader r \[)]
    [ast (slurp r)]))

(defn sql [a-reader]
  (let [grammar-id "sql"
        grammar (grammar-for grammar-id)
        parser (insta/parser grammar :string-ci true :total true)
        ast (parse! parser a-reader)]
    ast))

(deftest test-embedded-dsl-reader
  (testing "Parse SQL string as input"
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "extended-lisp-reader.core-test/sql select foo.*, bar.*] (foo bar)")))))

