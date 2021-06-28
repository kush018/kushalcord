FROM openjdk:11.0.11-jre-slim

COPY ./target/kushalcord-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/kushalcord/kushalcord.jar
WORKDIR /usr/kushalcord

CMD ["java", "-jar", "kushalcord.jar"]
