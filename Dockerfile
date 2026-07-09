FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY country-connector/pom.xml country-connector/
COPY border-crossing-backend/pom.xml border-crossing-backend/
COPY border-crossing-app/pom.xml border-crossing-app/
RUN ./mvnw dependency:go-offline -q
COPY country-connector/src country-connector/src
COPY border-crossing-backend/src border-crossing-backend/src
COPY border-crossing-app/src border-crossing-app/src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/border-crossing-app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
