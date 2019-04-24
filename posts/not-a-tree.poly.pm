#lang pollen

◊(define-meta title "This Website Is Not a Tree")
◊(define-meta published "2019-03-10")
◊(define-meta topics "meta,writing")

◊c[#:span "8"]{

◊section{Preliminaries}

◊blockquote{It must be emphasized, lest the orderly mind shrink in horror from anything that is not clearly articulated and categorized in tree form, that the idea of overlap, ambiguity, multiplicity of aspect and the semilattice are not less orderly than the rigid tree, but more so. They represent a thicker, tougher, more subtle and more complex view of structure.}
Christopher Alexander, ◊link["https://www.patternlanguage.com/archive/cityisnotatree.html"]{"A City Is Not A Tree"}
◊numbered-note{essential reading.}

abcdefghijklmnopqrstuvwxyzabcdefghijklmn

This is the first post of respatialized, a website about actual and potential spaces. Part of the reason it took me so long to launch it is because nearly every static site generator forces your writing into a tree-like structure. Only one lets you extend the site generation methods to reflect your own ideas: Matthew Butterick's ◊a[#:href "https://docs.racket-lang.org/pollen/" #:class "tech"]{pollen}. Because I want to combine the sequential and additive writing style of a blog with the associational and iterative nature of a wiki, this was the only choice.

It would have taken me even longer to get started if Joel Dueck hadn't already done the excellent work of creating ◊a[#:href "https://github.com/otherjoel/thenotepad" #:class "tech"]{thenotepad}, which includes functions to produce many of the things we expect from blogs, like sequential indices and RSS feeds, and many that we should expect, but don't (like the ability to generate a PDF from the blog). The code that generates this blog is forked from ◊a[#:href "https://github.com/otherjoel/thenotepad" #:class "tech"]{thenotepad} and licensed under the MIT License.

}