# Stage 1: Build con Maven
FROM 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime con JRE liviano
FROM 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]


# CONSTRUIR LA IMAGEN (no depende de target local)
# docker build -t 50luisangelsanchezromero/luis-sanchez-37-be:1.0 .

# EJECUTAR LA IMAGEN
# docker run -d --name luis-sanchez-37-be -p 8082:8082 50luisangelsanchezromero/luis-sanchez-37-be:1.0

# SUBIR IMAGEN A DOCKERHUB
# docker push 50luisangelsanchezromero/luis-sanchez-37-be:1.0


# PRE-REQUISITO: tener estas imágenes base en tu DockerHub
# docker pull maven:3.9-amazoncorretto-17-alpine
# docker tag maven:3.9-amazoncorretto-17-alpine 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine
# docker push 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine

# docker pull eclipse-temurin:17-jre-alpine
# docker tag eclipse-temurin:17-jre-alpine 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
# docker push 50luisangelsanchezromero/eclipse-temurin:17-jre-alpineeclipse-temurin:17-jre-alpine
# docker push 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
