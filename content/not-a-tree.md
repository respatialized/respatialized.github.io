---
title: This Website Is Not a Tree
published: "2019-03-10"
topics: meta,writing
---

<blockquote class="bl bw2 b--green w-60">
<p class="f3 pl2 bold">It must be emphasized, lest the orderly mind shrink in horror from anything that is not clearly articulated and categorized in tree form, that the idea of overlap, ambiguity, multiplicity of aspect and the semilattice are not less orderly than the rigid tree, but more so. They represent a thicker, tougher, more subtle and more complex view of structure.</p>

<span class="pl2">Christopher Alexander, [A City Is Not A Tree](https://www.patternlanguage.com/archive/cityisnotatree.html) </span>
<div class="f7 tr">essential reading.</span>
</blockquote>

<span class="f2 bold">Preliminaries</span>
<span class="f4">2019-03-10</span>

This is the first post of respatialized, a website about actual and potential spaces. Part of the reason it took me so long to launch it is because nearly every static site generator forces your writing into a tree-like structure. Only one lets you extend the site generation methods to reflect your own ideas: Matthew Butterick's [`pollen`](https://docs.racket-lang.org/pollen/). Because I want to combine the sequential and additive writing style of a blog with the associational and iterative nature of a wiki, this was the only choice.

It would have taken me even longer to get started if Joel Dueck hadn't already done the excellent work of creating  [`thenotepad`](https://github.com/otherjoel/thenotepad), which includes functions to produce many of the things we expect from blogs, like sequential indices and RSS feeds, and many that we should expect, but don't (like the ability to generate a PDF from the blog). The code that generates this blog is forked from  [`thenotepad`](https://github.com/otherjoel/thenotepad) and licensed under the MIT License.

<span class="f2 bold">Extensible Textual Notation</span>
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

<span class="f2 bold">Extensible Textual Notation, part 2</span>
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
