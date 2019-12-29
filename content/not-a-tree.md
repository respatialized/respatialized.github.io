---
title: This Website Is Not a Tree
published: "2019-03-10"
topics: meta,writing
---

<blockquote class="bl bw2 b--green w-60">
<p class="f3 pl2 b">It must be emphasized, lest the orderly mind shrink in horror from anything that is not clearly articulated and categorized in tree form, that the idea of overlap, ambiguity, multiplicity of aspect and the semilattice are not less orderly than the rigid tree, but more so. They represent a thicker, tougher, more subtle and more complex view of structure.</p>

<span class="pl2">Christopher Alexander, [A City Is Not A Tree](https://www.patternlanguage.com/archive/cityisnotatree.html) </span>
<div class="f7 tr">essential reading.</span>
</blockquote>

<span class="f2 b">Preliminaries</span>
<span class="f4">2019-03-10</span>

This is the first post of respatialized, a website about actual and potential spaces. Part of the reason it took me so long to launch it is because nearly every static site generator forces your writing into a tree-like structure. Only one lets you extend the site generation methods to reflect your own ideas: Matthew Butterick's [`pollen`](https://docs.racket-lang.org/pollen/). Because I want to combine the sequential and additive writing style of a blog with the associational and iterative nature of a wiki, this was the only choice.

It would have taken me even longer to get started if Joel Dueck hadn't already done the excellent work of creating  [`thenotepad`](https://github.com/otherjoel/thenotepad), which includes functions to produce many of the things we expect from blogs, like sequential indices and RSS feeds, and many that we should expect, but don't (like the ability to generate a PDF from the blog). The code that generates this blog is forked from  [`thenotepad`](https://github.com/otherjoel/thenotepad) and licensed under the MIT License.

<span class="f2 b">Extensible Textual Notation</span>
<span class="f4">2019-11-13</span>

I recently switched from `pollen` to `perun`. `perun`'s model of publishing everything via a composable collection of `boot` tasks encapsulates everything that I want from `pollen`'s organizational and compositional capabilities. `pollen`'s pagetrees can be recreated by mapping and filtering the sequential collections of `hiccup` data structures `perun` generates, and applying those transformations to generic collections comes more readily to me than creating `.ptree` files (the Clojure refrain, it's just data, etc.). My artwork and other content is also written in and generated using Clojure, so I don't want to have to drop into a different language that I don't know as well just to get it out. For me, the ability to iterate quickly depends on low friction and the power of simplicity: `boot`'s Swiss army knife approach matches that perfectly. 

However, I agree wholeheartedly with Matthew Butterick when [he argues](https://docs.racket-lang.org/pollen/second-tutorial.html) that Markdown is a constraining environment in which to write, especially if you're looking to write a sustained treatment of a topic which usually generates a deep and rich collection of self and cross references and its own conventions for referring back to subtopics organically over time. Markdown supports the lowest level of this: links. Anything else, you're on your own, but with a completely restricted method of manipulating the input texts.

Also, sometimes I want to contextually distinguish textual elements using CSS and I want to do it without rewriting my markdown parser. I currently do this by littering my markdown posts with `<div class="...">` tags, which is kludgy and offers no way of systematically changing the classes applied to the textual element apart from doing find-replace on all of them with `grep`.

The `#lang pollen` directive provides a beautiful way of letting prose be prose while still letting you access the full power of a programming lanugage whenever you need it via the lozenge (`◊`) special character. 

What I'm looking for, basically:
<div class="ws-normal bg-light-silver code">
I'd like the ability to embed ◊(link hiccup "https://github.com/weavejester/hiccup")/clojure data structures into my program. They can either be data (see below) or functions called at render time that evaluate into data (see above).

[:em {:class "topic"} Extensibility]

Clojure already supports this notion in its canonical representation of data, extensibile data notation. I want to bring it to textual information, and maybe, HTML Canvas objects as well. The full power of a programming language means that we can flexibly switch between graphical and textual representations, something that pollen doesn't yet support.
</div>

This approach also acts as a force-multiplier on immutable, compositional CSS tools like `tachyons` or `tailwind`, because it brings the power of Clojure into the tool you're using to write the text, which in turn leverages tools like `tachyons` to apply a unified style to what you're writing using inline, simple notation.

Other examples in this space:

- idyll
- mdx

Both of these are built atop Javascript, and are more focused on interactivity for users than on procedural generation of text at write-time. Clojure(script)'s homoiconicity makes it ideally suited for both purposes- it can be used to generate interactive programs as well as any other form of data you'd want to display. I'm personally more interested in the latter, right now.

<span class="f2 b">Extensible Textual Notation, part 2</span>
<span class="f4">2019-11-17</span>

Within the Clojure world and beyond, there are a few tools that suggest directions for what I'm thinking of here. 

#### `perun`

This is what I'm using to write and compile the blog itself right now. However, I don't like that most of the decisions about how to parse markdown into HTML are decided by the fiats of `flexmark-java`. I would much prefer to be able to manipulate the content in the form of  `hiccup`  data structures as I see fit _before_ passing it to `hiccup.core/html5` for rendering. [This was discussed](https://github.com/hashobject/perun/issues/30) in the `perun` repo, but was set aside when the use case of hyphenation didn't actually require it.

However, there are many other reasons you'd be interested in representing your writing as data.
>  _if the book is a program, the source for that book should look more like your brain, and less like HTML (or XML or LaTeX or ...)?_  [Matthew Butterick](https://docs.racket-lang.org/pollen/second-tutorial.html)

Personally, I'm interested in using the most powerful tools I have. For example, you could parse the writing into discrete chunks represented as `hiccup` data structures, record them as facts in a [`datascript`](https://github.com/tonsky/datascript) DB, and query them like any other source of data. This would be even more powerful if you ran it against not just the current state of your writing, but its revision history.

#### [`oz`](https://github.com/metasoarous/oz)

As a Clojure enthusiast who got started with Jupyter notebooks, I quite like the idea of using Clojure for interactive documents. I just wish Markdown wasn't so uncritically accepted as the default for text authoring, because it seems silly to give yourself the whole power of a programming language in rendering a document and then arbitrarily restrict its scope to making graphs. Scientific documents in particular deal with lots of structured data: batteries of tests, statistical analyses, summary tables. Presentation of that data is not limited to graphs: a more powerful authoring model would allow you to dynamically generate and restructure the prose annotations of scientific data as easily as the graphs that summarize it. 

Anything less feels like an arbitrary step backward.

### Code documentation tools

The major area in which programmers _currently_ perform programmatic manipulation of prose and data in tandem is in the realm of documentation generators. In my experience, these tools fall into two broad categories:

- narrative-first tools like `sphinx, reStructuredText`, etc. 
  They have one primary benefit: if they're to be of any use at all they require the author to write a good amount of prose introducing the project, its rationale and purpose, and the main ways of interacting with it. However, in these tools, docs are generally separate from code - even if they live in the same repo, they're often in `docs/` and can easily come out of sync with the actual code.
- code-first tools like any `javadoc, docco, Roxygen2`, etc.
  These have the benefit of being much closer to the day-to-day work of developers and are much less likely to become out of sync with the code, because they are usually parsed out of docstrings and special comments and the process of updating documentation can be built into a project's deployment pipeline without much overhead. The drawbacks? You generally end up with a completely decontextualized list of classes or functions that doesn't inform or give examples of how you'd actually _use_ them. 

#### [`marginalia`](https://gdeer81.github.io/marginalia/)

`marginalia` generates elegant-looking literate programming documents from plain Clojure source code. Like `perun`, however, it returns rendered HTML rather than structured data from its parsing of source files. 

#### [`cod`](https://github.com/namuol/cod)

As a code documentation tool, `cod` feels like it has the right idea at its core. Instead of deciding how to present the documentation it pulls out of the source code for you, `cod` simply returns JSON data representing the annotations. Any further decisions about how to represent that JSON data in the final documentation are up to the author, allowing for a better blend of narrative and code than other documentation tools.

#### [`scribble`](https://docs.racket-lang.org/scribble/)

The fact that Racket libraries tend to have _vastly_ superior documentation (on average) than any other programming language is a testament to the power of Scribble. Naturally, `pollen` owes a lot to the starting point that `scribble` created.

### Why look at code documentation tools?

Mostly because I know that I'm going to have to write my own solution to this problem. I want the solution's source code to itself generate an example of the kind of document I want it to produce, so I'm hoping I can steal as many existing functions as possible from these other libraries while I'm bootstrapping the project.

<span class="f2 b">Structural features of writing and information management systems</span>
<span class="f4">2019-12-14</span>

I've gone through myriad to-do apps, organizers, journaling systems. Here's a table depicting my overall thoughts.

<div class="mr1">

| Type               | Examples            | Advantages                                             | Disadvantages                  |
| ---                | -----               | ------                                                 | -----------                    |
| Binder notebook    | Filofax             | associative,organic,frictional,multi-modal,simple      | atemporal,apresentist          |
| Diary              | Bullet journal      | chronological,frictional,reflective,multi-modal,simple | apresentist                    |
| To-do app          | Nozbe, todoist      | fast,simple,portable                                   | decontextualized,non-iterative |
| Outline app        | Org-mode, workflowy | fast,deep                                              | hierarchical                   |
| Kanban             | Trello              | situated                                               | decontextualized               |
| "free-form" / wiki | Notion              | associative,compositional,iterative                    | hierarchical,laborious         |
| Website            | This                | frictional,multi-modal,associative                     | laborious,atemporal            |
|                    |                     |                                                        |                                |

</div>

All of these advantages and disadvantages stem from one real underlying issue, in my view: each tool imposes its own view over the data you put into it, making alternative ways of looking at the same information difficult or impossible. Paul Chiusano has [written nicely](https://pchiusano.github.io/2016-10-13/view-inspired.html) about the conceptually weak data model an "intuitive" design imposes on the information it represents:

> We often think about views first because views are concrete, and it’s what we interact with directly when we use software. But actually designing software ‘view first’ is problematic because it leads to rigid models that aren’t flexible enough to support the myriad of creative ways that people use your software. It also leads invariably to feature creep—when your model is overly influenced by some concrete views you had in mind during design, it invariably ends up insufficiently general purpose. So as your software becomes more popular, you start adding one-off ‘features’ to support concrete use cases that your users are asking for. A few years pass of this feature creep, and you have a bloated, complicated piece of software that no one gets joy out of using.

Every to-do list and knowledge management system suffers from this problem, In fact, I can feel the constraints imposed by the table above limiting what I want to say about each tool, so let's dive into what I mean by each of these words:

- **associative** - topics and items added at different times can be seen side-by-side, permitting recontextualization of existing information.
- **organic** - order emerges from what is added rather than being imposed.
- **frictional** - the extra effort required to add additional material actually performs useful work rather than being a hindrance (a benefit that has thus far made handwritten notes more valuable to me than digital ones).
- **multi-modal** - multiple systems of representation can be easily employed in the same context.
- **atemporal** - the system has no direct representation of the temporal ordering of its contents.
- **chronological** - the system has a direct representation of the temporal ordering of its contents.
- **reflective** - the system provides opportunities for reflection.
- **apresentist** - the system has no direct representation of what's "current."
- **fast** - adding information is quick and reliable.
- **simple** - the system itself does not impose barriers to adding additional information.
- **portable** - information can be added and recalled through multiple mechanisms or devices.
- **decontextualized** - information or items are cut off from their surrounding context.
- **non-iterative** - the system does not support the process of refining information added to it; it expects items in their "final state."
- **deep** - the system supports long-form treatments of the information added to it
- **hierarchical** - the system requires information to be organized in a tree format, thwarting associational views of it.
- **situated** - the system provides useful background information without getting in the way of the work the information is intended to support.
- **compositional** - underlying data, through association, can be _composed_ into higher-level information.
- **laborious** - the effort required to add or revise material imposes costs rather than providing benefits.

Most recently, I've been using a Zettelkasten-style system for my notes with a filofax binder. It's superb for free association, quick entry, and the generative friction that only putting pen to paper can provide. It's not so good for revisiting previous notes, synthesizing them into new information, reflecting on the past, or maintaining a view of what's "current." Before that, I used a journal-style notebook that was similarly good at quick free-form entry and helped maintain a chronological view of things that aided in reflection, but failed to support associational views of the information recorded within it and similarly suffered from difficulties in keeping things current. I think a two-phase system that facilitates the refinement of paper "drafts" into digital "facts" would be ideal for me, personally.

Many digital systems for doing this exist already. I chafe at using them because they all uncritically accept that markdown is a useful format for representing semantically rich textual information, and then shoehorn features on top of it to make up for its limitations.

Obviously, I'm also taking notes here instead of on paper. Writing this doesn't provide exactly the same generative friction as pen and paper, but does a good enough job of forcing me to clarify my thoughts through the pressure of putting them in a public format. I also have complete control over the content (once I can overcome the limitations of markdown). Given that what I write has currently a 1:1 file:destination relationship, it also prevents association and composition of the information I record here. Ideally you'd want to break this input/output link, which would support both private views of some information and also let you think about how to refer to the same piece of information from multiple public views. 

The question on how to [individuate pieces of information](https://plato.stanford.edu/entries/information-semantic/) is permanently open, so an ideal system would support "contention" in that it can facilitate multiple methods of splitting up and representing a topic. How to do that on a technical level is obviously also an open and extremely difficult problem.

It seems daunting to come up with a solution for this, but I've been reading about something that may offer a partial way out recently: Datascript, mentioned in passing earlier. Where Chiusano proposes algebraic data types to manage this, I would prefer to start with datoms that get freely composed into views through datalog queries. Pieces of information (or even bits of writing themselves) would be decomposed down into EAVT facts and recorded in some persistent database where they can change in the future without fear of losing knowledge by revising it.

There's a lot more to say on the design of this, but mostly I wanted to get this concept "on paper" for further development into a design.

<span class="f2 b">Structural features of writing and information management systems, part 2</span>
<span class="f4">2019-12-15</span>

Another distinction that cuts across everything that I referenced in that table above is the idea of _closed_ versus _open_ knowledge management systems. While my notebook has acquired a significant amount of internal complexity, it is largely a _closed_ system, making interaction with other sources of information more difficult. I have a gigantic pinboard backlog, highlights in a kindle, scattered paper notes about physical books, and no means of integrating them or refining them into something more meaningful.

A lot of PIMs designed to support academic reseearch are "open" towards producing and consuming the primary objects of academic research: papers and monographs. I'm not an academic. While writing helps me clarify my ideas, I also need tools oriented towards the work I do in programming, which means supporting a more _situated_ understanding of what I'm doing. By that I mean supporting a "keep this in mind as you act" understanding of something rather than a "discrete textual description" understanding of something. In cybernetics terms, one might say that my information management systems have not had the _requisite variety_ to handle the tasks I want them to support. They are not _open_ to non-textual workflows. 

Here's a quick sketch of what this might be look like:

<div class="w-30">

![views-sketch](media/views_sketch.jpg)

</div>

The bottom has a pomodoro-style task tracker and the "current task", the right pane has a grouping of recent commits to keep the actual output of that task in mind as well. The role of these panes isn't the important part - the mechanism by which they're generated is. By pulling information from a common store, simple contextual visualizations of relevant parts of it would be easy to construct via Datalog queries. 

A further source of information comes from the seemingly simple fact that these pieces of information are _displayed together_. The entities referenced by the views currently active could be linked through additional queries - for example, the commits happening in the text editor could create corresponding entities with attributes linking them to the entity of the current task. Similarly, information entries updated when a text file is open or a namespace is edited could be linked with that text file. This establishes a notion of _relevance_ for the supporting materials of the work being done.

<span class="f2 b">This website (could be) a CRDT</span>
<span class="f4">2019-12-26,2019-12-28</span>

While considering potential applications of [relay](https://respatialized.net/relay.html) software, I recalled the notion of a _conflict-free replicated data type_, a data structure that provides a probably correct solution to the problem of imposing a total order on a sequence of edits to a file that arrive out of order, editing different subsets of text, with unreliable timestamps. This data type would be what you reach for if you were designing a collaborative text editor with online and offline editing capabilities, because it would save you from making hard choices about which text to discard and which to keep (or worse, making the user deal with any errors caused by your software and imposing those choices on them).

I started reading about the concept, glossing over the mathematical details in favor of an interest in its potential as an expressive medium for thought. Some ideas that fell out of this.

<span class="f3">Making the library metaphor in "code library" concrete</span>

_heavily inspired by Rich Hickey's talk [Spec-ulation](https://youtube.com/watch?v=oyLBGkS5ICk)_

Right now you have to take home the whole library when you write some code that uses one page of one book.

Statically typed languages that rely on complex class hierarchies, especially because the compiler may make multiple passes across the codebase for definitions in different files ("... all you wanted was the banana." - Joe Armstrong) force you to ship all this supporting material to use one part of it. 

_aside: I don't intend this as an intrinsic dig at statically typed, compiled languages per se. Smart compilers can do dead code elimination, but usually this technique is put to the purpose of reducing _executable_ size rather than reducing _dependency_ size. It'd be very interesting to see a compiler targeting library code that minimizes the volume of the library code pulled in by the code which declares it as a dependency. [Unison](https://www.unisonweb.org/docs/tour) has done some interesting work in this direction because of its ability to serialize algebraic data types and send them over the network to perform remote computation._

So if instead of classes defined across files or nested relationships between algebraic data types defined at compile time, we had functions operating on simple, immutable values defined in self-contained s-expressions, plus some annotations:

```
(defn myfunc
 {:calls #{this.ns/func other.ns/func}
...)
```

_this could maybe be achieved even without the manual annotation if you used a macro to pull the symbols out of the function expression at compile time_

Rather than a scope defined by a global namespace of evaluated expressions, these explicit references define exactly what a function needs to be lifted out of its lending library and used independently of the codebase it came from.

_to make an analogy to Unison above, using `core.spec` plus these dependency annotations means that the functions would be addressed by _contract_ rather than by _content_. I think that Unison's emphasis on making functions immutable is a good one, but annotating the contract rather than the internals may do a better job of preserving intent for a dynamically typed language._

S-expressions would slot naturally into the delimited data structures required by a CRDT, making this serialization easy (other programming languages may have a harder time). This opens up another application:

<span class="f3">New forms of revision control</span>

CRDTs can contain arbitrary series of revisions to the same underlying data, in a method guaranteed to converge on a consistent result.

Documentation could be stored in the same CRDT. If the documentation has old timestamps, a tool could be built atop them to warn the user or author that they're stale relative to the rest of the code. Test results could be stored with the hash of the CRDT at the time they were executed, making failing tests trivial to reproduce. With a clever index, the failing tests associated with a given function could be recalled from the codebase's history with a query, providing useful context for finding the source of an unexpected regression.  

_again, this requires a language with an unambiguous syntax and referential transparency to be truly effective I make no claims to being fair to non-lisp programming languages._

Configuration for external systems, or expressions that modify it, could be stored in the same CRDT as the code itself. Integration and system tests could be linked with configuration changes in the manner I describe above, providing context for when the components of a distributed system fail and are made to work again. 

Tests could be shipped around with their functions using a similar annotation syntax to the one above so that someone can have guarantees about the external code they're relying on.

<span class="f3">A notebook for the table beside your hammock</span>

If code and documentation are part of the same data structure as a whole, then an "ideas first" approach to software is as easy to start and maintain as a new experimental repo. The recorded ideas can evolve in tandem with the code that implements them, and their interplay gets expressed through the immutable history of the data structure recording them. It's an environment that makes hammock-driven development as easy as flow-state coding and bug squashing, with the ability to fluidly switch between them without breaking the flow. Code itself as one component part of an open system that doesn't treat writing down the problem and writing the code that solves it as separate activities.

What else is possible? Right now, code takes on the shape that Git repositories, and the software we use to interact with them, want it to take. Can we break code revision history and reuse out of the paradigm of discrete individual repositories? Is a distributed data structure like this enough to make the distinction between "monolithic" and "microservice-oriented" code obsolete? 

I'm definitely interested in where this could lead, but I have to figure out how to create s-expressions from my prose first. 

_Oh, and by the way, the formal term for the structure that emerges from a properly implemented CRDT is a_ [monotonic semilattice](http://archagon.net/blog/2018/03/24/data-laced-with-history/). _Which, according to Christopher Alexander in the essay I quote above, is exactly the form required to capture the interdependent complexity of a city._

<span class="f2 b">Extensible Textual Notation, part 3</span>
<span class="f4">2019-12-28</span>

<span class="f3 b">Structure from text</span>

I want to replicate `pollen`'s ability to let prose be prose while still incrementally bringing in a programming language when it's needed, but also combine it with Clojure's own data structures to capture the structure that emerges organically from the act of writing, so I could, for example, capture the table above not just as a sequence of textual elements but also preserve the structure of the tabular data itself for future use somewhere else. 

The simplest implementation of that would be just reading in the file line by line and constructing maps from the paragraphs separated by line breaks:

```clojure
{:id e4268ac2
 :text "Paragraph one."}
{:id e4268ac3
 :text "Paragraph two."}
```

The initial reading process creates entities that serve as placeholders for text as it is when read and as it may be in the future, all recorded as facts in a EAVT/RDF semantic triple format. Knowledge atoms instead of data atoms. But a collection of facts doesn't preserve the ordering of their original composition, which is a lot of structure to throw away. There are two ways of preserving it that initially occurred to me:

[1] files are entities too - just have them refer to their contents as distinct entities.

```clojure
{:entity 23542
 :attribute :filename
 :value "plaintext-file.txt"}
{:entity 23542
 :attribute :contents
 :value [52952 29587 29042]}
```

In this mode, order of paragraphs is asserted as a fact on the basis of the vector of entity ids of the constituent paragraphs.

[2] Alternatively, the facts about the paragraph order could just be composites of other facts:

```clojure
 {:entity 23542
  :attribute :contents
  :value [{:uuid ab50234 :text "opening paragraph goes here"}
          {:uuid ab50235 :text "second paragraph goes here"}]}
```

I don't really like 2. it feels ad-hoc and non-relational, whereas 1 seems more relationally correct but is semantically not as rich as an individual fact. This shortcoming is easily resolved by a query to pull in the relevant text, however. 

Speaking of which:

<span class="f3 b">Text from structure</span>

When thinking about where to store this data, I was led to [`cause`](https://github.com/smothers/cause), a very well-documented Clojure implementation of a causal tree, a type of CRDT. It places the notion of a `CausalBase` front and center, which sounds great, except it doesn't quite have the power implied by the "database" referred to by its name - which is generally okay in Clojure because the language already has pretty powerful facilities for quick operations on collections of maps. 

But what if someone went further than that, combining a CRDT with a data model and query engine like in DataScript? Turns out in describing that I'm describing [`datahike`](https://github.com/replikativ/datahike), a Datalog implementation atop the `hitchiker-tree` CRDT. 

With existing text snapshotted as facts and recorded in a CRDT, queries could be run against that data to associate formerly disparate pieces of data into new forms, and the composites those queries create could themselves be recorded and annotated as new facts about the collection. The query that retrieves those facts could be stored as data itself, with the new structure that the query identifies added as an annotation to it. Use these queries and the expressive power they create to give a new life to `structur` and `alpha`, the venerable software extensions to `Kedit` written by Howard J. Strauss to aid [John McPhee in his writing process:](https://www.newyorker.com/magazine/2013/01/14/structure)

> _He listened to the whole process from pocket notebooks to coded slices of paper, then mentioned a text editor called Kedit, citing its exceptional capabilities in sorting. Kedit (pronounced “kay-edit”), a product of the Mansfield Software Group, is the only text editor I have ever used. I have never used a word processor. Kedit did not paginate, italicize, approve of spelling, or screw around with headers, WYSIWYGs, thesauruses, dictionaries, footnotes, or Sanskrit fonts. Instead, Howard wrote programs to run with Kedit in imitation of the way I had gone about things for two and a half decades._
>
> _He wrote Structur. He wrote Alpha. He wrote mini-macros galore. Structur lacked an “e” because, in those days, in the Kedit directory eight letters was the maximum he could use in naming a file. In one form or another, some of these things have come along since, but this was 1984 and the future stopped there. Howard, who died in 2005, was the polar opposite of Bill Gates—in outlook as well as income. Howard thought the computer should be adapted to the individual and not the other way around. One size fits one. The programs he wrote for me were molded like clay to my requirements—an appealing approach to anything called an editor._
>
> _Structur exploded my notes. It read the codes by which each note was given a destination or destinations (including the dustbin). It created and named as many new Kedit files as there were codes, and, of course, it preserved intact the original set. In my first I.B.M. computer, Structur took about four minutes to sift and separate fifty thousand words. My first computer cost five thousand dollars. I called it a five-thousand-dollar pair of scissors._
>
> _I wrote my way sequentially from Kedit file to Kedit file from the beginning to the end of the piece. Some of those files created by Structur could be quite long. So each one in turn needed sorting on its own, and sometimes fell into largish parts that needed even more sorting. In such phases, Structur would have been counterproductive. It would have multiplied the number of named files, choked the directory, and sent the writer back to the picnic table, and perhaps under it. So Howard wrote Alpha. Alpha implodes the notes it works on. It doesn’t create anything new. It reads codes and then churns a file internally, organizing it in segments in the order in which they are meant to contribute to the writing._
>
> _Alpha is the principal, workhorse program I run with Kedit. Used again and again on an ever-concentrating quantity of notes, it works like nesting utensils. It sorts the whole business at the outset, and then, as I go along, it sorts chapter material and subchapter material, and it not infrequently arranges the components of a single paragraph. It has completely served many pieces on its own._

[The book is a program](https://docs.racket-lang.org/pollen/). Tools for writing digital books should be at least as powerful as the tools created for conventional books decades ago. CRDTs provide a reliable and immutable foundation to the discrete chunks of knowledge that McPhee has used for his entire career. A query engine provides the toolkit to devise new ways of composing them together as powerful as `structur` and `alpha`, but with the added benefit of an entire programming language so that the text (or the collection of notes used to produce it) is no longer a closed system but can instead pull in data from the rest of the world. 

<span class="f2 b">The Markdown Cargo Cult</span>
<span class="f4">2019-12-29</span>

> _First and worst, Markdown isn’t semantic._
[Matthew Butterick](https://docs.racket-lang.org/pollen/second-tutorial.html)

I view basically every other problem with Markdown as downstream from this. Like Butterick, I'm utterly baffled by the degree to which everyone developing new types of interactive authoring tools simply assumes that everyone will want to write text in a format that's completely blind to the organic structure that emerges from ordinary writing. [Jupyter notebooks](https://jupyter-notebook.readthedocs.io/en/stable/examples/Notebook/Working%20With%20Markdown%20Cells.html),  [documentation tools](https://www.mkdocs.org/), [interactive data science toolboxes](https://github.com/metasoarous/oz/blob/be700e721fd758024f0783083279132afc42f317/examples/test.md), [self-contained environments for interactive authoring](https://github.com/nteract/nteract/blob/f94502e4ff654bb58166bff262f133d4f449b049/packages/outputs/src/components/media/markdown.md), [JS-based explorable explanation tools](https://idyll-lang.org/docs/syntax),  [revolutionary new prototypes of combined programming languages and visual environments](https://github.com/witheve/Eve/commit/fa1700cb37198d1a02ebbaaa506c70c40b201d76), [further explorations of how programming could be different](https://github.com/mhuebert/maria/blob/88776252f16ccacb23fb63d83223186b8cd55f8b/editor/src/maria/commands/prose.cljs), all of them voluntarily choosing to tie the millstone of this impoverished format around their necks despite serious attempts to rethink the combination of code and prose. 

Why do we use it? Because one of [Apple's court intellectuals decided it was convenient for him?](https://daringfireball.net/projects/markdown/) 

> _How is Markdown innovative exactly?_
>
> _It took ideas from the 70s, dropped the interesting parts, and was hailed as a revolutionary approach to marking up documents. Ie, the past 30 years of computing have been about narrowing the interface between programmer and computer to the equivalent of a straw (everything as text!) and then try to build an entire system around that._

[Spotted on Hacker News](https://news.ycombinator.com/item?id=16230676), the only reasonable response to someone calling Markdown "a triumph of programmer ergonomics."

Every system built atop Markdown will invariably have some ad-hoc and kludgy method of attempting to recapture some part of the structure that emerges from text authored in markdown, and it will be different from every other one because Markdown is blind to structure in all but the most basic of ways. In that regard it is very similar to "plain-text configuration" tools like [YAML](/against-metadata.html), which have all kinds of templating engines bolted on to them to overcome the limitations of a flat-file key-value store.

> _Everyone already knows markdown! It's fast and easy!_

Just be aware of what you're giving up as an author in pursuit of that, and what you may be imposing on yourself later on down the line if you want to overcome these constraints.

_yes, there's no small irony in the fact that the [source code for this post](https://gitlab.com/respatialized/respatialized.gitlab.io/blob/master/content/not-a-tree.md) is currently written in Markdown. It is indeed fast and easy to get started writing with it, but I'd largely attribute that to path dependence (and the fact that my particular parser leaves the `<div>` tags I've littered throughout this post intact, which is an accident of choosing to use `perun` and thereby `flexmark-java`) rather than the virtues of the format itself. I have every intention of changing the authoring tool I use into something semantically richer, but I had to get my resistance to the format on paper first._


<span class="f2 b">Extensible Textual Notation, part 4</span>
<span class="f4">2019-12-29</span>

<div class = "flex">
<div class="fl w-40">

![thinking about things](media/thinking-about-things.jpg)
</div>

<div class="fl f3 pl3 w-40">

_"I read relentlessly. I don’t do any programming not directed at making the computer do something useful, so I don’t do any exercises. I try to spend more time thinking about the problem than I do typing it in."_

[Rich Hickey](http://web.archive.org/web/20160918041754/http://codequarterly.com/2011/rich-hickey/)

</div>
</div>

_inspiration for what the medium should make possible, and for my prose-first approach to thinking about it: Bret Victor's laptop sticker and Rich Hickey's mindset; the antithesis of the "shut up and show me the code" brogrammer ethos_

<span class="f3 b">Beyond plain text: storing prose within `datahike`   </span>

Here's a [background post on the Datahike internals](https://blog.datopia.io/2018/11/03/hitchhiker-tree/) for context about how the hitchhiker B-tree structure allows for self-balancing and efficient updates that "hitchhike" on queries.

Here's another on using the `dat://` protocol for [P2P replication of the data stored in a Datahike instance](https://lambdaforge.io/2019/12/08/replicate-datahike-wherever-you-go.html). It serves as a useful starting point for getting a Datahike instance up and running.

Here's what would be a useful starting point for programmatic prose parsing: including a quotation in a piece of prose writing that gets parsed as a separate component and then added to a global list of quotations maintained by the text parser, with a link back to its original positional context within the piece of writing that quoted it.
