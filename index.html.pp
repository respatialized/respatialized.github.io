#lang pollen
◊(require racket/list pollen/pagetree pollen/template pollen/private/version)
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta name="generator" content="Racket ◊(version) + Pollen ◊|pollen:version|">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Respatialized</title>
        ◊|meta-favicons|

        <link rel="stylesheet" href="/raster.css" media="screen">
        <link rel="stylesheet" type="text/css" href="/codemirror.css">
        <link rel="stylesheet" type="text/css" href="/cmtheme.css">

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
                    <span class="tagline">is not a tree</span>
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
      ◊for/s[post (latest-posts 10)]{
      <c span="4" span-s="row" class="post-header">
          ◊(hash-ref post 'header_html)
      </c>
      <br>
           ◊(hash-ref post 'html)
           <hr>
           }

      <footer class="main">
          <c span="row" class="global-footer">
            <ul class="compact">
                <li><a href="/feed.xml" class="rss" title="RSS feed">RSS</a></li>
                <li><a href="mailto:info@respatialized.net">info@respatialized.net</a></li>
                <li><a href="https://github.com/respatialized/">Github</a></li>
            </ul>
          </c>
      </footer>
        </grid>
        <script src="https://storage.googleapis.com/app.klipse.tech/plugin/js/klipse_plugin.js">
        </script>
    </body>

  <!-- ---- internal only -- not part of raster ---- -->
  <script>(function(){
let ua = navigator.userAgent
if (ua.indexOf("Chrome/") != -1) {
  document.documentElement.classList.add('chrome')
} else if (ua.indexOf("Firefox/") != -1) {
  document.documentElement.classList.add('firefox')
}
let css = `
html.inverted { background: #010101; color: white }
html.inverted hr { background: white }
html.size-mode-relative {
  --fontSize: calc(100vw / 80);
  --hrThickness: 0.17rem;
}
.settings c:nth-child(2n+2) {
  font-feature-settings:'ss02' 1;
  user-select:none;
}
.baselineBeacon {
  height: var(--baseline);
  overflow: hidden;
  display: none;
  position: absolute;
}
`.trim()
let style = document.createElement('style')
style.appendChild(document.createTextNode(css))
document.head.appendChild(style)
let link = document.createElement('link')
link.rel = "stylesheet"
link.href = "/raster.debug.css"
document.head.appendChild(link)
let baselineBeacon = document.createElement('div')
baselineBeacon.className = 'baselineBeacon'
baselineBeacon.innerText = 'x'
document.body.appendChild(baselineBeacon)
function fmtnum(n) {
  let s = n.toFixed(2)
  if (s.substr(-3) == '.00') {
    s = s.substr(0, s.length-3)
  }
  return s
}
function setLabel(id, value) {
  let label = document.getElementById(id)
  label && (label.innerText = value)
}
let tapevent = 'PointerEvent' in window ? 'pointerdown' : 'click'
function bindTapableOption(msgname, fn) {
  let label = document.getElementById(msgname + '-msg')
  label && label.parentElement.addEventListener(tapevent, fn)
}
function updateComputedValueLabels() {
  let cs = getComputedStyle(baselineBeacon)
  let baseline = parseFloat(cs.height)
  let fontSize = parseFloat(cs.fontSize)
  let lineHeight = parseFloat(cs.lineHeight)
  setLabel('baseline-value-msg', fmtnum(baseline) + ' dp')
  setLabel('fontsize-value-msg', fmtnum(fontSize) + ' dp')
  setLabel('lineheight-value-msg', fmtnum(lineHeight) + ' dp')
}
function updateDebugModeLabel() {
  let on = document.body.classList.contains('debug')
  setLabel('debug-mode-msg', on ? 'On' : 'Off')
}
function updateBaseGridLabel() {
  let on = document.body.classList.contains('show-base-grid')
  setLabel('base-grid-msg', on ? 'On' : 'Off')
}
function updateInvertedLabel() {
  let on = document.documentElement.classList.contains('inverted')
  setLabel('inverted-msg', on ? 'On' : 'Off')
}
function updateSizeModeLabel() {
  let rel = document.documentElement.classList.contains('size-mode-relative')
  setLabel('size-mode-msg', rel ? 'Viewport' : 'Constant')
}
function toggleDebugMode() {
  document.body.classList.toggle('debug')
  updateDebugModeLabel()
}
function toggleBaseGrid() {
  document.body.classList.toggle('show-base-grid')
  updateBaseGridLabel()
}
function toggleInvertedMode() {
  document.documentElement.classList.toggle('inverted')
  updateInvertedLabel()
}
function toggleSizeMode() {
  document.documentElement.classList.toggle('size-mode-relative')
  updateSizeModeLabel()
  updateComputedValueLabels()
  setTimeout(updateComputedValueLabels, 10)
}
bindTapableOption('debug-mode', toggleDebugMode)
bindTapableOption('base-grid', toggleBaseGrid)
bindTapableOption('inverted', toggleInvertedMode)
bindTapableOption('size-mode', toggleSizeMode)
function handleKeyPress(key) {
  switch (key) {
    case "d": case "D":  toggleDebugMode();    return true
    case "g": case "G":  toggleBaseGrid();     return true
    case "i": case "I":  toggleInvertedMode(); return true
    case "s": case "S":  toggleSizeMode();     return true
  }
  return false
}
document.addEventListener('keypress', ev => {
  if (!ev.metaKey && !ev.ctrlKey && !ev.altKey && handleKeyPress(ev.key)) {
    ev.preventDefault()
    ev.stopPropagation()
  }
}, {passive:false, capture:true})
let resizeTimer = null
window.addEventListener('resize', ev => {
  if (resizeTimer === null) {
    resizeTimer = setTimeout(() => {
      resizeTimer = null
      updateComputedValueLabels()
    }, 100)
  }
})
// main
updateDebugModeLabel()
updateBaseGridLabel()
updateInvertedLabel()
updateSizeModeLabel()
updateComputedValueLabels()
})();</script>

</html>
