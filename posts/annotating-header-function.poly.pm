#lang pollen

◊(define-meta title "Annotating the blog's own source")
◊(define-meta published "2019-04-22")
◊(define-meta topics "meta,writing")



◊section{Intro}

This is an example of using Pollen to understand Pollen.

First, we'll establish a layout.

◊section{Layout demo}

◊c[#:span "3"]{
this is the annotation for our function in two cols
the code can be seen to the right
using the columns, we can comment on the code to the side of what we're writing
} ◊c[#:span "3"]{
◊code{
(define (sample-func arg1 arg2 rest) (map some-func rest))

(define res1 (sample-func "a" "b" '(1 2 3 4)))
}
}





