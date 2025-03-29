FROM openjdk:8
VOLUME /tmp
ADD *.jar app.jar
ENV TZ=Asia/Shanghai

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=utf-8","-jar","/app.jar"]
