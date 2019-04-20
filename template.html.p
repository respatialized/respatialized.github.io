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
    </body>
</html>
