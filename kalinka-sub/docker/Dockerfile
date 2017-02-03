FROM openjdk:8-jdk-alpine                                                                                                                                                                                                                

MAINTAINER michas@jarmoni.org

RUN apk add --update bash && rm -rf /var/cache/apk/*

COPY entrypoint.sh /entrypoint.sh
COPY kalinka-sub-*.jar /kalinka-sub.jar

ENTRYPOINT ["/entrypoint.sh"]
