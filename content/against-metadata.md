---
title: Against Metadata
date-created: 2019-08-16
topics: programming
---
<span class="f2 bold">Frustrations with YAML</span>
<span class="f4">2019-08-16</span>

This is a rant that will probably get me yelled at by librarians, but I feel it strongly from the perspective of a programmer. It's a visceral response, so don't take it too seriously; I may not even fully believe it on reflection.

Anyway, here goes: the distinction between data and metadata is a false dichotomy.

Here's an archetypical example of the distinction as usually drawn: digital photography.

Data: RGB pixel values (or, if you're shooting RAW: a mapping of voltages to sensors).
Metadata: the date the photograph was taken, aperture, location, etc.

In short, most of what we actually care about is in the metadata. These offer the ability to understand the process by which the photograph was created, its temporal context, and (in the case of a geotag) what it actually depicts. The only meaningful basis I can see for drawing this distinction is that data is what matters to the machine, and the metadata is what matters to us.

But the _data_, the _real stuff_, out of which the photograph is composed, only has meaning due to us. This might seem obvious, but I think it has important consequences. Why did I write this? Because I don't like YAML. YAML is designed for metadata, and metadata alone. But now it does a lot more than what was initially asked of it. It was originally used to _describe_; now it is expected to _perform_. What was once "Yet Another Markup Language" is now "YAML ain't markup language." 

Sooner or later, the values in a YAML file are not metadata. They are data, and should be managed like data. When your YAML file defines the various libraries that are [constituent parts of your Docker image](https://docs.docker.com/compose/), across its revision history it defines the successive states of your software stack. When it defines the [health checks to your Kubernetes applications](https://github.com/helm/charts/blob/12754f06cee246f7e89d0ffbfa66cbadb644e443/stable/mysql/templates/deployment.yaml#L113), it provides a window into all the times the stack failed to work and was made to work again. These are all hugely valuable sources of context for how real software gets made. Shoving it into underdesigned storage formats (or worse, spreading those values across several files with templating languages that break when using hyphens in names) and forcing people to do microsurgery on git revisions to reconstruct how the values have changed over time is tantamount to throwing this history away, because git commit hashes are inadequate to the task anyway. These two values were changed by the same commit? Cool. Do they depend on one another, or can I change one back to its previous state without altering the other? A "configuration as code" practice does not answer this question. 

I feel strongly about this because it seems like programmers are doomed to reinvent wheels, over and over again. You might think that managing configuration in YAML will force you to "keep things simple." But sooner or later, the complexity spills over.

I doubt I'm the first person to notice these shortcomings; they likely motivated the development of the templating tools that make it easier to more flexibly produce YAML files. It would be nice to not hard-code every JSON value, right? Maybe we can create simple expressions that allow us to swap in the results of simple arithmetical expressions, so we can increment the build number easily. Also, string interpolation and concatenation would help us name things more easily, too. [What a concept!](https://jsonnet.org) So, the config now consists of a sequence of referentially transparent expressions that yield values based on the application of simple rules of substitution and a few special forms? Wait, [I think I've seen something like this before...](https://en.wikipedia.org/wiki/Lisp_(programming_language))

Metadata is data. The same tools we use to process data efficiently, store it reliably, and link it together should be used with it. Doing things declaratively is a laudable goal, but those taking it on must recognize how [complex](https://en.wikipedia.org/wiki/Datalog) [declarative](https://en.wikipedia.org/wiki/SQL) [programs](https://en.wikipedia.org/wiki/MiniKanren) actually are. Otherwise, they'll just sweep the existing complexity of the subject under the rug.

Previously:

- [The Configuration Complexity Clock](https://mikehadlow.blogspot.com/2012/05/configuration-complexity-clock.html)
- ["At least XML had schemas..."](https://twitter.com/dr_c0d3/status/1040092903052378112) 

<span class="f2 bold">Thoughts on Fossil</span>
<span class="f4">2019-11-16</span>

[`fossil`](https://www.fossil-scm.org/) is an alternative to git developed for and atop the sqlite database. When I first read about it, I was working through the configuration of automated build snapshots using CI tools and wanted a place to reliably store information about the dates of the failed builds and the underlying causes. I thought that a DVCS with an integrated database would be a ideal for this: I could add a "builds" table with the dates of successful and failed builds and track this history right with the project. More generally it would help tremendously with all the per-repository configuration management that currently gets shoved into YAML files: instead of flat key-value files,  you could record the configuration values in the database backing the project with a consistent relational model.

As far as I can tell, this sort of thing isn't possible "out of the box" in `fossil`, but it would be a fantastic concept for a VCS that returns to the more fully expanded notion of "SCM": _software configuration management_ instead of _source control management_. 
