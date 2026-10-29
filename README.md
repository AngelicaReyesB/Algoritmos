# Ranking de términos con Counting, Radix y Bucket Sort

Universidad del Quindío · Ingeniería de Sistemas y Computación · Análisis de Algoritmos

Aplicación que procesa los abstracts de 20 artículos científicos sobre IA generativa,
genera el ranking de los términos más frecuentes usando algoritmos de ordenamiento
no basados en comparaciones (Counting Sort, Radix Sort y Bucket Sort), mide sus
tiempos de ejecución y presenta los resultados en una interfaz web.

## Integrantes

- Angelica María Reyes
- Andrés Felipe Zambrano

## Estructura

```
java/            Versión principal: backend en Java
web/             Frontend: HTML, CSS y JavaScript
python/          Versión en Python (comparación de lenguajes)
data/
  pdfs/          PDFs del corpus (no se versionan)
  abstracts/     Abstracts extraídos
results/
  java/          Resultados de la versión Java
  python/        Resultados de la versión Python
docs/            Informe y diagramas
```

## Ejecución

## Paquetes del backend (java/src/main/java/co/edu/uniquindio/ranking)

| Paquete | Contenido |
| --- | --- |
| `modelo` | Records de datos (Articulo, FrecuenciaTermino, ...) |
| `pdf` | Lectura de PDF (PDFBox) y extraccion del abstract |
| `texto` | Tokenizadores, palabras vacias y conteo de frecuencias |
| `algoritmos` | Counting, Radix y Bucket Sort (seguimiento) |
| `busqueda` | KMP, Boyer-Moore y Rabin-Karp (Requerimiento 3) |
| `similitud` | Levenshtein, Needleman-Wunsch, coseno TF-IDF y Jaccard (Requerimiento 1) |
| `embeddings` | GloVe y MiniLM (Requerimiento 1, enfoques de IA) |
| `agrupamiento` | Single, Complete y Average linkage y correlacion cofenetica (Requerimiento 2) |
| `medicion` | Medicion de tiempos |
| `repositorio` | Acceso a PostgreSQL |
| `servicio` | Carga del corpus, ranking y medicion |
| `controlador` | API REST |

### Versión Python

```bash
cd python
pip install -r requirements.txt
python -m unittest discover tests
python main.py demo
python main.py extract
python main.py run --repeats 200
```

### Versión Java

Requisitos: Java 21 y Docker.

```bash
docker compose up -d          # levanta PostgreSQL 16 (puerto 55432)
cd java
./mvnw spring-boot:run        # en Windows (CMD): mvnw.cmd spring-boot:run
```

La aplicación queda en http://localhost:8080

## Organización del trabajo

Tablero Kanban en Trello. Cada tarjeta corresponde a una rama
`feature/<ID>-<descripcion>` y se integra a `main` mediante pull request.