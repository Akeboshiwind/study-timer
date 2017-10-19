FROM java:8-alpine
MAINTAINER Oliver Marshall <olivershawmarshall@gmail.com>

ADD target/uberjar/study-timer.jar /study-timer/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/study-timer/app.jar"]
