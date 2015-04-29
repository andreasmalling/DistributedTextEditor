# Kør koden:
1. Flyt mappen *DistributedTextEditor* til `fh.cs.au.dk` og `ssh -X` ind på to forskellige maskiner.
2. Naviger til mappen og kør `run.sh` på begge maskiner.
3. Tryk `File>Listen` på den ene maskine, og noter IP samt port i titlen på vinduet.
4. På den anden maskine skrives IP og port i input-felterne nederst i vinduet. Herefter trykkes der `File>Connect`.
5. Efter et kort øjeblik er forbindelsen oprettet, og der kan nu indtastes i det øverste tekstfelt på begge maskiner.
6. Tryk på `File>Disconnect` på en hvilken som helst maskine for at afbryde forbindelsen, og gå tilbage til start-tilstanden.

# Exercise 9 (Hand-in) (2015)
In this exercise you have to code a very simple distributed editor. You are free to code it from scratch or modify the code from Exercise 3. Your editor should have these capabilities:

1. If a DistributedTextEditor has the menu item Listen fired, then it will become a TCP/IP server listening for connections on some incoming port of your choice. You might use the one in the text field saying "Port number here" if there is a port number in that field, but you don't have to. However, you must write the IP address and the port number of the server in the title of the window. You might be inspired by the DDistDemoServer from Exercise 2. Your report should include the code that you added for implementing this, including a description of where you added the code, and why.
2. If a DistributedTextEditor has the menu item Connect fired, then it will connect to the server who is listening on the IP address and port number specified in the bottom two text fields. You might be inspired by the DDistDemoServer from Exercise 2. Your report should include the code that you added for implementing this, including a description of where you added the code, and why.
3. When two DistributedTextEditor's are thus connected, what is written in the top text field of the client should appear in the bottom text field of the server, and what is written in the top text field of the server should appear in the bottom text field of the client. You should implement this by changing the implementation of EventReplayer such that it sends MyTextEvent's to the peer instead of replaying them locally. At the peer the implementation of EventReplayer should then receive these remote MyTextEvent's and replay them in the bottom text area. Drop the delay of 1 second. To transport the events from one peer to the other, use the ObjectInputStream class and the ObjectOutputStream class. See the Java API documentation for more details. Your report should include the code that you added for implementing this, including a description of where you added the code, and why.
4. If any of the two peers has the Disconnect menu item fired, then they should disconnect. This should be done so cleanly that the client might connect to another server afterwards and such that the server might get contacted by another client afterwards. Your report should include the code that you added for implementing this, including a description of where you added the code, and why.
5. You are free to add extra features. Describe in the report if you did. Extra features could include handling text attributres like bold. Another extra feature could be added robustness, like gracefully handling of the other peer crashing and so on. 
6. Your report should describe how your system is designed and why it is an appropriate design. In describing the design of the system you should try to use as many relevant concepts from the course as possible.
7. Your report should contain instructions on how the TA can easily run a demo of your system.
8. Your report should contain a link to the code of your system, and the code should be readable, commented and idiomatic.

<div class="align-right">*Source: [dDist 2015](https://bb.au.dk/webapps/blackboard/execute/content/blankPage?cmd=view&content_id=_271630_1&course_id=_33604_1&mode=reset)*</div>