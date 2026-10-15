# Ranking de términos con Counting, Radix y Bucket Sort

Universidad del Quindío · Ingeniería de Sistemas y Computación · Análisis de algoritmos (bibliometría, IA generativa).

Genera el ranking de términos más frecuentes de un corpus de 20 abstracts usando algoritmos de ordenamiento **no basados en comparaciones** (implementados desde cero), mide sus tiempos en milisegundos y valida el Top 15.

## Estructura

```
src/sorting.py      Counting, Radix y Bucket Sort (estables, orden descendente)
src/extract.py      PDF -> abstract (PyMuPDF)
src/preprocess.py   minúsculas, tokenización, stopwords, frecuencias
src/benchmark.py    tiempos (ms), Top 15, CSV y gráficos
main.py             CLI: extract | run | demo
tests/              pruebas de corrección y estabilidad
data/pdfs/          aquí van los 20 PDFs del docente
data/abstracts/     abstracts extraídos (JSON)
results/            tiempos.csv, top15.csv, tiempos.png, top15.png
docs/               informe
```

## Uso

```bash
pip install -r requirements.txt
python -m unittest discover tests      # verifica los algoritmos
python main.py demo                    # prueba rápida sin PDFs
# copiar los 20 PDFs a data/pdfs/
python main.py extract                 # revisar los avisos de abstracts no detectados
python main.py run --repeats 200       # genera resultados en results/
```

## Decisiones de diseño

- **Desempate**: los tres algoritmos son estables; los empates conservan el orden de primera aparición en el corpus, así los tres producen exactamente el mismo ranking.
- **Orden descendente** mediante la clave `max_freq - freq`, sin invertir la lista (para no perder estabilidad).
- No se usan `sorted()`, `list.sort()` ni `Counter.most_common()` en la lógica; `sorted` solo aparece en los tests como referencia.
- Preprocesamiento: minúsculas, solo letras, longitud ≥ 3, stopwords en inglés y español (lista editable en `src/preprocess.py`).

## Pendiente

- [ ] Verificar la extracción de abstracts con los PDFs reales
- [ ] Confirmar con el docente: lematización, stopwords, formato de entrega
- [ ] Informe en `docs/` (fundamentación teórica, metodología, resultados, conclusiones)
