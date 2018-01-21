Books On Tweet
==============

Forked from the *Unofficial Giter8 template for the Typelevel Stack*, this project is an ongoing self-tutorial project to learn Typelevel technologies while implementing a hilarious "Books on Tweet".

The project: Using public domain books in UTF-8 .txt format from [Project Gutenberg](http://www.gutenberg.org/), parse them and tweet them, 140 characters at a time. 

The first project will be using Kafka's Metamorphosis so all parsing logic is somewhat hard-coded to this copy to start.

To Run
------

Because the g8 project's default http server is still contained in this project, there are two Main classes. To run in SBT (which by default will parse the classpath-contained copy of Metamormphosis and 'tweet' out via console the first 25 tweets):

    sbt runMain booksontweet.Runner

Why metamorphosis?
------------------

Several reasons:
1. it's relatively short (back-of-napkin calculations I vaguely recall from years ago resulted in a tweet every ~15 minutes completing the book within a week or two)
2. it amuses this author to no end to discover bugs while reading about a man who wakes up to discover he's become a bug. Extra funny during late coding nights

Sample Output
-------------

~~~
Tweet #1: [138 chars]**Ch I** One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin.  He lay

Tweet #2: [138 chars] on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff

Tweet #3: [138 chars] sections.  The bedding was hardly able to cover it and seemed ready to slide off any moment.  His many legs, pitifully thin compared with

Tweet #4: [139 chars] the size of the rest of him, waved about helplessly as he looked. ¶ "What's happened to me?" he thought.  It wasn't a dream.  His room, a

Tweet #5: [139 chars] proper human room although a little too small, lay peacefully between its four familiar walls.  A collection of textile samples lay spread

Tweet #6: [134 chars] out on the table - Samsa was a travelling salesman - and above it there hung a picture that he had recently cut out of an illustrated

Tweet #7: [138 chars] magazine and housed in a nice, gilded frame.  It showed a lady fitted out with a fur hat and fur boa who sat upright, raising a heavy fur

Tweet #8: [137 chars] muff that covered the whole of her lower arm towards the viewer. ¶ Gregor then turned to look out the window at the dull weather. Drops

Tweet #9: [135 chars] of rain could be heard hitting the pane, which made him feel quite sad.  "How about if I sleep a little bit longer and forget all this

Tweet #10: [137 chars] nonsense", he thought, but that was something he was unable to do because he was used to sleeping on his right, and in his present state

Tweet #11: [139 chars] couldn't get into that position.  However hard he threw himself onto his right, he always rolled back to where he was.  He must have tried

Tweet #12: [139 chars] it a hundred times, shut his eyes so that he wouldn't have to look at the floundering legs, and only stopped when he began to feel a mild,

Tweet #13: [137 chars] dull pain there that he had never felt before. ¶ "Oh, God", he thought, "what a strenuous career it is that I've chosen! Travelling day

Tweet #14: [134 chars] in and day out.  Doing business like this takes much more effort than doing your own business at home, and on top of that there's the

Tweet #15: [136 chars] curse of travelling, worries about making train connections, bad and irregular food, contact with different people all the time so that

Tweet #16: [135 chars] you can never get to know anyone or become friendly with them.  It can all go to Hell!"  He felt a slight itch up on his belly; pushed

Tweet #17: [137 chars] himself slowly up on his back towards the headboard so that he could lift his head better; found where the itch was, and saw that it was

Tweet #18: [138 chars] covered with lots of little white spots which he didn't know what to make of; and when he tried to feel the place with one of his legs he

Tweet #19: [132 chars] drew it quickly back because as soon as he touched it he was overcome by a cold shudder. ¶ He slid back into his former position. 

Tweet #20: [139 chars] "Getting up early all the time", he thought, "it makes you stupid.  You've got to get enough sleep.  Other travelling salesmen live a life

Tweet #21: [136 chars] of luxury.  For instance, whenever I go back to the guest house during the morning to copy out the contract, these gentlemen are always

Tweet #22: [139 chars] still sitting there eating their breakfasts.  I ought to just try that with my boss; I'd get kicked out on the spot.  But who knows, maybe

Tweet #23: [135 chars] that would be the best thing for me.  If I didn't have my parents to think about I'd have given in my notice a long time ago, I'd have

Tweet #24: [137 chars] gone up to the boss and told him just what I think, tell him everything I would, let him know just what I feel.  He'd fall right off his

Tweet #25: [137 chars] desk! And it's a funny sort of business to be sitting up there at your desk, talking down at your subordinates from up there, especially

~~~
