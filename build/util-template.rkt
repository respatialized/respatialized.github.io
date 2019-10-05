#lang racket

(require
  racket/date
  pollen/template
  pollen/core
  pollen/cache
  pollen/pagetree
  pollen/file
  "util-date.rkt"
  txexpr)

(provide (all-defined-out))

(define (pdfable? file-path)
  (string-contains? file-path ".poly"))

(define (pdfname page) (string-replace (path->string (file-name-from-path page))
                                       "poly.pm" "pdf"))

(define (sourcename page) (string-replace (path->string (file-name-from-path page))
                                       "poly.pm" "pollen.html"))
                                       
(define (source-listing p)
  (regexp-replace #px"(\\.html$)" (symbol->string p) ".pollen.html"))

(define (post-header post metas)
  (define updated (select-from-metas 'updated metas))
  (define updated-xexpr
    (cond [updated `((em "Updated " (time [[datetime ,updated]] ,(pubdate->english updated))) nbsp middot nbsp)]
          [else '("")]))

  (define topics (select-from-metas 'topics metas))
  (define topics-xexpr
    (cond [topics
           (define topic-listitems
             (map (λ(t) `(li (a [[href ,(string-append "/topics.html#" t)]] ,t)))
                  (string-split (regexp-replace* #px"\\s*,\\s*" topics ",") ",")))
           `(ul ,@topic-listitems)]
          [else ""]))
  (define timestamp-raw (select-from-metas 'published metas))
  (define timestamp
    (cond [timestamp-raw
           `("written " 
             (time [[datetime ,timestamp-raw]]
                   ,(pubdate->english timestamp-raw))
             nbsp middot nbsp)]
          [else '("")]))
  
  `(header
    (h1 (a [[href ,(string-append "/" (symbol->string post))]] ,(select-from-metas 'title metas)))
    (p ,@timestamp
       ,@updated-xexpr
       (a [[class "source-link"] [href ,(string-append "/posts/" (sourcename (select-from-metas 'here-path metas)))]]
          loz "Pollen" nbsp "source"))
    ,topics-xexpr))

(define (split-body-comments post-doc)
  (define (is-comment? tx)
    (and (txexpr? tx)
         (eq? (get-tag tx) 'section)
         (attrs-have-key? tx 'class)
         (string=? (attr-ref tx 'class) "comments")))

  (splitf-txexpr post-doc is-comment?))


