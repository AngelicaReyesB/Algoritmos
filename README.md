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

Pendiente (ver tarjeta CONF-03: selección de tecnologías).

## Organización del trabajo

Tablero Kanban en Trello. Cada tarjeta corresponde a una rama
`feature/<ID>-<descripcion>` y se integra a `main` mediante pull request.