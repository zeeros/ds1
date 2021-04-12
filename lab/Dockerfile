FROM oracle/jdk:8
RUN yum -y install wget unzip
RUN wget https://services.gradle.org/distributions/gradle-6.8.3-bin.zip -P /tmp
RUN unzip -d /opt/gradle /tmp/gradle-*.zip
ENV PATH="/opt/gradle/gradle-6.8.3/bin:${PATH}"
COPY hello/ /home/src/hello
WORKDIR /home/src/hello
