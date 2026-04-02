FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY user-service/pom.xml user-service/pom.xml
COPY finance-service/pom.xml finance-service/pom.xml

RUN mvn -pl finance-service -am dependency:go-offline -DskipTests

COPY finance-service/src finance-service/src

RUN mvn -pl finance-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/finance-service/target/finance-service-1.0.0.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
