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

However, I agree wholeheartedly with Matthew Butterick when he argues that Markdown is a constraining environment in which to write, especially if you're looking to write a sustained treatment of a topic which usually generates a deep and rich collection of self and cross references and its own conventions for referring back to subtopics organically over time. Markdown supports the lowest level of this: links. Anything else, you're on your own, but with a completely restricted method of manipulating the input texts.

Also, sometimes I want to contextually distinguish textual elements using CSS and I want to do it without rewriting my markdown parser. I currently do this by littering my markdown posts with `<div class="...">` tags, which is kludgy and offers no way of systematically changing the classes applied to the textual element apart from doing find-replace on all of them with `grep`.

The `#lang pollen` directive provides a beautiful way of letting prose be prose while still letting you access the full power of a programming lanugage whenever you need it via the lozenge (`◊`) special character. 

What I'm looking for, basically:
<div class="ws-normal code">
I'd like the ability to embed ◊(link hiccup "https://github.com/weavejester/hiccup")/clojure data structures into my program. They can either be data (see below) or functions called at render time that evaluate into data (see above).

[:em {:class "topic"} Extensibility]

Clojure already supports this notion in its canonical representation of data, extensibile data notation. I want to bring it to textual information, and maybe, HTML Canvas objects as well. The full power of a programming language means that we can flexibly switch between graphical and textual representations, something that pollen doesn't yet support.
</div>

This approach also acts as a force-multiplier on immutable, compositional CSS tools like `tachyons` or `tailwind`, because it brings the power of Clojure into the tool you're using to write the text, which in turn leverages tools like `tachyons` to apply a unified style to what you're writing using inline, simple notation.

Other examples in this space:

- idyll
- mdx

Both of these are built atop Javascript, and are more focused on interactivity for users than on procedural generation of text at write-time. Clojure(script)'s homoiconicity makes it ideally suited for both purposes- it can be used to generate interactive programs as well as any other form of data you'd want to display. I'm personally more interested in the latter, right now.
