"""Medicion de tiempos por etapa, validacion del Top 15 y datos del equipo.

Mismo procedimiento que MedicionManual.java y ServicioMedicion.java:
mismas etapas, mismo calentamiento y mismas repeticiones, para comparar
Java y Python en igualdad de condiciones.
"""

import csv
import math
import os
import platform
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Dict, List

from .algoritmos import ALGORITMOS, Par
from .extraccion import cargar_abstracts, extraer_abstract, leer_primeras_paginas
from .texto import contar_terminos, tokenizar_todos

# Calentamiento y repeticiones por etapa (iguales a la version Java)
CALENTAMIENTO_PDF, REPETICIONES_PDF = 1, 5
CALENTAMIENTO_LECTURA, REPETICIONES_LECTURA = 5, 50
CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO = 20, 200
CALENTAMIENTO_ORDEN, REPETICIONES_ORDEN = 100, 1000

TOP = 15


@dataclass
class ResultadoMedicion:
    etapa: str
    repeticiones: int
    media_ms: float
    desviacion_ms: float
    minimo_ms: float


def medir(etapa: str, calentamiento: int, repeticiones: int, tarea: Callable[[], object]) -> ResultadoMedicion:
    """Ejecuta la tarea varias veces sin medir (calentamiento) y luego mide cada repeticion."""
    for _ in range(calentamiento):
        tarea()

    tiempos_ms: List[float] = []
    for _ in range(repeticiones):
        inicio = time.perf_counter_ns()
        tarea()
        fin = time.perf_counter_ns()
        tiempos_ms.append((fin - inicio) / 1_000_000)

    media = sum(tiempos_ms) / repeticiones
    desviacion = math.sqrt(sum((t - media) ** 2 for t in tiempos_ms) / repeticiones)
    return ResultadoMedicion(etapa, repeticiones, media, desviacion, min(tiempos_ms))


def informacion_equipo() -> Dict[str, str]:
    """Caracteristicas del equipo donde se mide."""
    return {
        "Procesador": platform.processor() or "desconocido",
        "Nucleos logicos": str(os.cpu_count()),
        "Sistema operativo": f"{platform.system()} {platform.release()}",
        "Python": f"{platform.python_version()} ({platform.python_implementation()})",
    }


def _extraer_todos(pdfs: List[Path]) -> List[str]:
    """Tarea medida en la etapa 1: leer y extraer todos los PDF."""
    return [extraer_abstract(leer_primeras_paginas(pdf)) for pdf in pdfs]


def medir_todo(carpeta_pdfs: Path, carpeta_abstracts: Path, carpeta_resultados: Path) -> bool:
    """Mide todas las etapas, valida el ranking y guarda los resultados. Devuelve si coinciden."""
    resultados: List[ResultadoMedicion] = []

    # Etapa 1: extraccion de los PDF con PyMuPDF (se omite si no hay PDF)
    pdfs = sorted(carpeta_pdfs.glob("*.pdf"))
    if pdfs:
        resultados.append(medir("Extraccion PDF (PyMuPDF)", CALENTAMIENTO_PDF, REPETICIONES_PDF,
                                lambda: _extraer_todos(pdfs)))
    else:
        print(f"[!] No hay PDF en {carpeta_pdfs}: se omite la etapa de extraccion")

    # Etapa 2: lectura de los abstracts (en Python se leen de los JSON, no hay base de datos)
    resultados.append(medir("Lectura de abstracts (JSON)", CALENTAMIENTO_LECTURA, REPETICIONES_LECTURA,
                            lambda: cargar_abstracts(carpeta_abstracts)))
    textos = cargar_abstracts(carpeta_abstracts)

    # Etapa 3: preprocesamiento
    resultados.append(medir("Preprocesamiento", CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO,
                            lambda: tokenizar_todos(textos)))
    terminos = tokenizar_todos(textos)

    # Etapa 4: conteo de frecuencias
    resultados.append(medir("Conteo de frecuencias", CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO,
                            lambda: contar_terminos(terminos)))
    frecuencias = contar_terminos(terminos)

    # Etapa 5: los tres algoritmos sobre la misma lista
    rankings: Dict[str, List[Par]] = {}
    for nombre, algoritmo in ALGORITMOS.items():
        resultados.append(medir(nombre, CALENTAMIENTO_ORDEN, REPETICIONES_ORDEN,
                                lambda a=algoritmo: a(frecuencias)))
        rankings[nombre] = algoritmo(frecuencias)

    # Validacion: los tres rankings deben ser identicos
    referencia = rankings["Counting Sort"]
    coinciden = all(ranking == referencia for ranking in rankings.values())
    frecuencia_maxima = max(f for _, f in frecuencias)

    equipo = informacion_equipo()
    _imprimir(resultados, len(terminos), len(frecuencias), frecuencia_maxima, coinciden, equipo)
    _guardar(resultados, rankings, len(terminos), len(frecuencias), frecuencia_maxima, equipo, carpeta_resultados)
    return coinciden


def _imprimir(resultados, terminos_totales, n, k, coinciden, equipo) -> None:
    print("\n========== MEDICION DE TIEMPOS (ms) ==========")
    print(f"{'Etapa':<28} {'Rep.':>6} {'Media':>10} {'Desv.':>10} {'Minimo':>10}")
    for r in resultados:
        print(f"{r.etapa:<28} {r.repeticiones:>6} {r.media_ms:>10.4f} {r.desviacion_ms:>10.4f} {r.minimo_ms:>10.4f}")
    print(f"Terminos totales: {terminos_totales} | Terminos distintos (n): {n} | Frecuencia maxima (k): {k}")
    print("Los 3 algoritmos producen el mismo ranking:", "SI" if coinciden else "NO <-- revisar")
    for clave, valor in equipo.items():
        print(f"{clave}: {valor}")


def _guardar(resultados, rankings, terminos_totales, n, k, equipo, carpeta: Path) -> None:
    """Mismos archivos y formato que la version Java: tiempos.csv, top15.csv y equipo.txt."""
    carpeta.mkdir(parents=True, exist_ok=True)

    with open(carpeta / "tiempos.csv", "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["etapa", "repeticiones", "media_ms", "desviacion_ms", "minimo_ms"])
        for r in resultados:
            escritor.writerow([r.etapa, r.repeticiones, f"{r.media_ms:.4f}",
                               f"{r.desviacion_ms:.4f}", f"{r.minimo_ms:.4f}"])

    with open(carpeta / "top15.csv", "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        encabezado = ["posicion"]
        for nombre in rankings:
            encabezado += [f"{nombre} - termino", f"{nombre} - frecuencia"]
        escritor.writerow(encabezado)
        for i in range(min(TOP, n)):
            fila = [i + 1]
            for ranking in rankings.values():
                fila += [ranking[i][0], ranking[i][1]]
            escritor.writerow(fila)

    lineas = [f"{clave}: {valor}" for clave, valor in equipo.items()]
    lineas += [f"Terminos totales: {terminos_totales}", f"Terminos distintos (n): {n}",
               f"Frecuencia maxima (k): {k}"]
    (carpeta / "equipo.txt").write_text("\n".join(lineas) + "\n", encoding="utf-8")

    _graficar(resultados, rankings, carpeta)
    print(f"Resultados guardados en {carpeta.resolve()}")


def _graficar(resultados, rankings, carpeta: Path) -> None:
    """Graficos con matplotlib: tiempos de los algoritmos y Top 15."""
    import matplotlib
    matplotlib.use("Agg")  # genera imagenes sin abrir ventanas
    import matplotlib.pyplot as plt

    algoritmos = [r for r in resultados if r.etapa in ALGORITMOS]
    figura, eje = plt.subplots(figsize=(7, 3.5))
    eje.barh([r.etapa for r in algoritmos][::-1], [r.media_ms for r in algoritmos][::-1], color="#2459c2")
    eje.set_xlabel("Tiempo medio (ms)")
    eje.set_title("Algoritmos de ordenamiento - Python")
    figura.tight_layout()
    figura.savefig(carpeta / "tiempos.png", dpi=150)
    plt.close(figura)

    top = rankings["Counting Sort"][:TOP]
    figura, eje = plt.subplots(figsize=(7, 5))
    eje.barh([t for t, _ in top][::-1], [f for _, f in top][::-1], color="#2459c2")
    eje.set_xlabel("Frecuencia")
    eje.set_title("Top 15 de terminos - Python")
    figura.tight_layout()
    figura.savefig(carpeta / "top15.png", dpi=150)
    plt.close(figura)