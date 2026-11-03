# ============================================================
# Dockerfile multi-stage para el backend Spring Boot
# Stage 1: Build con Maven + JDK 17
# Stage 2: Runtime con JRE 17 Alpine (imagen ligera ~180 MB)
# ============================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar solo pom.xml primero (mejor cacheo de capas)
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar el resto del código
COPY src ./src

# Compilar (sin ejecutar tests, ya se corren en CI)
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el jar compilado
COPY --from=build /app/target/*.jar app.jar

# Puerto del backend
EXPOSE 5001

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=dev \
    PORT=5001 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# Healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget --spider -q http://localhost:5001/api/health || exit 1

# Arrancar la app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
