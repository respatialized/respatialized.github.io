---
title: Extensible Textual Notation
published: 2019-11-13
topics: clojure, documents, notation
---

I recently switched from `pollen` to `perun`. `perun`'s model of publishing everything via a composable collection of `boot` tasks encapsulates everything that I want from `pollen`'s organizational and compositional capabilities. `pollen`'s pagetrees can be recreated by mapping and filtering the sequential collections of `hiccup` data structures `perun` generates, and applying those transformations to generic collections comes more readily to me than creating `.ptree` files (the Clojure refrain, it's just data, etc.). My artwork and other content is also written in and generated using Clojure, so I don't want to have to drop into a different language that I don't know as well just to get it out. For me, the ability to iterate quickly depends on low friction and the power of simplicity: `boot`'s Swiss army knife approach matches that perfectly. 

However, I agree wholeheartedly with Matthew Butterick when he argues that Markdown is a constraining environment in which to write, especially if you're looking to write a sustained treatment of a topic which usually generates a deep and rich collection of self and cross references and its own conventions for referring back to subtopics organically over time. Markdown supports the lowest level of this: links. Anything else, you're on your own, but with a completely restricted method of manipulating the input texts.

Also, sometimes I want to contextually distinguish textual elements using CSS and I want to do it without rewriting my markdown parser. I currently do this by littering my markdown posts with `<div class="...">` tags, which is kludgy and offers no way of systematically changing the classes applied to the textual element apart from doing find-replace on all of them with `grep`.

The `#lang pollen` directive provides a beautiful way of letting prose be prose while still letting you access the full power of a programming lanugage whenever you need it via the lozenge (`◊`) special character. 

What I'm looking for, basically:
<div class=".flex-wrap">

```
I'd like the ability to embed ◊(link hiccup "https://github.com/weavejester/hiccup")/clojure data structures into my program. They can either be data (see below) or functions called at render time that evaluate into data (see above).

[:em {:class "topic"} Extensibility]

Clojure already supports this notion in its canonical representation of data, extensibile data notation. I want to bring it to textual information, and maybe, HTML Canvas objects as well. The full power of a programming language means that we can flexibly switch between graphical and textual representations, something that pollen doesn't yet support.

```
</div>

This approach also acts as a force-multiplier on immutable, compositional CSS tools like `tachyons` or `tailwind`, because it brings the power of Clojure into the tool you're using to write the text, which in turn leverages tools like `tachyons` to apply a unified style to what you're writing using inline, simple notation.


Other examples in this space:
- idyll
- mdx
