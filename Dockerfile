# ---------- Etapa 1: compilar con Maven ----------
# Esta imagen solo se usa para construir el JAR; no queda en la imagen final.
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /build

# Primero solo el pom.xml: si las dependencias no cambian, Docker reutiliza esta capa
# y no vuelve a descargarlas en cada compilacion.
COPY java/pom.xml java/pom.xml
RUN mvn -B -q -f java/pom.xml dependency:resolve

COPY java/src java/src
RUN mvn -B -q -f java/pom.xml package -DskipTests

# ---------- Etapa 2: ejecutar con Java 21 ----------
# Imagen basada en Ubuntu (no Alpine): el modelo MiniLM usa librerias nativas de ONNX
# que necesitan glibc.
FROM eclipse-temurin:21-jre

# Misma estructura de carpetas que el repositorio. Asi las rutas relativas de
# application.properties (../web, ../data, ../results/java) funcionan sin cambiarlas.
#   /app/java     <- carpeta de trabajo (donde esta el JAR)
#   /app/web      <- frontend
#   /app/data     <- abstracts, metadatos y vectores (los PDF no se copian)
#   /app/results  <- donde la aplicacion escribe sus CSV
RUN useradd --system --no-create-home app \
    && mkdir -p /app/java /app/results/java \
    && chown -R app /app/results
WORKDIR /app/java

COPY --from=compilar /build/java/target/ranking-*.jar app.jar
COPY web/ /app/web/
COPY data/ /app/data/

# No se ejecuta como root
USER app

# Limite de memoria del heap de Java. Se puede cambiar con la variable JAVA_OPTS.
ENV JAVA_OPTS="-Xmx350m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
