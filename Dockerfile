FROM amazoncorretto:24-alpine-jdk
WORKDIR /app
COPY target/*.jar group-a-order.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/group-a-order.jar"]