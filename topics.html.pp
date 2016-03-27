#lang pollen

◊(local-require "util-topics.rkt" pollen/template pollen/pagetree)
◊(define main-pagetree (dynamic-require "index.ptree" 'doc))

<!DOCTYPE html>
<html lang="en" class="gridded">
    <head>
        <meta charset="utf-8">
        <title>Topics (The Notepad)</title>
        <link rel="stylesheet" href="/styles.css" media="screen" charset="utf-8">
    </head>
    <body>
        <header class="main">
            <p><a href="/" class="home">The Notepad</a>’s full of ballpoint hypertext</p>
            <nav>
                <ul>
                    <li><a href="/topics.html">Topics</a></li>
                    <li><a href="/books.html">Books to Read</a></li>
                    <li><a href="/about.html">About</a></li>
                </ul>
            </nav>
        </header>
        <section class="main">
            ◊; Get two lists: one of all index links in the current pagetree,
            ◊; another of all the unique headings used in the first list.
            ◊(define tlinks (collect-index-links (children 'index.html main-pagetree)))
            ◊(define topics (index-headings tlinks))
            <dl class="topic-list">
                ◊(define (ddlink lnk) `(dd ,lnk))
                ◊(->html (apply append (for/list([topic topics])
                                         `((dt (a [[href ,(string-append "#" topic)]
                                                   [name ,topic]] ,topic))
                                           ,@(map ddlink (match-index-links topic tlinks))))))
            </dl>
        </section>
        <footer class="main">
            <p>RSS &middot; <a href="mailto:joel@jdueck.net">joel@jdueck.net</a> &middot; <a href="https://twitter.com/joeld">@joeld</a>
            <br>Produced with <a href="http://pollenpub.com">Pollen</a>. Source code on Github. Valid HTML5 + CSS. </p>
        </footer>
    </body>
</html>