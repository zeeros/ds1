Setting up the programming environment
======================================

First of all, unzip the provided example "hello.zip".

If you use a Java IDE (Idea, Eclipse, etc.), import the provided Gradle project (hello/build.gradle) 
in the IDE and try to run it. You should see the output listed below.

If you prefer using the command line and a plain text editor for programming, follow the instructions below.

1. Download and install Java SDK, Standard Edition. Version 8 is recommended for compatibility reasons.
   http://www.oracle.com/technetwork/java/javase/downloads/index.html

2. Install Gradle build tool
   https://gradle.org/install/

3. In the command line, change dir to "hello" and run the project:
      cd hello
      gradle run

4. You should see output like this:
[receiver] received a message from sender2: Hello from sender2
[receiver] received a message from sender3: Hello from sender3
[receiver] received a message from sender4: Hello from sender4
[receiver] received a message from sender0: Hello from sender0
[receiver] received a message from sender1: Hello from sender1

5. Hit Enter to stop the program.

