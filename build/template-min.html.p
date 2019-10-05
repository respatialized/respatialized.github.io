◊(local-require pollen/private/version)
◊(define-values (doc-body comments) (split-body-comments doc))
◊(define doc-body-html (->html (cdr doc-body)))

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>◊(select-from-metas 'title here)</title>

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
    <grid columns="8" columns-s="6">
      <br>
      ◊doc-body-html
      <br>
    </grid>
</body>
</html>
