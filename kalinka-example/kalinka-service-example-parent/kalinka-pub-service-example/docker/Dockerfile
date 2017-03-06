FROM openjdk:8-jdk-alpine                                                                                                                                                                                                                

MAINTAINER michas@jarmoni.org

RUN apk add --update bash && rm -rf /var/cache/apk/*

COPY entrypoint.sh /entrypoint.sh
COPY kalinka-pub-service-example*.jar /kalinka-pub-service.jar

ENTRYPOINT ["/entrypoint.sh"]
