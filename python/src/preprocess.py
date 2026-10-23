"""Preprocesamiento de texto y conteo de frecuencias."""

import re
from typing import Dict, Iterable, List, Tuple

# Lista reducida de stopwords (ingles + espanol). Ajustar y justificar en el informe.
STOPWORDS = set("""
a about above after again all also am an and any are as at be because been before being
between both but by can could did do does doing during each few for from further had has
have having he her here hers him his how i if in into is it its itself just may me more
most my no nor not of off on once only or other our out over own same she should so some
such than that the their them then there these they this those through to too under until
up use used using very was we were what when where which while who whom why will with would
you your among within based paper study studies result results approach shown show
de la que el en y a los se del las un por con no una su para es al lo como mas pero sus le
ya o este si porque esta entre cuando muy sin sobre tambien me hasta hay donde quien desde
todo nos durante todos uno les ni contra otros ese eso ante ellos e esto antes algunos que
""".split())

_TOKEN_RE = re.compile(r"[a-záéíóúüñ]+")


def tokenize(text: str, min_len: int = 3) -> List[str]:
    """Minusculas, solo letras, sin stopwords, longitud minima."""
    text = text.lower()
    return [t for t in _TOKEN_RE.findall(text) if len(t) >= min_len and t not in STOPWORDS]


def word_frequencies(texts: Iterable[str]) -> List[Tuple[str, int]]:
    """Devuelve pares (termino, frecuencia) en orden de PRIMERA APARICION.

    Ese orden es el criterio de desempate de los tres algoritmos estables.
    """
    freq: Dict[str, int] = {}          # dict preserva orden de insercion
    for text in texts:
        for tok in tokenize(text):
            freq[tok] = freq.get(tok, 0) + 1
    return list(freq.items())
