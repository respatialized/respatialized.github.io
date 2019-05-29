#lang pollen

◊(local-require "util-topics.rkt" pollen/template pollen/pagetree pollen/private/version pollen/cache)
◊(define main-pagetree (cached-doc (string->path "index.ptree")))

<!DOCTYPE html>
<html lang="en" class="gridded">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <title>Respatialized: Topics</title>
        <link rel="stylesheet" href="/raster.css" media="screen">
        <!--
             /**
             * @license
             * MyFonts Webfont Build ID 3767650, 2019-05-28T22:01:07-0400
             *
             * The fonts listed in this notice are subject to the End User License
             * Agreement(s) entered into by the website owner. All other parties are 
             * explicitly restricted from using the Licensed Webfonts(s).
             *
             * You may obtain a valid license at the URLs below.
             *
             * Webfont: ArminGrotesk-Black by W Foundry
             * URL: https://www.myfonts.com/fonts/without-foundry/armin-grotesk/black/
             *
             * Webfont: ArminGrotesk-UltraBold by W Foundry
             * URL: https://www.myfonts.com/fonts/without-foundry/armin-grotesk/ultra-bold/
             *
             *
             * License: https://www.myfonts.com/viewlicense?type=web&buildid=3767650
             * Licensed pageviews: 10,000
             * Webfonts copyright: Copyright &#x00A9; 2018 by David Suid. All rights reserved.
             *
             * © 2019 MyFonts Inc
             */

        -->
        <link rel="stylesheet" type="text/css" href="/MyFontsWebfontsKit.css">

    </head>
    <body>
        <grid columns="8" columns-s="6">
            <c span="4" span-s="row">
                <header class="main">
                    <p><h1 class="page-title large"><a href="/index.html">Respatialized</a></h1></p>
                    <span class="tagline">actual/potential spaces</span>
                    <nav>
                        <ul class="compact">
                            <li class="current-section"><a href="/topics.html">Topics</a></li>
                            <li><a href="/books.html">Books</a></li>
                            <li><a href="/about.html">About</a></li>
                            <li><a href="/feed.xml" class="rss" title="RSS feed">RSS</a></li>
                        </ul>
                    </nav>
                </header>
            </c>
            <hr>
            <c span="row" class="topics"></c>
            ◊for/s[topic (topic-list)]{
            <c span="1-2">
                <span name="◊(car topic)" class="topic-name">◊(car topic)</span>
            </c>
            <c span="3-6" class="topic-posts">
                <ul>
                    ◊for/s[post (cdr topic)]{
                    <li><a href="/◊(list-ref post 0)">◊(list-ref post 1)</a></li>
                    }</ul>
            </c>
            <br>
            }
            <hr>

        <footer class="main">
            <c span="row" class="global-footer">
                <ul class="compact">
                    <li><a class="rss" href="/feed.xml">RSS</a></li>
                    <li><a href="mailto:info@respatialized.net">info@respatialized.net</a></li>
                    <li><a href="https://github.com/respatialized/">Github</a></li>
                </ul>
            </c>
        </footer>
        </grid>
    </body>
</html>
