---
title: Clojure is My TI-83
published: 2019-05-20
updated: 2019-07-07
topics: math, clojure
---

### Part 1

<div class="fl-w 50">

I was brushing up on some statistics using the [OpenStax Introductory Statistics]("https://openstax.org/details/books/introductory-statistics") free textbook. The exercises call for punching in functions on a TI-83 to generate random numbers and compute correlation coefficients. I have a much more powerful tool at my disposal: Clojure. 

> LISP is a high-level language, but you can still feel the bits sliding between your toes. 

Guy Steele

Clojure is an unusually expressive programming language. Its minimal syntax and aggressive polymorphism surprised me with what my intuition came up with on the fly. Here's an example. I'm not super familiar with the standard library or the idiomatic way of doing things, so I was trying to figure out how to filter a vector based on whether the elements were contained in a set.
</div>
<div class="language-klipse code fl-w 30">
; uncomment the lines one by one to get a better idea:
;(#{3 4 5} 4)
;(#{3 4 5} 2)
;(map #{3 4 5} [1 2 3 4 5])
(filter #{3 4 5} [1 2 3 4 5])
</div>
<div class="fl-w 30">

Just like that. As soon as the idea came to me I found that I could call a data structure as a function and filter the data I wanted with no syntactic overhead. This expressive power means that it basically takes me as long to _implement_ a function as it does to look up how it might be done on a graphing calculator.
</div>
<div class="fl-w 50">
Further experiments and exercises confirmed that I could indeed feel the data sliding between my toes.
</div>
### Part 2: Tidying Data

In order to do my [self-assigned math homework]("https://cnx.org/contents/MBiUQmmY@23.31:i_O99VEg/1-6-Sampling-Experiment"), I needed to ensure the data provided are in an appropriate format. Here's how the book provides the data.

<script>
    window.klipse_settings = {selector: '.language-klipse'};
</script>
<script src="http://app.klipse.tech/plugin/js/klipse_plugin.js" async></script>
<link rel="stylesheet" type="text/css" href="http://app.klipse.tech/css/codemirror.css" media="print" onload="this.media='all'">

