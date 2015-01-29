(ns extended-lisp-reader.core)

(defn embeded-lang-reader! [a-reader __ignored__dispatch-char]
  "Consumes one form (plus any following *Java whitespace*; i.e. ","
   is not consumed as whitespace) via
   ```clojure.lang.LispReader/read``` from the given
   ```java.io.PushbackReader```.

   The form has to be a symbol which will be resolved in the current
   namespace (i.e. the namespace of the form that the ```LispReader```
   is consuming). The resolved ```Var``` must deref to a function
   taking a ```java.io.PushbackReader``` (pushback-buffer size 1).

   The function will be called with the passed in
   ```java.io.PushbackReader``` which will still hold the data after
   the consumed first form/whitespace. The function must then consume
   the reader/input up to (and including) the **closing ```]```**.

   It must return a Clojure data structure that is the *unevaluated
   value* of the consumed input.

   This value is returned and will thus be given to the Clojure
   compiler for compilation/evaluation. Note that macro expansion will
   still take place on the returned value.

   This function is not meant to be called by user code but should be
   hooked into ```cloure.lang.LispReader``` via
   ```install-embeded-lang-reader!```.
   "
  (let [fn-sym (clojure.lang.LispReader/read a-reader true nil true)
        fn-var (or (ns-resolve *ns* fn-sym)
                   (throw (RuntimeException.
                           (format "Could not resolve symbol '%s' (current namespace *ns* is '%s')"
                                   fn-sym
                                   *ns*))))
        consuming-fn (or @fn-var
                         (throw (RuntimeException.
                                 (format "Symbol %s resolved to var %s which is nil."
                                         fn-sym fn-var))))]
    ;; eat up Whitespace
    ((fn [] (let [c (clojure.lang.LispReader/read1 a-reader)]
              (if (Character/isWhitespace c)
                (recur)
                (.unread a-reader c)))))
    (consuming-fn a-reader)))

(defn install-embeded-lang-reader! []
  "Put ```embeded-lang-reader!``` into the static array
   ```clojure.lang.LispReader/dispatchMacros``` at index
   ```\\]```. After this the ```clojure.lang.LispReader``` will
   delegate to ```embeded-lang-reader!``` when it encounters a form
   starting with ```#[``` (i.e. an *embeded language form*).
  "
  (let [lisp-reader-dispatch-macros
        (.get
         (doto (.getDeclaredField clojure.lang.LispReader "dispatchMacros")
           (.setAccessible true))
         nil)]
    (aset lisp-reader-dispatch-macros \[ embeded-lang-reader!)))

;; Install when loading this namespace.
(install-embeded-lang-reader!)

