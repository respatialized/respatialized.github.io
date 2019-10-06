#lang pollen
◊(define-meta title "Clojure is My TI-83, part 1")
◊(define-meta published "2019-05-20")
◊(define-meta topics "math,clojure")
◊c[#:span "1-3" #:span-s "row"]{
I was brushing up on some statistics using the ◊link["https://openstax.org/details/books/introductory-statistics"]{OpenStax Introductory Statistics} free textbook. The exercises call for punching in functions on a TI-83 to generate random numbers and compute correlation coefficients. I have a much more powerful tool at my disposal: Clojure. 
◊blockquote{LISP is a high-level language, but you can still feel the bits sliding between your toes.} Guy Steele

Clojure is an unusually expressive programming language. Its minimal syntax and aggressive polymorphism surprised me with what my intuition came up with on the fly. Here's an example. I'm not super familiar with the standard library or the idiomatic way of doing things, so I was trying to figure out how to filter a vector based on whether the elements were contained in a set.}
◊c[#:span "1-3" #:span-s "row"]{
◊pre{◊code[#:class "language-klipse"]{
; uncomment the lines one by one to get a better idea:
;(#{3 4 5} 4)
;(#{3 4 5} 2)
;(map #{3 4 5} [1 2 3 4 5])
(filter #{3 4 5} [1 2 3 4 5])}}}
◊c[#:span "2" #:span-s "row" #:class "small"]{
Just like that. As soon as the idea came to me I found that I could call a data structure as a function and filter the data I wanted with no syntactic overhead. This expressive power means that it basically takes me as long to ◊em{implement} a function as it does to look up how it might be done on a graphing calculator.}
◊c[#:span "1-3" #:span-s "row"]{Further experiments and exercises confirmed that I could indeed feel the data sliding between my toes.}