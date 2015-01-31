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
(def cfg (partial stream-parser/parse! insta/cfg-parser-for))
(def ab1 (partial stream-parser/parse! (insta/cfg-parser-for "s = 'a'* 'b'*")))
(def ab2 (partial stream-parser/parse! #[cfg s = 'a'* 'b'*]))

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
  (testing "Parse embeded SQL form."
    (is (= #[sql select foo.*, bar.*]
           [:sql "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"])))
  (testing "Parse SQL expression and check tail content"
    (is (= (consume-string "extended-lisp-reader.core-test/sql select foo.*, bar.*] (foo bar)")
           [[:sql "SELECT" " " [:a-name "foo"] ".*" "," " " [:a-name "bar"] ".*"] " (foo bar)"]))))

;;#_ (def foo #[bar])


