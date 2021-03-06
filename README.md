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

When you're consuming DSLs with a parser you usually build an AST
first. Then you walk this AST and build up some data structure which
you pass to some sort if *interpreter* (there are parsers that let you
build that data structure as part of the parsing process;
e.g. https://theantlrguy.atlassian.net/wiki/display/ANTLR4/Actions+and+Attributes). Of
course you could skip the extra step and just interpret the AST
directly while walking through it.

In Clojure we have ```clojure.lang.Compiler```. So all we really have
to do is build the AST (that's what instaparse gives us) and then
```postwalk``` the tree and **produce clojure data structures** which
can/will be given to the ```Compiler``` by the ```LispReader```. So we
(re-) use the fact, that Clojure is a compiled language instead of
building an interpreter for our DSLs.

Below you'll find a simple example of how to parse an *embedded CSV
form* and how to process it, using ```defun```.

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

## More examples

### Parsing CSV

This is a simple instaparse CSV grammar (there are some things
missing, it's just here to show how things work *in principle*;
see ```src/extended_lisp_reader/csv.clj``` for the source):

	(def csv-data!
	  #[insta/insta-cfg!
		file = record (line-break record)*
		<_> = <" "+>
		<CR> = '\u000D'
		<LF> = <'\u000A'>
		<CRLF> = CR LF
		<line-break> = CRLF | CR | LF
		<field-sep> = <#" *, *">
		<field> = unquoted-field | quoted-field
		unquoted-field = #"[a-zA-Z0-9]*"
		quoted-field = <'"'> #"[^\"\n\r]*" <'"'>
		record = _? field (field-sep field)* _?
		])

You can now evaluate:

    #[csv-data!
      foo , "bar man ccc", boo
      fred , fox
    ]

Which gives you:

	[:file [:record [:unquoted-field "foo"] [:quoted-field "bar man ccc"] [:unquoted-field "boo"]]
	       [:record [:unquoted-field "fred"] [:unquoted-field "fox"]]
	       [:record [:unquoted-field ""]]]

Now you can transform this AST and generate the result (using https://github.com/killme2008/defun).
In this case we do this within one funtion but you may as well separate
those two logical steps (this transformation is silly -- TODO: generate XML instead of CSV to make
it a little more usefull)

	(defun/defun xform
	  ([[:file & es]] (clj-walk/postwalk xform es)) 
	  ([[:unquoted-field e]] e) 
	  ([[:quoted-field e]] (pr-str e))
	  ([[:record & es]] (apply str (interpose "," es)))
	  ([e] e))

Now run:

	(xform
	 #[csv-data!
	   foo , "bar man ccc", boo
	   fred , fox
	   ]) ;--> ["foo,\"bar man ccc\",boo" "fred,fox" ""]

## Bugs/TODO

* This works (when put into ```example.clj```):

	    (def ccc #[insta/cfg-parser! s = 'a'* 'b'*])
		(ccc "aabb") ;-> [:s "a" "a" "b" "b"]

  But this doesn't:

		(#[insta/cfg-parser! s = 'a'* 'b'*] "aabb")
		;-> java.lang.IllegalArgumentException: No matching ctor found for class clojure.core$partial$fn__4190

* Doing ```slime-eval-buffer``` (I'm still using Emacs/SLIME/swank)
  fails if the buffer contains an embedded language form and the
  buffer is the one that installs ```extended-lisp-reader.core```
  (e.g. when ```csv.clj``` is the first buffer evaluated). It seems
  that first the whole buffer is read by the Clojure reader and then
  each form is evaluated. In this case the form ```#[...]``` cannot be
  read, because **before** the namespace form must be evaluated. The
  same is true for ```#[insta/insta-cfg! ...]``` and
  ```#[csv-file ...]```. So in this case you'll have to evaluate the
  namespace form and the ```#[insta/insta-cfg! ...]``` form separatly.

  I tried to have that evaluated via a reader eval but that did not
  work (see comment in ```csv.clj```).

* Make the stream-reading parser also parse/consume String (not only
  PushBackReader). That can be used for ad-hoc tests etc.

* Add an example showing how to in-line *real* SQL code.

* Add an example for embedding arbitrary text (ala bash here-document
  http://en.wikipedia.org/wiki/Here_document) maybe with *escape to
  Clojure* via reader eval -- e.g.

        (def w ({1 "one" 2 "two" 4 "four"}))
		
		#[here <<<HERE
		 This pangram lists #=(w 4) a's, #=(w 1) b, #=(w 1) c, #=(w 2) d's,
		 twenty-nine e's, eight f's, three g's, five h's, eleven i's,
		 one j, one k, three l's, two m's, twenty-two n's, fifteen o's,
		 two p's, one q, seven r's, twenty-six s's, nineteen t's,
		 four u's, five v's, nine w's, two x's, four y's, and one z.
		HERE]

* Add *language escape to Clojure* to one of the example
  grammars. E.g. use the backtick char to signal, that the next
  (Clojure) form should be processed by the Clojure reader (i.e. the
  LispReader) and the value of that form is the value that the
  LispReader returns. Can this be done with instaparse? It would mean
  that the parser delegates the analysis to some function and
  continues the parse once the function has returned.

* Usually the LispReader will return **one value** to the Compiler
  (for one S-expression) which will evaluate it.

  Q: Can the LispReader return a value that will be interpreted as
  many values (i.e. a *list of values*) by the Compiler and which will
  be evaluated as *multiple forms*?

  The rational of the question is that this would allow for *multi
  value embedded language forms*. Otherwise one would have to use
  multiple embedded language forms instead.

  Example: Instead of
  
		#[x foo = 42] ;-> (def foo 42)
        #[x bar = 2]  ;-> (def bar 2)

  one would like to use:
  
        #[x foo = 42; bar = 2] ;-> (def foo 42) (def bar 2)

* Change contract of the parsing function: right now it returns
  nil. So ```extended-lisp-reader.stream-parser/parse!``` has no
  information about **why** a tried parse has failed. So if some input
  cannot be parsed at all and EOF is reached, we have no chance to
  give a hint about why some parses that have been tried might have
  failed. Chances are that we did place the closing "]" at the right
  position but that our input did not fit the grammar. So if we could
  give the reason for the last failure to the user, it could help a
  lot. instaparse does give this information. We just have to hand it
  to the stream-consuming loop, keep it there and then make it part of
  the exception that is throwm in case of EOF.

* Can clojure.data.csv, clojure-csv.core and semantic-csv.core be
  integrated with this?

