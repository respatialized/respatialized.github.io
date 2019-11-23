---
title: Notes on AI and labor
---

Here's some thoughts scattered around a linkdump on the topic of *AI and labor*:


We hear a lot more about the dazzling advancements in task-solving by AI than the resources required to achieve that feat. Moore's law is getting soaked by exponential increases in the amount of CPU time to achieve linear improvements in ability of a model to perform its tasks.
Here's an [article admitting this from OpenAI](https://openai.com/blog/ai-and-compute/).

> "We’re very uncertain about the future of compute usage in AI systems, but it’s difficult to be confident that the recent trend of rapid increase in compute usage will stop, and we see many reasons that the trend could continue. Based on this analysis, we think policymakers should consider increasing funding for academic research into AI, as it’s clear that some types of AI research are becoming more computationally intensive and therefore expensive."

Unsurprisingly, they conclude that the only real problem here is that research budgets aren't high enough.

To the extent that AI gets "smarter", it's largely due to the fact that we're getting better at throwing more resources at the problem. [And those resources are still very much physical.](https://www.technologyreview.com/s/613630/training-a-single-ai-model-can-emit-as-much-carbon-as-five-cars-in-their-lifetimes/)

> “In general, much of the latest research in AI neglects efficiency, as very large neural networks have been found to be useful for a variety of tasks, and companies and institutions that have abundant access to computational resources can leverage this to obtain a competitive advantage."

Back when I was younger, my idea of progress in AI would consist of individual computers doing better with less resources – but that doesn't describe the "state of the art" at all. The quotation from OpenAI is particularly glaring in light of these resource costs- they simply accept this massive expenditure of resources as a fact of life that everyone needs to come around to. 

AI researchers like games because they provide a bounded problem space with immediate feedback, and they can run as many iterations of an experiment as they have the money to pay for. François Chollet, creator of Keras, [argues that this is a complete mistake](https://arxiv.org/abs/1911.01547):

> "We argue that solely measuring skill at any given task falls short of measuring intelligence, because skill is heavily modulated by prior knowledge and experience: unlimited priors or unlimited training data allow experimenters to "buy" arbitrary levels of skills for a system, in a way that masks the system's own generalization power." 

In other words, the real world that intelligence has to operate in is not as generous. So how do we get that feedback in the real world, at the scale required for massive distributed models that operate on millions of novel observations per day to avoid overfitting? [Data factories](https://www.nytimes.com/2019/08/16/technology/ai-humans.html).

You've done some of this labor yourself. You've identified traffic lights, doors, license plates to prove your own humanity to a website. That very system is making that test meaningless; it is collecting the discrete inputs necessary to teach a machine to do the same thing. The "intelligence" of the AI system actually turns out to be an aggregation of human intelligences doing the absolute simplest and most repetitive forms of pattern matching and inference. The requirements of distributing the problem across many different people in many different places and obtaining a high signal to noise ratio from each data point flatten and erase the ambiguity and complexity of real-world situations, with predictably disastrous results.

Steve Dent, [Uber self-driving car involved in fatal crash couldn't detect jaywalkers](https://www.engadget.com/2019/11/06/uber-self-driving-car-fatal-accident-ntsb/)

> "When you invent the ship, you also invent the shipwreck; when you invent the plane you also invent the plane crash; and when you invent electricity, you invent electrocution." Paul Virilio


Vi Hart, ["Changing My Mind on Universal Basic Income and AI"](https://theartofresearch.org/ai-ubi-and-data/)

Hart refers more directly to the real problem with AI than a lot of other commentary I've read.

> I’m reminded of something from our VR research days. A company I won’t name was frequently making the VR news with big claims about something their proprietary software could do algorithmically. Mathematically this seemed suspect to me. We met with a room full of young men pitching their technology with all the jargon in the book, but unable to answer basic questions that only someone in their field would know to ask. Months later at an event we ran into someone who worked there. Her job was to do by hand the thing they claimed their algorithm could do. The entire rest of the industry pretended she didn’t exist, and the company truly believed that any day now, maybe if they just hire one more rockstar developer, they’d finish up this algorithm they were pretending to have, so she wasn’t worth much anyway.

She doesn't refer to it in these terms specifically, but the dynamic is familiar to socialists: the reduction of skilled to simple labor via the production of fixed capital. I also feel like it's worth mentioning that the genders of who's claiming to automate the work and who's actually doing it are particularly salient here.
