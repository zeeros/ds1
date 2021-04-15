docker run -it --rm -u gradle -v gradle-cache:/home/gradle/.gradle -v ${PWD}:/home/gradle/project -w /home/gradle/project gradle:latest bash
