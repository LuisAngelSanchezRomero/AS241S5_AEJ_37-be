# Stage 1: Build with Maven
FROM 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with Java
FROM 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


# CONSTRUIR UNA IMÁGEN MEDIANTE UN Dockerfile EN LA RAÍZ
# docker build -t 50luisangelsanchezromero/as241s5_aej_37-be:latest .

# EJECUTAR LA IMÁGEN GENERADA
# docker run -d --name as241s5-aej-37-be -p 8082:8082 50luisangelsanchezromero/as241s5_aej_37-be:latest

# SUBIR IMÁGEN GENERADA A DOCKERHUB
# docker push 50luisangelsanchezromero/as241s5_aej_37-be:latest


# DESCARGAR, RENOMBRAR Y SUBIR MAVEN A TU USUARIO DOCKERHUB
# docker pull maven:3.9-amazoncorretto-17-alpine
# docker tag maven:3.9-amazoncorretto-17-alpine 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine
# docker push 50luisangelsanchezromero/maven:3.9-amazoncorretto-17-alpine


# DESCARGAR, RENOMBRAR Y SUBIR JDK17 A TU USUARIO DOCKERHUB
# docker pull eclipse-temurin:17-jre-alpine
# docker tag eclipse-temurin:17-jre-alpine 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
# docker push 50luisangelsanchezromero/eclipse-temurin:17-jre-alpine
