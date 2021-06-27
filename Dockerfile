FROM debian

RUN apt-get update && apt-get upgrade -y
RUN apt-get install openjdk-11-jdk -y

COPY ./target/kushalcord-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/kushalcord/kushalcord.jar
WORKDIR /usr/kushalcord

CMD ["java", "-jar", "kushalcord.jar"]