"""Extraccion del abstract (y titulo) desde los PDF del corpus, con PyMuPDF.

Misma logica que ExtractorAbstract.java (java/.../pdf).
"""

import json
import re
import unicodedata
from pathlib import Path
from typing import List, Optional

# Donde empieza el abstract
_INICIO = re.compile(r"\babstract\b|\bresumen\b", re.IGNORECASE)

# Donde termina el abstract: lo primero que aparezca de esta lista
_FIN = re.compile(
    r"\bkey\s?words?\b|\bindex terms\b|\bpalabras clave\b"
    r"|\n\s*(?:1|I)\s*[\.\-]?\s*introduction\b|\bintroduction\b|\bintroducci[oó]n\b"
    r"|\bccs\s+concepts\b"                       # formato ACM
    r"|\bequal contribution\b"                   # nota al pie de autores
    r"|\b\d+(?:st|nd|rd|th)\s+conference on\b"   # nota al pie de la conferencia
    r"|\bpreprint\b"                             # "Preprint. Under review."
    r"|\n\s*contents\s*\n"                       # indice de contenidos
    r"|\bfig(?:ure|\.)\s*1\s*[:.]"               # leyenda de la Figura 1
    r"|\n\s*[QA]:\s",                            # ejemplos de una figura (Q: / A:)
    re.IGNORECASE,
)

# Sello lateral que arXiv agrega en la primera pagina
_SELLO_ARXIV = re.compile(r"arXiv:\d{4}\.\d{4,5}(?:v\d+)?\s*\[[^\]]+\]\s*\d{1,2}\s+\w{3}\s+\d{4}")

# Aviso de copyright de ACM que interrumpe el abstract
_PERMISO_ACM = re.compile(
    r"Permission to make digital or hard copies.*?https?://doi\.org/\S+",
    re.IGNORECASE | re.DOTALL,
)

# Si no se encuentra el fin, se toman como maximo estos caracteres
_LONGITUD_MAXIMA = 2500


def leer_primeras_paginas(ruta_pdf: Path, paginas: int = 2) -> str:
    """Texto de las primeras paginas del PDF (ahi estan el titulo y el abstract)."""
    import pymupdf  # se importa aqui: solo se necesita al leer PDF

    with pymupdf.open(ruta_pdf) as documento:
        return "\n".join(documento[i].get_text() for i in range(min(paginas, len(documento))))


def normalizar(texto: str) -> str:
    """Convierte ligaduras tipograficas en letras normales: 'ﬁ' -> 'fi'."""
    return unicodedata.normalize("NFKC", texto)


def extraer_abstract(texto: str) -> Optional[str]:
    """Devuelve el abstract, o None si el texto no tiene la palabra 'Abstract'."""
    limpio = normalizar(texto)
    limpio = _SELLO_ARXIV.sub(" ", limpio)
    limpio = _PERMISO_ACM.sub(" ", limpio)

    inicio = _INICIO.search(limpio)
    if not inicio:
        return None
    resto = limpio[inicio.end():]

    fin = _FIN.search(resto)
    resumen = resto[: fin.start()] if fin else resto[:_LONGITUD_MAXIMA]
    resumen = re.sub(r"-\n(?=[a-z])", "", resumen)        # une palabras cortadas
    resumen = re.sub(r"\s+", " ", resumen).strip(" :.-—∗*")
    return resumen or None


def extraer_titulo(texto: str) -> str:
    """Titulo aproximado: la primera linea no vacia (limitacion conocida)."""
    for linea in normalizar(texto).splitlines():
        if linea.strip():
            return linea.strip()
    return ""


def extraer_corpus(carpeta_pdfs: Path, carpeta_abstracts: Path) -> int:
    """Extrae el abstract de cada PDF y lo guarda en un JSON.

    Los JSON con "manual": true (corregidos a mano) no se sobrescriben.
    """
    carpeta_abstracts.mkdir(parents=True, exist_ok=True)
    pdfs = sorted(carpeta_pdfs.glob("*.pdf"))
    if not pdfs:
        print(f"[!] No hay PDF en {carpeta_pdfs}")
        return 0

    extraidos = 0
    for pdf in pdfs:
        archivo_json = carpeta_abstracts / f"{pdf.stem}.json"
        if archivo_json.exists() and json.loads(archivo_json.read_text(encoding="utf-8")).get("manual"):
            print(f"[=] {pdf.name}: corregido a mano, se conserva")
            extraidos += 1
            continue

        texto = leer_primeras_paginas(pdf)
        resumen = extraer_abstract(texto)
        if resumen is None:
            print(f"[!] Sin abstract detectado: {pdf.name} (revisar manualmente)")
            continue

        registro = {"file": pdf.name, "title": extraer_titulo(texto), "abstract": resumen}
        archivo_json.write_text(json.dumps(registro, ensure_ascii=False, indent=2), encoding="utf-8")
        extraidos += 1

    print(f"Abstracts extraidos: {extraidos}/{len(pdfs)}")
    return extraidos


def cargar_abstracts(carpeta_abstracts: Path) -> List[str]:
    """Lee los abstracts de los JSON, ordenados por nombre de archivo."""
    return [
        json.loads(archivo.read_text(encoding="utf-8"))["abstract"]
        for archivo in sorted(carpeta_abstracts.glob("*.json"))
    ]