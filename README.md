Books On Tweet
==============

Forked from the *Unofficial Giter8 template for the Typelevel Stack*, this project is an ongoing self-tutorial project to learn Typelevel technologies while implementing a hilarious "Books on Tweet".

The project: Using public domain books in UTF-* .txt format from [Project Gutenberg](http://www.gutenberg.org/), parse them and tweet them, 140 characters at a time. 

The first project will be using Kafka's Metamorphosis so all parsing logic is somewhat hard-coded to this copy to start.

To Run
------

Because the g8 project's default http server is still contained in this project, there are two Main classes. To run in SBT (which by default will parse the classpath-contained copy of Metamormphosis and 'tweet' out via console the first 25 tweets):

    sbt runMain booksontweet.Runner

Why metamorphosis?
------------------

Several reasons:
1. it's relatively short (back-of-napkin calculations I vaguely recall from years ago resulted in a tweet every ~15 minutes completing the book within a week or two)
2. it amuses this author to know end to discover bugs while reading about a man who wakes up to discover he's become a bug. Extra funny during late coding nights
