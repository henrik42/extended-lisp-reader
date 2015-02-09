(defproject extended-lisp-reader "0.1.0-SNAPSHOT"
  :description "Extend the Clojure LispReader with non-Lisp-ish DSLs"
  :url "https://github.com/henrik42/extended-lisp-reader"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [swank-clojure "1.4.3"]
                 [defun "0.2.0-RC"]
                 [instaparse "1.3.5"]]
  :source-paths ["src"]
  :test-paths ["test"]
  ;;:main ^:skip-aot extended-lisp-reader.core
  :java-cmd "/opt/jdk1.8.0/bin/java"
  :target-path "target/%s"
  ;;:profiles {:uberjar {:aot :all}}
  )
