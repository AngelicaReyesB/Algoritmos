"""Punto de entrada.

Uso:
    python main.py extract            # PDFs (data/pdfs) -> abstracts JSON (data/abstracts)
    python main.py run                # frecuencias + 3 algoritmos + tiempos + Top 15
    python main.py run --repeats 200
    python main.py demo               # prueba rapida con texto de ejemplo (sin PDFs)
"""

import argparse
from pathlib import Path

from src.benchmark import run_benchmark
from src.extract import extract_corpus, load_abstracts
from src.preprocess import word_frequencies

BASE = Path(__file__).resolve().parent.parent  # raiz del repositorio

DEMO_TEXTS = [
    "Generative artificial intelligence models generate text, images and code. "
    "Large language models are trained on large datasets using deep learning.",
    "Transformers and diffusion models power generative artificial intelligence. "
    "Training data quality affects model performance and bias.",
    "We evaluate language models on generation tasks, measuring accuracy and bias "
    "in generative systems built with deep neural networks.",
]


def print_results(freqs, timings, tops, consistent, top=15):
    print(f"\nTerminos distintos (n): {len(freqs)}")
    print("\nTiempos (ms):")
    print(f"{'Algoritmo':<15}{'media':>10}{'desv':>10}{'min':>10}")
    for name, (m, s, mn) in timings.items():
        print(f"{name:<15}{m:>10.4f}{s:>10.4f}{mn:>10.4f}")
    print(f"\nTop {top} (Counting Sort):")
    for i, (t, f) in enumerate(tops["Counting Sort"], 1):
        print(f"{i:>3}. {t:<20}{f}")
    print("\nLos 3 algoritmos coinciden:", "SI" if consistent else "NO  <-- revisar")
    print("Resultados guardados en results/")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    ap.add_argument("command", choices=["extract", "run", "demo"])
    ap.add_argument("--repeats", type=int, default=100)
    args = ap.parse_args()

    if args.command == "extract":
        extract_corpus(BASE / "data" / "pdfs", BASE / "data" / "abstracts")
        return

    if args.command == "demo":
        texts = DEMO_TEXTS
    else:
        texts = load_abstracts(BASE / "data" / "abstracts")
        if not texts:
            raise SystemExit("No hay abstracts. Coloca los PDFs en data/pdfs y ejecuta: python main.py extract")

    freqs = word_frequencies(texts)
    timings, tops, ok = run_benchmark(freqs, repeats=args.repeats, out_dir=BASE / "results" / "python")
    print_results(freqs, timings, tops, ok)


if __name__ == "__main__":
    main()
