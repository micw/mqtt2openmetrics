FROM openjdk:17-alpine

ADD target/mqtt2openmetrics-1.0.0-SNAPSHOT.jar /app/mqtt2openmetrics.jar

ENV TZ=Europe/Berlin

WORKDIR /app

ENTRYPOINT ["java","-XX:+UnlockExperimentalVMOptions","-XX:+UseContainerSupport","-jar","mqtt2openmetrics.jar"]
