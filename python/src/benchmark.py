"""Medicion empirica, validacion del Top 15 y graficos."""

import csv
import statistics
import time
from pathlib import Path
from typing import Callable, Dict, List, Tuple

from .sorting import ALGORITHMS, Pair


def time_algorithm(fn: Callable[[List[Pair]], List[Pair]], data: List[Pair], repeats: int = 100):
    """Tiempo en milisegundos: (media, desviacion, minimo) sobre `repeats` corridas."""
    fn(data)  # calentamiento
    samples = []
    for _ in range(repeats):
        t0 = time.perf_counter()
        fn(data)
        samples.append((time.perf_counter() - t0) * 1000.0)
    return statistics.mean(samples), statistics.pstdev(samples), min(samples)


def run_benchmark(data: List[Pair], repeats: int = 100, top: int = 15, out_dir: Path = Path("results")):
    out_dir.mkdir(parents=True, exist_ok=True)
    timings: Dict[str, Tuple[float, float, float]] = {}
    tops: Dict[str, List[Pair]] = {}
    for name, fn in ALGORITHMS.items():
        timings[name] = time_algorithm(fn, data, repeats)
        tops[name] = fn(data)[:top]

    # Validacion: los tres algoritmos deben coincidir
    reference = tops["Counting Sort"]
    consistent = all(t == reference for t in tops.values())

    # CSV de tiempos
    with open(out_dir / "tiempos.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["algoritmo", "n_terminos", "repeticiones", "media_ms", "desv_ms", "min_ms"])
        for name, (m, s, mn) in timings.items():
            w.writerow([name, len(data), repeats, f"{m:.4f}", f"{s:.4f}", f"{mn:.4f}"])

    # CSV Top 15 por algoritmo
    with open(out_dir / "top15.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["posicion"] + [c for n in ALGORITHMS for c in (f"{n} - termino", f"{n} - frecuencia")])
        for i in range(min(top, len(reference))):
            row = [i + 1]
            for n in ALGORITHMS:
                row += [tops[n][i][0], tops[n][i][1]]
            w.writerow(row)

    _plot(timings, tops, out_dir)
    return timings, tops, consistent


def _plot(timings, tops, out_dir: Path):
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        print("[!] matplotlib no instalado: se omiten los graficos")
        return

    names = list(timings)
    means = [timings[n][0] for n in names]
    errs = [timings[n][1] for n in names]
    fig, ax = plt.subplots(figsize=(6, 4))
    ax.bar(names, means, yerr=errs, capsize=5, color=["#4C78A8", "#F58518", "#54A24B"])
    ax.set_ylabel("Tiempo de ejecucion (ms)")
    ax.set_title("Comparacion de tiempos (media ± desv. est.)")
    fig.tight_layout()
    fig.savefig(out_dir / "tiempos.png", dpi=150)
    plt.close(fig)

    ref = tops[names[0]]
    fig, ax = plt.subplots(figsize=(7, 5))
    ax.barh([t for t, _ in ref][::-1], [f for _, f in ref][::-1], color="#4C78A8")
    ax.set_xlabel("Frecuencia")
    ax.set_title("Top 15 terminos del corpus")
    fig.tight_layout()
    fig.savefig(out_dir / "top15.png", dpi=150)
    plt.close(fig)
