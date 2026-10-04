FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN mvn -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre
ARG MODULE
RUN useradd -r app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar /app.jar
USER app
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app.jar"]
