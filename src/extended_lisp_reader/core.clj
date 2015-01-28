(ns extended-lisp-reader.core)

;; TODO: which namespace does the resolve take place in **exactly**?
(defn embeded-lang-reader! [a-reader ignored-dispatch-char]
  "Consumes one form via ```clojure.lang.LispReader/read``` from the
   given ```java.io.PushbackReader```. The form has to be a symbol
   which will be resolved in the current namespace. The resolved var
   must and is deref'ed to a function taking a
   ```java.io.PushbackReader``` (buffer size 1). The function will be
   called with the passed in ```java.io.PushbackReader``` which will
   still hold the data after the consumed first form. The function
   must then consume the reader/input up to (and including) the
   *closing* ```]```. It must then return a Clojure data structure
   that is the *unevaluated value* of the consumed input. This value
   is returned and will thus be given to the Clojure compiler for
   compilation/evaluation. Note that macro expansion will still take
   place on the returned value. This function is not meant to be
   called by user code but should be hooked into
   ```cloure.lang.LispReader``` via
   ```install-embeded-lang-reader!```.
   "
  (let [fn-sym (clojure.lang.LispReader/read a-reader true nil true)
        a-ns (.deref clojure.lang.RT/CURRENT_NS)
        ;;_ (.println System/out (str "*ns* == " a-ns "  fn-sym = " fn-sym))
        fn-var (or (ns-resolve a-ns fn-sym)
                   (throw (RuntimeException.
                           (format "Could not resolve symbol %s (namespace is '%s')"
                                   fn-sym a-ns))))
        consuming-fn (or @fn-var
                         (throw (RuntimeException.
                                 (format "Symbol %s resolved to var %s which is nil."
                                         fn-sym fn-var))))
        _ nil #_ (.println System/out
                           (format "sym = %s  ns = %s  a-var = %s  fn = %s"
                                   fn-sym *ns* fn-var consuming-fn))]
    (consuming-fn a-reader)))

(defn install-embeded-lang-reader! []
  "Put ```embeded-lang-reader!``` into the static array
   ```clojure.lang.LispReader/dispatchMacros``` at index
   ```\\]```. After this the ```clojure.lang.LispReader``` will
   delegate to ```embeded-lang-reader!``` when it encounters a form
   starting with ```#[```.
  "
  (let [lisp-reader-dispatch-macros
        (.get
         (doto (.getDeclaredField clojure.lang.LispReader "dispatchMacros")
           (.setAccessible true))
         nil)]
    (aset lisp-reader-dispatch-macros \[ embeded-lang-reader!)))

;; Install when loading this namespace.
(install-embeded-lang-reader!)

