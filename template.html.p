◊(local-require pollen/private/version)
◊(init-db)
◊(define-values (doc-body comments) (split-body-comments doc))
◊(define doc-body-html (->html (cdr doc-body)))
◊(define doc-header (->html (post-header here metas)))
◊(cond [(select-from-metas 'published metas) (save-post here metas doc-header doc-body-html)])
<!DOCTYPE html>
<html lang="en" class="gridded">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>◊(select-from-metas 'title here)</title>
        <link rel="stylesheet" href="/raster.css" media="screen">
        ◊|meta-favicons|
    </head>
    <body>
      <header class="main">
        <grid columns="6" columns-s="4">
          <c span=1-4 span-s=row>
            <p><a href="/index.html" class="h1">Respatialized</a></p>
        actual / potential spaces
          </c>
          <c span=5-6 span-s=row >
          <grid columns=2 class="compact">
              <c span=1><a href="/topics.html">Topics</a></c>
              <c span=1><a href="/books.html">Books</a></c>
              <c span=1><a href="/about.html">About</a></c>
              <c span=1><a href="/feed.xml" class="rss">RSS Feed</a></c>
          </grid>
          </c>
        </grid>
      </header>

        <article>
            ◊doc-header
            ◊doc-body-html
            ◊(->html comments)
        </article>
        <footer class="main">
            <ul>
                <li><a class="rss" href="/feed.xml">RSS</a></li>
                <li><a href="mailto:info@respatialized.net">info@respatialized.net</a></li>
                <li><a href="https://github.com/respatialized/">Github</a></li>
                </ul>
        </footer>
    </body>
</html>
