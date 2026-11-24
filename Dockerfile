# Immagine unica per il deploy: frontend Angular compilato dentro il jar Spring Boot.
# Un solo contenitore, una sola origine: /api e /ws/lobby rispondono sullo stesso host.

# --- 1. Frontend: da sorgenti Angular a file statici ---
FROM node:22-alpine AS frontend
WORKDIR /lavoro

# Prima solo i manifest, cosi' npm ci resta in cache quando cambia il codice.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# --- 2. Backend: jar eseguibile con il frontend nelle risorse statiche ---
FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /lavoro

COPY backend/pom.xml ./
RUN mvn -B -q -DskipTests dependency:resolve

COPY backend/src ./src
# Spring Boot serve classpath:/static: index.html risponde su "/".
COPY --from=frontend /lavoro/dist/frontend/browser ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# --- 3. Runtime: solo JRE 25 e il jar ---
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 1001 stoppa
COPY --from=backend /lavoro/target/*.jar /app/app.jar
USER stoppa

# Su Render il piano free ha 0.1 CPU e 512 MB: serial GC e heap al 70% della RAM.
# La porta arriva da PORT (Render); in locale senza PORT resta la 8080.
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -jar /app/app.jar --server.port=${PORT:-8080}"]
