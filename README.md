# extended-lisp-reader

Extend the Clojure LispReader with non-Lisp-ish DSLs

**WARNING! Do this at home --- ONLY!**

## Lisp-ish DSLs

There are several features in Clojure that help you build DSLs (macros
http://clojure.org/macros, Reader Macros
http://clojure.org/reader). But these DSLs are still Lisp-ish in their
grammar because the *parts* of these DSLs are still Clojure *forms*
(i.e. they **must be** Clojure forms).

Some links on Clojure & DSL:

* http://stackoverflow.com/questions/5457066/use-of-clojure-macros-for-dsls
* https://pragprog.com/magazines/2011-07/growing-a-dsl-with-clojure
* http://storm.apache.org/documentation/Clojure-DSL.html
* http://www.clojure.net/2012/02/22/DSL-Examples/
* http://www.learnr.pro/content/17979-the-joy-of-clojure-thinking-the-clojure-way/327#1341569447:13244.835007405198

If you want to offer a non-Lisp-ish DSL to others (e.g. customers) you
can use one of the parsers (e.g. ANTLR http://www.antlr.org/,
instaparse https://github.com/Engelberg/instaparse) available for the
JVM: build the grammar, let the user write their input to some file
and use the parser to consume that file.

But sometimes you may wish to just write the DSL input **in your
Clojure code** --- like this:

	(def foo #[42 - 3 * 7])
	#[foo = 42 -3 * 7]
	(def bar #[select * from mytable])

## Drawbacks

There are good reasons **not to go this way** (see
http://clojure-log.n01se.net/date/2008-11-06.html#19:38a). Among
others:

* Other people will not understand what your code means ... although
  I've seen code that builds on macros a lot that is as hard to
  understand as my proposed extensions here ;)

* You'll screw up your favorite editor because it will have a hard
  time to tell which part of the files belongs to which grammar (for
  code highlighting etc). Of course there is always Emacs
  (http://www.loveshack.ukfsn.org/emacs/multi-mode.el)

So please take this lib as an **experiment** and play around with it
if you like. Don't expect others to use -- let alone **like** -- it.

## The LispReader

The central class that parses all input 
is ```clojure.lang.LispReader```
(https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/LispReader.java). It
has a few things that you (as a programmer) can control from outside
via the Clojure API. The class itself **is not part of the Clojure
API** so anything that you do to it directly may break once you switch
to another version of Clojure.

The ```LispReader``` uses two arrays for dispatching on the
input. Here I use ```clojure.lang.LispReader/dispatchMacros``` which
holds Clojure functions (```IFn```) that are called by
the ```LispReader``` with the ```java.io.PushbackReader``` that carries
further input. This array is used to *dispatch* on a character after
having found a ```#``` (reader macros). As of Clojure 1.6 this array
does not have an entry 
for ```[```. So for this lib I decided to use ```#[``` in order to dispatch to my *embeded DSL reader*.

Others have done this before: http://briancarper.net/blog/449/clojure-reader-macros

This function must consume the *head* of the input (i.e. the
passed in ```PushbackReader```) and find a successfull parse in
terms of the grammar. The input that follows this *head* (i.e.
the *tail*) must remain unconsumed in the ```PushbackReader``` so it can be
consumed by further processing (driven by the ```LispReader```).

Since I want to be able to use **diffent grammars in one file**
I decided to use the first Clojure form following the ```#[``` to control the grammar.

Example: ```#[sql select * from table]```

## Registering grammars

**TODO**

So how do we map ```sql``` to the grammar?

## Defining grammars

**TODO**

	#[:def-grammar ...]

## Usage

**TODO**


