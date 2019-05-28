#!/bin/bash

FILENAME=$1

cat << EOF
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title>Pollen Source: $FILENAME</title>
  <link rel="stylesheet" href="/raster.css" media="screen" charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style type="text/css">
    pre.sourcebox {
        padding: 1em;
        border: solid 1px #efefef;
        background: #f5fff0;
        font-size: smaller;
        width: auto;
        }
    body { font-family: "League Mono"; }
    h1 { font-family: "League Mono"; }
    article { width: 90%; max-width: 55rem; }
    header { margin-bottom: 1em; font-family: "Armin Grotesk"; font-weight: 800;}
  </style>
</head>
<body>
    <article>
        <header>
          <p>
            <a href="/$(echo $FILENAME | sed -e "s/poly\.pm/html/g;s/html\.pm/html/g")">Back to page</a>
            &middot;
            <a href="https://github.com/respatialized/site/commits/public/$FILENAME">View history on Github</a>
          </p>
        </header>
        <div class="listing-filename">&#128196; $FILENAME</div>
        <pre class="fullwidth code sourcebox" style="white-space: pre-wrap;">$(perl -C -MHTML::Entities -pe 'encode_entities($_);' < $FILENAME)</pre>
    </article>
</body>
</html>
EOF
