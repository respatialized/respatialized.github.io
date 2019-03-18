#lang pollen
◊(require racket/list pollen/pagetree pollen/template pollen/private/version)
<!DOCTYPE html>
<html lang="en" class="gridded">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Respatialized</title>
        <link rel="stylesheet" href="/styles.css" media="screen">
        ◊|meta-favicons|
    </head>
    <body>
        <header class="main">
            <p><a href="/" class="home">Respatialized</a> <span class="tagline">is not a tree</span></p>
            <nav>
                <ul>
                    <li class="current-section"><a href="/topics.html">Topics</a></li>
                    <li><a href="/books.html">Books to Read</a></li>
                    <li><a href="/about.html">About</a></li>
                    <li><a href="/feed.xml" class="rss" title="RSS feed">RSS Feed</a></li>
                </ul>
            </nav>
        </header>
        
        ◊for/s[post (latest-posts 10)]{
           <article>
           ◊(hash-ref post 'header_html)
           ◊(hash-ref post 'html)
           </article>
           <hr>
        }

        <footer class="main">
            <ul>
                <li><a href="/feed.xml" class="rss" title="RSS feed">RSS Feed</a></li>
                <li><a href="mailto:info@respatialized.net">info@respatialized.net</a></li>
                <li><a href="https://github.com/respatialized/">Github</a></li>
            </ul>
        </footer>
    </body>
</html>
