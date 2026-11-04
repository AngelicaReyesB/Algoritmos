# Análisis de algoritmos en el contexto de la bibliometría

Universidad del Quindío · Ingeniería de Sistemas y Computación · Análisis de Algoritmos · 2026-2

Aplicación web que analiza los abstracts de 20 artículos científicos entregados por el docente. Compara textos, agrupa artículos y busca patrones con algoritmos implementados desde cero.

- **Aplicación desplegada:** https://algoritmos-ranking.onrender.com
- **Plan gratuito:** la primera solicitud después de un tiempo sin visitas puede tardar varios minutos.

## Integrantes

- Angelica María Reyes
- Andrés Felipe Zambrano

## Requerimientos

| Requerimiento | Contenido | Pestaña |
| --- | --- | --- |
| R1. Similitud textual | Levenshtein, Needleman-Wunsch, coseno con TF-IDF, Jaccard, FastText y MiniLM | Similitud |
| R2. Agrupamiento jerárquico | Single, Complete y Average Linkage, dendrogramas y correlación cofenética | Agrupamiento |
| R3. Búsqueda de patrones | Knuth-Morris-Pratt, Boyer-Moore y Rabin-Karp | Búsqueda |
| R4. Despliegue | Docker, Render y PostgreSQL | Toda la aplicación |

La aplicación conserva las pestañas del seguimiento: Artículos, Top 15 y Tiempos.

Los algoritmos no usan funciones de alto nivel como `indexOf()` o `find()`, ni librerías de similitud, agrupamiento o búsqueda. Las librerías de apoyo solo leen los PDF (PDFBox) o convierten texto en vectores en el caso de MiniLM (LangChain4j con ONNX). FastText usa un archivo de vectores preentrenados, filtrado a las palabras del corpus.

Apache Lucene y JMH quedan del seguimiento. Lucene solo se usa para comparar el tokenizador propio, y ningún requerimiento del proyecto final depende de él.

## Tecnologías

- Java 21 y Spring Boot 3.5
- PostgreSQL 16
- HTML, CSS y JavaScript sin framework
- Python para la comparación de lenguajes
- Docker y Render para el despliegue

## Estructura

```text
java/                 Backend en Java
web/                  Frontend en HTML, CSS y JavaScript
python/               Versión en Python para medir y comparar
data/
  pdfs/               PDF del corpus
  abstracts/          Archivos JSON usados por la medición controlada
  embeddings/         Vectores de FastText y MiniLM
results/
  java/               Resultados de la medición en Java
  python/             Resultados de la medición en Python
docs/
  postman/            Colecciones de Postman
Dockerfile            Imagen de la aplicación
docker-compose.yml    Aplicación, PostgreSQL y túnel opcional de Ngrok
```

## Paquetes del backend

Ruta: `java/src/main/java/co/edu/uniquindio/ranking`

| Paquete | Contenido |
| --- | --- |
| `controller` | API REST |
| `service` | Carga del corpus y coordinación de cada requerimiento |
| `repository` | Acceso a PostgreSQL |
| `model` | Datos que viajan entre capas y como JSON |
| `pdf` | Lectura de PDF y extracción de abstract, título y autores |
| `texto` | Tokenizadores, palabras vacías y conteo de términos |
| `similitud` | Levenshtein, Needleman-Wunsch, coseno con TF-IDF y Jaccard |
| `embeddings` | FastText, MiniLM y operaciones con vectores |
| `agrupamiento` | Matriz de distancias, agrupamiento y correlación cofenética |
| `busqueda` | KMP, Boyer-Moore y Rabin-Karp |
| `algoritmos` | Counting, Radix y Bucket Sort del seguimiento |
| `medicion` | Medición manual de tiempos |

## Ejecución local

Requisitos: Java 21 y Docker. Los comandos usan una terminal Bash.

### Opción 1: base en Docker y backend con Maven

```bash
cp .env.example .env
docker compose up -d postgres
cd java
./mvnw spring-boot:run
```

En Windows (CMD), el último comando es `mvnw.cmd spring-boot:run`.

La aplicación queda en http://localhost:8080.

### Opción 2: todo en Docker

```bash
cp .env.example .env
docker compose up -d --build
```

La aplicación queda en http://localhost:8081.

Para detenerla:

```bash
docker compose down
```

## API

| Método y ruta | Parámetros | Requerimiento |
| --- | --- | --- |
| `GET /api/articulos` | Ninguno | Corpus |
| `GET /api/ranking` | `top`, de 1 a 100 | Seguimiento |
| `GET /api/tiempos` | Ninguno | Seguimiento |
| `POST /api/similitud` | Cuerpo `{"ids": [5, 6]}`, mínimo 2 ids distintos | R1 |
| `GET /api/agrupamiento` | `similitud=tfidf` o `similitud=minilm` | R2 |
| `GET /api/busqueda` | `patron`, de 1 a 100 caracteres | R3 |

## Pruebas

### Java

```bash
cd java
./mvnw test
```

### Python

```bash
cd python
pip install -r requirements.txt
python -m unittest discover
```

Las colecciones de Postman están en `docs/postman`.

## Mediciones

### Búsqueda en Java

Ejecute el método `main` de `MedicionBusqueda` desde la carpeta `java/`. El resultado queda en `results/java/busqueda.csv`.

### Búsqueda en Python

```bash
cd python
python main.py buscar
```

El resultado queda en `results/python/busqueda.csv`.

Esta medición usa los 13 archivos JSON de `data/abstracts`, unidos en un texto de 21.116 caracteres. La pestaña Búsqueda usa los 20 abstracts guardados en la base de datos, así que sus tiempos no son comparables de forma directa.

### Caso de estudio de MiniLM

Ejecute el método `main` de `CasoMiniLm` desde la carpeta `java/`. Muestra el producto punto, las normas y el coseno de los artículos 5 y 6.

## Despliegue

La imagen se construye con el `Dockerfile` de la raíz. En producción se configuran estas variables de entorno:

| Variable | Uso |
| --- | --- |
| `DB_URL` | Conexión JDBC a PostgreSQL |
| `DB_USER` | Usuario de la base de datos |
| `DB_PASSWORD` | Clave de la base de datos |
| `CARGAR_CORPUS` | `true` para procesar los PDF al arrancar |
| `JAVA_OPTS` | Opciones de memoria de la JVM |

El archivo `.env` no se sube al repositorio. Use `.env.example` como plantilla.

## Organización del trabajo

El trabajo se organizó en un tablero Kanban de Trello. Cada tarjeta corresponde a una rama, y los cambios se integran a `main` mediante pull request.
