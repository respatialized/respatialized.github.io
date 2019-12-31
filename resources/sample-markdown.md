# General Idea

<div>

What I'm trying to do by extending markdown and eventually creating my own `pollen`-like markup syntax is to do more to capture the structure that arises naturally out of the process of writing prose text.

</div>

For example, one goal could be to parse out the information that's displayed in tabular format and preserve its structure.

| Design goal                         | Inspirations/prior art                       | Completed? |
| ----------------------------------- | -------------------------------------------- | ---------- |
| capturing structure from plain text | org-mode                                     | false      |
| structural inference using queries  | datomic, datahike, datascript                | false      |

Overall the system that I'm trying to make is a system that supports iterative refinement of code and the ideas it represents alongside one another as my understanding of a problem changes. Here's a passage that represents what I'm talking about:

>A good class project for undergraduates who have not become too tainted with either the commercial or research computing milieu, is to have them design a computer system for a think tank such as RAND or the Institute for Advanced Study at Princeton. It is a delightfully nebulous question, since they quickly realize it will be impossible for them to even discover what the majority of the thinkers are doing. Indeed, many of the researchers will not know themselves or be able to articulate that state of mind of just feeling around. It is at this point that a wide philosophical division appears in the students. Almost all of them agree that there is really nothing that they can do for the scientists. The more engineering-minded of the students, seeing no hard and fast solution, stop there. The rest, who are somewhat more fanciful in their thoughts, say ...maybe 'nothing' is exactly the right thing to deliver, providing it is served up in the proper package. They have articulated an important thought. Not being able to solve any one scientist's problems, they nevertheless feel that they can provide tools in which the thinker can describe his own solutions and that these tools need not treat specifically any given area of discourse. 
>The latter group of students has come to look at a computing engine not as a device to solve differential equations, nor to process data in any given way, but rather as an abstraction of a well-defined universe which may resemble other well-known universes to any necessary degree. When viewed from this vantage point, it is seen that some models may be less interesting than the basic machine (payroll programs). Others may be more interesting (simulation of new designs, etc.). Finally, when they notice that the power of modeling can extend to simulate a communications network, an entirely new direction for providing a system is suggested.
>While they may not know the jargon and models of an abstruse field, yet possibly enough in general of human communications is known for a meta-system to he created in which the specialist himself may describe the symbol system necessary to his work. In order for a tool such as this to be useful, several constraints must be true.
> 1) The communications device must be as available (in every way) as a slide rule.
> 2) The service must not be esoteric to use. (It must be learnable in private.)
> 3) The transactions must inspire confidence. (Kindness should be an integral part.)

Including a long quote serves as a perfect example of the frustrating limitations of markdown: these are all parsed as separate lines, each with a blockquote in it, instead of a discrete semantic unit, making capturing structure very difficult using its syntax.

## Another thing

<div>

Currently I use `<div>` tags to make up for markdown's shortcomings. Do any "off the shelf" clojure parsers keep them in the result? More broadly, what do the various parsers bring to the table?

</div>


| library              | div tags | tables |
| -------------------- | -------- | ------ |
| `markdown-to-hiccup` | true     | false  |
| `markdown-clj`       | true     | true   |
| `hickory`            | true     | true   |
| `commonmark-hiccup`  | false    | false  |


Looks like it's going to be `markdown-clj` + `hickory` for now! (as long as I can remember to put in the right number of hyphens in the tables, yeesh)
