"""Medicion de los algoritmos de busqueda (Requerimiento 3) en Python.

Mismo procedimiento que CorpusBusqueda.java y MedicionBusqueda.java, para
comparar Java y Python en igualdad de condiciones:
- Mismo texto: los 20 abstracts de data/abstracts unidos con un espacio.
- Mismos 4 escenarios (corto, largo, ausente_corto, ausente_largo).
- Preprocesamiento y busqueda medidos POR SEPARADO.
- Mismo calentamiento (200) y mismas repeticiones (1000).
- Mismo formato de CSV (results/python/busqueda.csv).
"""

import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Dict, List, Optional

from .busqueda import BoyerMoore, KnuthMorrisPratt, RabinKarp
from .medicion import ResultadoMedicion, informacion_equipo, medir

CALENTAMIENTO = 200
REPETICIONES = 1000

PREPROCESAMIENTO = "preprocesamiento"
BUSQUEDA = "busqueda"

# Escenarios (iguales a CorpusBusqueda.java)
CORTO = "corto"
LARGO = "largo"
AUSENTE_CORTO = "ausente_corto"
AUSENTE_LARGO = "ausente_largo"

PATRON_CORTO = "model"
PATRON_AUSENTE_CORTO = "zqxjk"
LONGITUD_PATRON_LARGO = 40
CARACTER_QUE_ROMPE = "#"

COLUMNAS = ["algoritmo", "escenario", "longitud_patron", "longitud_texto", "etapa", "repeticiones",
            "media_ms", "desviacion_ms", "minimo_ms", "ocurrencias", "colisiones"]


@dataclass
class Fila:
    """Una fila del CSV. ocurrencias y colisiones solo aplican a la etapa de busqueda."""
    algoritmo: str
    escenario: str
    longitud_patron: int
    longitud_texto: int
    etapa: str
    medicion: ResultadoMedicion
    ocurrencias: Optional[int] = None
    colisiones: Optional[int] = None


@dataclass
class Etapas:
    """Las dos etapas de un algoritmo para un patron, listas para medir."""
    algoritmo: str
    preprocesamiento: Callable[[], object]
    busqueda: Callable[[], object]
    posiciones: List[int]
    colisiones: Optional[int] = None


# --------------------------------------------------------------------------- #
# Texto y escenarios (CorpusBusqueda.java)
# --------------------------------------------------------------------------- #
def cargar_texto(carpeta_abstracts: Path) -> str:
    """Lee el campo "abstract" de cada JSON (ordenados por nombre, igual que Java)
    y los une con un espacio."""
    resumenes = []
    for archivo in sorted(carpeta_abstracts.glob("*.json")):
        with open(archivo, encoding="utf-8") as entrada:
            resumenes.append(json.load(entrada)["abstract"])
    return " ".join(resumenes)


def _fragmento_del_medio(texto: str) -> str:
    """40 caracteres desde la primera palabra completa que empieza despues de la mitad del texto."""
    inicio = len(texto) // 2
    while inicio < len(texto) and texto[inicio] != " ":
        inicio += 1
    inicio += 1  # la primera letra de la palabra siguiente
    if inicio + LONGITUD_PATRON_LARGO > len(texto):
        inicio = len(texto) // 2  # texto sin espacios o muy corto: se usa la mitad tal cual
    return texto[inicio:inicio + LONGITUD_PATRON_LARGO]


def _comprobar(texto: str, escenario: str, patron: str, debe_aparecer: bool) -> None:
    aparece = len(KnuthMorrisPratt().buscar(texto, patron)) > 0
    if aparece != debe_aparecer:
        estado = "deberia aparecer y no aparece" if debe_aparecer else "no deberia aparecer y aparece"
        raise ValueError(f"El escenario '{escenario}' (patron '{patron}') {estado} en el corpus")


def crear_escenarios(texto: str) -> Dict[str, str]:
    """Los cuatro escenarios (nombre -> patron), en el mismo orden que Java,
    comprobando que los presentes aparecen y los ausentes no."""
    if len(texto) < 2 * LONGITUD_PATRON_LARGO:
        raise ValueError("El texto es demasiado corto para crear los escenarios")

    largo = _fragmento_del_medio(texto)
    ausente_largo = largo[:-1] + CARACTER_QUE_ROMPE

    escenarios = {
        CORTO: PATRON_CORTO,
        LARGO: largo,
        AUSENTE_CORTO: PATRON_AUSENTE_CORTO,
        AUSENTE_LARGO: ausente_largo,
    }
    _comprobar(texto, CORTO, PATRON_CORTO, True)
    _comprobar(texto, LARGO, largo, True)
    _comprobar(texto, AUSENTE_CORTO, PATRON_AUSENTE_CORTO, False)
    _comprobar(texto, AUSENTE_LARGO, ausente_largo, False)
    return escenarios


# --------------------------------------------------------------------------- #
# Etapas de cada algoritmo (MedicionBusqueda.java)
# --------------------------------------------------------------------------- #
def _etapas_kmp(texto: str, patron: str) -> Etapas:
    kmp = KnuthMorrisPratt()
    tabla = kmp.construir_tabla_prefijos(patron)
    return Etapas(kmp.nombre,
                  lambda: kmp.construir_tabla_prefijos(patron),
                  lambda: kmp.buscar_con_tabla(texto, patron, tabla),
                  kmp.buscar_con_tabla(texto, patron, tabla))


def _etapas_boyer_moore(texto: str, patron: str) -> Etapas:
    bm = BoyerMoore()
    caracter_malo = bm.construir_tabla_caracter_malo(patron)
    sufijo_bueno = bm.construir_tabla_sufijo_bueno(patron)
    return Etapas(bm.nombre,
                  lambda: (bm.construir_tabla_caracter_malo(patron), bm.construir_tabla_sufijo_bueno(patron)),
                  lambda: bm.buscar_con_tablas(texto, patron, caracter_malo, sufijo_bueno),
                  bm.buscar_con_tablas(texto, patron, caracter_malo, sufijo_bueno))


def _etapas_rabin_karp(texto: str, patron: str) -> Etapas:
    rk = RabinKarp()
    pre = rk.preprocesar(texto, patron)
    resultado = rk.buscar_contando_colisiones(texto, patron, pre)
    return Etapas(rk.nombre,
                  lambda: rk.preprocesar(texto, patron),
                  lambda: rk.buscar_contando_colisiones(texto, patron, pre),
                  resultado.posiciones,
                  resultado.colisiones)


def _comprobar_que_coinciden(escenario: str, algoritmos: List[Etapas]) -> None:
    referencia = algoritmos[0].posiciones
    for etapas in algoritmos:
        if etapas.posiciones != referencia:
            raise ValueError(f"En el escenario '{escenario}' {etapas.algoritmo} no encuentra "
                             f"las mismas posiciones que {algoritmos[0].algoritmo}")


def medir_busqueda(texto: str, calentamiento: int = CALENTAMIENTO,
                   repeticiones: int = REPETICIONES) -> List[Fila]:
    """Mide los tres algoritmos en los cuatro escenarios, por etapa."""
    filas: List[Fila] = []
    for escenario, patron in crear_escenarios(texto).items():
        algoritmos = [_etapas_kmp(texto, patron),
                      _etapas_boyer_moore(texto, patron),
                      _etapas_rabin_karp(texto, patron)]
        _comprobar_que_coinciden(escenario, algoritmos)

        for etapas in algoritmos:
            preprocesamiento = medir(PREPROCESAMIENTO, calentamiento, repeticiones, etapas.preprocesamiento)
            busqueda = medir(BUSQUEDA, calentamiento, repeticiones, etapas.busqueda)
            filas.append(Fila(etapas.algoritmo, escenario, len(patron), len(texto),
                              PREPROCESAMIENTO, preprocesamiento))
            filas.append(Fila(etapas.algoritmo, escenario, len(patron), len(texto),
                              BUSQUEDA, busqueda, len(etapas.posiciones), etapas.colisiones))
    return filas


# --------------------------------------------------------------------------- #
# CSV y ejecucion
# --------------------------------------------------------------------------- #
def _vacio_si_none(valor: Optional[int]) -> str:
    return "" if valor is None else str(valor)


def escribir_csv(filas: List[Fila], destino: Path) -> None:
    """Mismas columnas y formato que el CSV de Java (6 decimales con punto)."""
    destino.parent.mkdir(parents=True, exist_ok=True)
    with open(destino, "w", newline="", encoding="utf-8") as salida:
        escritor = csv.writer(salida, lineterminator="\n")
        escritor.writerow(COLUMNAS)
        for fila in filas:
            m = fila.medicion
            escritor.writerow([fila.algoritmo, fila.escenario, fila.longitud_patron, fila.longitud_texto,
                               fila.etapa, m.repeticiones, f"{m.media_ms:.6f}", f"{m.desviacion_ms:.6f}",
                               f"{m.minimo_ms:.6f}", _vacio_si_none(fila.ocurrencias),
                               _vacio_si_none(fila.colisiones)])


def medir_y_guardar(carpeta_abstracts: Path, carpeta_resultados: Path) -> None:
    """Ejecuta la medicion completa, la imprime y la guarda en busqueda.csv."""
    texto = cargar_texto(carpeta_abstracts)
    print(f"Texto: {len(texto)} caracteres. Midiendo...")
    filas = medir_busqueda(texto)

    destino = carpeta_resultados / "busqueda.csv"
    escribir_csv(filas, destino)

    print(f"{'Algoritmo':<12} {'Escenario':<14} {'Etapa':<17} {'Media (ms)':>10} {'Minimo (ms)':>12}")
    for fila in filas:
        print(f"{fila.algoritmo:<12} {fila.escenario:<14} {fila.etapa:<17} "
              f"{fila.medicion.media_ms:>10.6f} {fila.medicion.minimo_ms:>12.6f}")
    print("\nEquipo:")
    for clave, valor in informacion_equipo().items():
        print(f"  {clave}: {valor}")
    print(f"Guardado en {destino.resolve()}")