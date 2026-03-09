
# build stage i kompajliranje
# pocinje sa osnovnim imagaem koji sadrzi JDK 17 na Alpine Linuxu(najmanji i najlaksi Linux image)
FROM eclipse-temurin:17-jdk-alpine AS build

#postavlja se radni direktorijum unutar containera na /app
WORKDIR /app

# kopira se Maven Wrapper (mvnw) i Maven Wrapper direktorijum (.mvn) u radni direktorijum containera
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .


# chmod +x mvnw = čini mvnw izvršivim
# ./mvnw dependency:go-offline = preuzima sve zavisnosti unaprijed (offline mode)
# -B = batch mode (bez interakcije)
# ZAŠTO? Zato što se zavisnosti mogu cachirati između buildova → brži builds
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B


# kpira se source code aplikacije u radni direktorijum containera
COPY src ./src

# pakuje se aplikacija, preskačući testove (DskipTests) i koristeći batch mode (-B)
RUN ./mvnw package -DskipTests -B


# Drugi stage - kreiranje finalnog image-a koji će se koristiti za pokretanje aplikacije
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
