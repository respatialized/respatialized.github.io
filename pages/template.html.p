◊(local-require pollen/private/version)
◊(init-db)
◊(define-values (doc-body comments) (split-body-comments doc))
◊(define doc-body-html (->html (cdr doc-body)))
◊(define doc-header (->html (post-header here metas)))
◊(cond [(select-from-metas 'published metas) (save-post here metas doc-header doc-body-html)])
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>◊(select-from-metas 'title here)</title>

        <link rel="stylesheet" href="/raster.css" media="screen">
        <link rel="stylesheet" type="text/css" href="/codemirror.css">
        <link rel="stylesheet" type="text/css" href="/cmtheme.css">
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

        <script>
         window.klipse_settings = {
             selector: '.language-klipse', // css selector for the html elements you want to klipsify
             codemirror_options_in: {
                 lineNumbers: true,
                 styleActiveLine: true,
                 lineWrapping: true
             },
             codemirror_options_out: {
                 lineNumbers: true,
                 styleActiveLine: true,
                 lineWrapping: true
             }
         };
        </script>
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
        <c span="4" span-s="row" class="post-header">
         ◊doc-header
        </c> 
         <br>
         ◊doc-body-html
         <br>
         <c span=row> ◊(->html comments) </c>
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
        <script src="https://storage.googleapis.com/app.klipse.tech/plugin/js/klipse_plugin.js">
        </script>
    </body>
</html>
