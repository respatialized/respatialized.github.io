#lang pollen

◊(define-meta title "Annotating the blog's own source")
◊(define-meta published "2019-04-22")
◊(define-meta topics "meta,writing")


◊c[#:span "row"]{◊section{Intro}}
◊c[#:span "row"]{
This is an example of using Pollen to understand Pollen and Raster.

First, we'll establish a layout.
}
◊c[#:span "row"]{◊section{Layout demo}

A good use case for side-by-side columns is annotating code.
}
◊c[#:span "3" #:span-s "row"]{
this is the annotation for our function in two cols

the code can be seen to the right

using the columns, we can comment on the code to the side of what we're writing
} ◊c[#:span "3" #:span-s "row" #:class "codeblock"]{
◊code{
(define (sample-func arg1 arg2 rest)

(blah (blah-blah (do-something (map some-func rest))))

(define res1 (sample-func "a" "b" '(1 2 3 4)))
}}
◊c[#:span "row"]{
◊section{Nota Bene}

if you want the columns to work properly with the pollen defaults, you shouldn't put line breaks between the columns you're demarcating.
}
