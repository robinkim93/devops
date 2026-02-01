FROM openjdk:17.0.2-jdk

WORKDIR /app

COPY backend/app/build/libs/app.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]