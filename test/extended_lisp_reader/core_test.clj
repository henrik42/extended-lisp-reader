(ns extended-lisp-reader.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [extended-lisp-reader.core :refer :all]))

(defn consume-string [s]
  (let [r (java.io.PushbackReader. (io/reader (.getBytes s)))
        ast (embeded-dsl-reader r \[)]
    [ast (slurp r)]))

(deftest test-embedded-dsl-reader
  (testing "Parse SQL string as input"
    (is (= [[:sql " " "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]
           (consume-string "sql select foo.*, bar.*] (foo bar)")))))

