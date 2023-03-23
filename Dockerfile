FROM openjdk:17-alpine

ADD target/mqtt2openmetrics-1.0.0-SNAPSHOT.jar /mqtt2openmetrics.jar

ENV TZ=Europe/Berlin

ENTRYPOINT ["java","-XX:+UnlockExperimentalVMOptions","-XX:+UseContainerSupport","-jar","/mqtt2openmetrics.jar"]
