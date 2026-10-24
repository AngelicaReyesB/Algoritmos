"""Preprocesamiento de texto y conteo de frecuencias.

Mismas reglas que el tokenizador propio de Java (java/.../texto):
1. Normaliza ligaduras de los PDF (NFKC): "ﬁ" -> "fi".
2. Pasa todo a minusculas.
3. Elimina URLs.
4. Recorre el texto caracter por caracter: solo las letras forman palabras.
5. Descarta palabras de menos de 2 letras (se conservan "ai" y "llm").
6. Descarta palabras vacias (stopwords).
No aplica lematizacion ni stemming.
"""

import re
import unicodedata
from typing import Dict, Iterable, List, Tuple

LONGITUD_MINIMA = 2

# Palabras vacias: exactamente las mismas listas que PalabrasVacias.java
BASE = set("""
a about above after again all also am an and any are as at be because been before being
between both but by can could did do does doing during each few for from further had has
have having he her here hers him his how i if in into is it its itself just may me more
most my no nor not of off on once only or other our out over own same she should so some
such than that the their them then there these they this those through to too under until
up us very was we were what when where which while who whom why will with would you your
among within without via
de la que el en y los se del las un por con una su para es al lo como mas pero sus le ya
este si porque esta entre cuando muy sin sobre tambien hasta hay donde quien desde todo
nos durante todos uno les ni contra otros ese eso ante ellos esto antes algunos
""".split())

DOMINIO = set("""
paper study studies result results approach approaches show shows shown use used uses
using based propose proposed present presents work however
""".split())

PALABRAS_VACIAS = BASE | DOMINIO

# re.ASCII: \w y \b se comportan igual que en Java (solo letras y digitos ASCII)
_URL = re.compile(r"https?://\S+|www\.\S+|\b[\w.-]+\.(?:com|org|net|io|edu|google)\b\S*", re.ASCII)


def _agregar_si_es_valida(palabra: str, terminos: List[str]) -> None:
    """Pasos 5 y 6: longitud minima y palabras vacias."""
    if len(palabra) >= LONGITUD_MINIMA and palabra not in PALABRAS_VACIAS:
        terminos.append(palabra)


def tokenizar(texto: str) -> List[str]:
    limpio = unicodedata.normalize("NFKC", texto)   # paso 1
    limpio = limpio.lower()                          # paso 2
    limpio = _URL.sub(" ", limpio)                   # paso 3

    terminos: List[str] = []
    palabra: List[str] = []
    for caracter in limpio:                          # paso 4
        if caracter.isalpha():
            palabra.append(caracter)                 # la palabra continua
        else:
            _agregar_si_es_valida("".join(palabra), terminos)  # la palabra termino
            palabra = []
    _agregar_si_es_valida("".join(palabra), terminos)          # ultima palabra
    return terminos


def tokenizar_todos(textos: Iterable[str]) -> List[str]:
    """Preprocesamiento: une en una sola lista los terminos de todos los textos."""
    terminos: List[str] = []
    for texto in textos:
        terminos.extend(tokenizar(texto))
    return terminos


def contar_terminos(terminos: Iterable[str]) -> List[Tuple[str, int]]:
    """Conteo: pares (termino, frecuencia) en orden de PRIMERA APARICION.

    Un dict de Python conserva el orden de insercion (como LinkedHashMap en Java).
    Ese orden es el criterio de desempate de los tres algoritmos estables.
    """
    frecuencias: Dict[str, int] = {}
    for termino in terminos:
        frecuencias[termino] = frecuencias.get(termino, 0) + 1
    return list(frecuencias.items())


def contar_frecuencias(textos: Iterable[str]) -> List[Tuple[str, int]]:
    """Tokeniza todos los textos y cuenta los terminos."""
    return contar_terminos(tokenizar_todos(textos))