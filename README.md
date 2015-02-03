# extended-lisp-reader

Extend the Clojure LispReader with non-Lisp-ish embedded language
forms.

**WARNING! Do this at home -- ONLY!**

## Usage

This lib has not been released yet, so you'll have to get the source
to use it.

See ```extended-lisp-reader/example.clj``` for usage examples.

In short:

* Define a parser for an instaparse grammar:

		(def as-and-bs #[insta-cfg! s = 'a'* 'b'*])

* And use it on some text:

		#[as-and-bs aabb] ;-> [:s "a" "a" "b" "b"]

Or -- if you already have an instaparse parser (```partest-parser```
in this example) -- do this:

	(def partest-parser (instaparse/parser (io/resource "partest.bnf")))
	(def partest! (insta/parser-for partest-parser))
	#[partest! [a]] ;-> [:s "[" [:s "a"] "]"]

## Motivation

There are several features in Clojure that help you build DSLs (macros
http://clojure.org/macros, reader macros http://clojure.org/reader,
tagged literals
http://clojure.org/reader#The%20Reader--Tagged%20Literals). But these
DSLs are still Lisp-ish in their grammar because the *parts* of these
DSLs still have to be Clojure *forms*.

Some links on Clojure & DSL & reader:

* http://stackoverflow.com/questions/5457066/use-of-clojure-macros-for-dsls
* https://pragprog.com/magazines/2011-07/growing-a-dsl-with-clojure
* http://storm.apache.org/documentation/Clojure-DSL.html
* http://www.clojure.net/2012/02/22/DSL-Examples/
* http://www.learnr.pro/content/17979-the-joy-of-clojure-thinking-the-clojure-way/327#1341569447:13244.835007405198
* http://clojure-log.n01se.net/date/2008-11-06.html#19:38a
* http://stackoverflow.com/questions/5746801/what-advantage-does-common-lisp-reader-macros-have-that-clojure-does-not-have
* This article inspired me to implement this lib: http://homepages.cwi.nl/~storm/publications/token-inter.pdf

If you want to offer a non-Lisp-ish DSL, you can use one of the
parsers (e.g. ANTLR http://www.antlr.org/, instaparse
https://github.com/Engelberg/instaparse) available for the JVM: build
the grammar, let the user write their input to some file and use the
parser to consume that file. That will give you an AST (abstract
syntax tree) which you have to *process* in some way -- usually this
means, you have to write some sort of *interpreter* for this
language/AST.

But sometimes you may wish to just write the DSL input **in your
Clojure code** -- maybe like this:

	(def foo #[42 - 3 * 7])
	#[foo = 42 -3 * 7]
	(def bar #[select * from mytable])

## Drawbacks

There are reasons **not to go this way**. Among others:

* Other people will not understand what your code means ... although
  I've seen code that builds on macros a lot that is as hard to
  understand as my proposed extensions here ;-)

* Using things that execute at *read time* introduces extra
  complexity. But then again that's true for eval reader
  (```#=(<forms>)```), record/type literal syntax
  (```#<type>[<forms>]```) and tagged literals (```#<namespace>/<sym>
  [<forms>]```) as well.

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
via the Clojure API (see above). The class itself **is not part of the
Clojure API** so anything that you do to it directly may break once
you switch to another version of Clojure.

The ```LispReader``` uses two arrays for dispatching on the
input. Here I use ```clojure.lang.LispReader/dispatchMacros``` which
holds Clojure functions (```IFn```) that are called by 
the ```LispReader``` with the ```java.io.PushbackReader``` that carries
further input. This array is used to *dispatch* on a character after
having found a ```#``` (reader macros). As of Clojure 1.6 this array
does not have an entry 
for ```[```. So for this lib I decided to use ```#[``` in order to
dispatch to my *embedded language form reader*.

Others have done something similar before:

* http://briancarper.net/blog/449/clojure-reader-macros

* http://fulldisclojure.blogspot.de/2009/12/how-to-write-clojure-reader-macro.html

This function must consume the *head* of the input (i.e. the
passed in ```PushbackReader```) and find a successful parse in
terms of the grammar. The input that follows this *head* (i.e.
the *tail*) must remain unconsumed in the ```PushbackReader``` so it can be
consumed by further processing (driven by the ```LispReader```).

Since I want to be able to use **diffent grammars in one file**
I decided to use the first Clojure form following the ```#[```
to control the grammar.

Example: ```#[sql select * from table]```

## Registering DSLs

So how do we map ```sql``` to the grammar? I think that Clojure's
Namespaces and Var's are a great way to *register* stuff. So instead
of defining my own mechanism I just use (namespace qualified) symbols.

	(def sql (partial stream-parser/parse! (insta/parser-for "sql")))
    #[sql select * from table]

This symbol resolves to a function that consumes
a ```java.io.PushbackReader``` and returns the *value* of the form. So
you're registering a function -- not a grammar.

## Defining grammars

When you think about it you may ask: could I -- as a special use case
-- define an instaparse grammar (or any other grammar) within the
Clojure code and build a function that I could then use to process DSL
input according to this grammar? Like this:

	(def abs #[insta-cfg! s = 'a'* 'b'*])
	#[abs aabbbb]

So in this case we have **two embedded languages**:

* ```insta-cfg!``` lets you define a parser/grammar with
  ```#[insta-cfg! s = 'a'* 'b'*]```

* and ```abs``` uses this parser/grammar to consume input like
  ```#[abs aabbbb]```

## Semantics

**TODO: this is my PLAN to go. Not implemented yet**

When you're consuming DSLs with a parser you usually build an AST
first. Then you walk this AST and build up some data structure which
you pass to some sort if *interpreter* (there are parsers that let you
build that data structure as part of the parsing process;
e.g. https://theantlrguy.atlassian.net/wiki/display/ANTLR4/Actions+and+Attributes). Of
course you could skip the extra step and just interpret the AST.

In Clojure we have ```clojure.lang.Compiler```. So all we really have
to do is build the AST (that's what instaparse gives us) and then
post-walk the tree and **produce clojure data structures** which
can/will be given to the ```Compiler``` by the ```LispReader```. So we
(re-) use the fact, that Clojure is a compiled language instead of
building an interpreter for our DSLs.

This way we can (re-)use any macro and function we have. We can use
the Lisp-ish macros and functions and build Lisp-ish DSLs if we like
(and this has been done) and have tests against that and use them as
usual.

And if we ever wanted a non-Lisp-ish DSL we can just build one on top
of that and re-use all the tested stuff we already have. No extra
wheel-re-inventing bug-ridden ad-hoc interpreter needed (which you
need for your usual Java solution -- see for example
http://bkiers.blogspot.de/2011/03/7-interpreting-and-evaluating-tl-i.html;
of course one can generate Java code as well
http://stackoverflow.com/questions/24766006/getting-antlr-to-generate-a-script-interpreter
and run the compiler on that, but still you would have to build that
yourself).

So in the end we could get a **compiled embedded DSL!**

## Gotchas

You'll may be surprised that you cannot comment-out embedded language
forms with ```#_``` in some cases (e.g. if ```foo``` has not been
defined -- like in this example):

	#_ #[foo bar]

You'll have to use
  
	; #[foo bar]

## Bugs/TODO

* This works (when put into ```example.clj```):

	    (def ccc #[insta/cfg-parser! s = 'a'* 'b'*])
		(ccc "aabb") ;-> [:s "a" "a" "b" "b"]

  But this doesn't:

		(#[insta/cfg-parser! s = 'a'* 'b'*] "aabb")
		;-> java.lang.IllegalArgumentException: No matching ctor found for class clojure.core$partial$fn__4190

* Add an example showing how to in-line CSV data and *real* SQL code.

* Add functionality to bring *semantics* into the processing -- i.e. a
  function that is applied to the AST and which returns the Clojure
  data structure that can be given to the Compiler (the equivalent of
  a *form*).

