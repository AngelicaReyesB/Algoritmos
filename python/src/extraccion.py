"""Extraccion del abstract desde los PDF del corpus, con PyMuPDF.

Misma logica que ExtractorAbstract.java y CargaArticulos.java (java/.../pdf y servicio).

Los 20 articulos del corpus vienen de cinco editoriales (Elsevier, Springer,
MDPI, Wiley y Emerald). Cada una arma la primera pagina de forma distinta;
las reglas de abajo cubren esos formatos.
"""

import json
import re
import unicodedata
from pathlib import Path
from typing import Dict, List, Optional

# Donde empieza el abstract.
# "\s?" entre letras acepta tambien la forma espaciada de Elsevier: "A B S T R A C T"
_INICIO = re.compile(r"\ba\s?b\s?s\s?t\s?r\s?a\s?c\s?t\b|\bresumen\b", re.IGNORECASE)

# Donde termina el abstract: lo primero que aparezca de esta lista
_FIN = re.compile(
    r"\n\s*k\s?e\s?y\s?w\s?o\s?r\s?d\s?s?\b"   # linea que empieza con Keywords, Key words o K E Y W O R D S (Wiley)
    r"|\n\s*palabras clave\b"
    r"|\b1\s*\.\s*introduction\b"              # "1. Introduction", tambien pegado: "texto.1. Introduction"
    r"|\n\s*introduction\b|\n\s*introducci[oó]n\b"  # linea que empieza con Introduction
    r"|(?<=\.) {3,}\n",                        # Elsevier: el abstract termina en ".   " (tres espacios)
    re.IGNORECASE,
)

# Una linea de palabras clave en la columna "Article info" de Elsevier es corta
_LARGO_LINEA_PALABRA_CLAVE = 45

# Si no se encuentra el fin, se toman como maximo estos caracteres
_LONGITUD_MAXIMA = 2500


def leer_primeras_paginas(ruta_pdf: Path, paginas: int = 2) -> str:
    """Texto de las primeras paginas del PDF (ahi esta el abstract)."""
    import pymupdf  # se importa aqui: solo se necesita al leer PDF

    with pymupdf.open(ruta_pdf) as documento:
        return "\n".join(documento[i].get_text() for i in range(min(paginas, len(documento))))


def normalizar(texto: str) -> str:
    """Convierte ligaduras tipograficas en letras normales: 'ﬁ' -> 'fi'."""
    return unicodedata.normalize("NFKC", texto)


def extraer_abstract(texto: str) -> Optional[str]:
    """Devuelve el abstract, o None si el texto no tiene la palabra 'Abstract'."""
    limpio = normalizar(texto)

    inicio = _INICIO.search(limpio)
    if not inicio:
        return None
    resto = saltar_palabras_clave(limpio[inicio.end():])

    fin = _FIN.search(resto)
    resumen = resto[: fin.start()] if fin else resto[:_LONGITUD_MAXIMA]

    resumen = unir_palabras_cortadas(resumen)
    resumen = re.sub(r"\s+", " ", resumen).strip(" :.-—∗*")
    return resumen or None


def saltar_palabras_clave(resto: str) -> str:
    """En algunos articulos de Elsevier la lista de palabras clave queda
    justo despues del titulo "A B S T R A C T":

        Keywords:
        Human-centered AI
        AI in education
        The rapid expansion of Learning Analytics ...   <- aqui empieza el abstract

    Se salta la linea "Keywords:" y las lineas cortas que la siguen.
    """
    lineas = resto.strip().split("\n")
    if not lineas[0].lower().startswith("keywords"):
        return resto
    i = 1
    while i < len(lineas) and len(lineas[i].strip()) < _LARGO_LINEA_PALABRA_CLAVE:
        i += 1
    return "\n".join(lineas[i:])


def unir_palabras_cortadas(texto: str) -> str:
    """Une las palabras que el PDF corta al final de una linea.

    1. Con guion visible: "gener-\\native" -> "generative".
    2. Elsevier: el guion no se lee, pero cada linea normal termina con un
       espacio. Si la mayoria de lineas termina en espacio, una linea que
       termina sin espacio es una palabra cortada: "the in\\nclusion" -> "the inclusion".
    """
    texto = re.sub(r"-\n(?=[a-z])", "", texto)

    lineas = texto.split("\n")
    con_espacio_final = sum(1 for linea in lineas if linea.endswith(" "))
    if con_espacio_final > len(lineas) // 2:
        texto = re.sub(r"(?<=[a-z])\n(?=[a-z])", "", texto)
    return texto


def numero_del_archivo(nombre: str) -> int:
    """Numero al inicio del nombre: '12. A meta review.pdf' -> 12. Sin numero: -1."""
    coincidencia = re.match(r"\d+", nombre)
    return int(coincidencia.group()) if coincidencia else -1


def listar_archivos(carpeta: Path, patron: str) -> List[Path]:
    """Archivos ordenados por el numero del inicio del nombre (1, 2, ..., 10, 11).

    Sin esto el orden alfabetico seria 1, 10, 11, ..., 2, 20, 3.
    """
    return sorted(carpeta.glob(patron), key=lambda archivo: (numero_del_archivo(archivo.name), archivo.name))


def cargar_metadatos(archivo_metadatos: Path) -> Dict[int, dict]:
    """Lee data/metadatos.json: numero del articulo -> {"titulo", "autores"}."""
    if not archivo_metadatos.exists():
        print(f"[!] No existe el archivo de metadatos: {archivo_metadatos}")
        return {}
    articulos = json.loads(archivo_metadatos.read_text(encoding="utf-8"))
    return {articulo["numero"]: articulo for articulo in articulos}


def extraer_corpus(carpeta_pdfs: Path, carpeta_abstracts: Path, archivo_metadatos: Path) -> int:
    """Extrae el abstract de cada PDF y lo guarda en un JSON.

    El titulo y los autores salen de data/metadatos.json.
    Los JSON con "manual": true (corregidos a mano) no se sobrescriben.
    """
    carpeta_abstracts.mkdir(parents=True, exist_ok=True)
    pdfs = listar_archivos(carpeta_pdfs, "*.pdf")
    if not pdfs:
        print(f"[!] No hay PDF en {carpeta_pdfs}")
        return 0

    metadatos = cargar_metadatos(archivo_metadatos)
    extraidos = 0
    for pdf in pdfs:
        archivo_json = carpeta_abstracts / f"{pdf.stem}.json"
        if archivo_json.exists() and json.loads(archivo_json.read_text(encoding="utf-8")).get("manual"):
            print(f"[=] {pdf.name}: corregido a mano, se conserva")
            extraidos += 1
            continue

        resumen = extraer_abstract(leer_primeras_paginas(pdf))
        if resumen is None:
            print(f"[!] Sin abstract detectado: {pdf.name} (revisar manualmente)")
            continue

        datos = metadatos.get(numero_del_archivo(pdf.name))
        if datos is None:
            print(f"[!] Sin metadatos: {pdf.name}")
        registro = {
            "file": pdf.name,
            "title": datos["titulo"] if datos else pdf.stem,
            "authors": datos["autores"] if datos else None,
            "abstract": resumen,
        }
        archivo_json.write_text(json.dumps(registro, ensure_ascii=False, indent=2), encoding="utf-8")
        extraidos += 1

    print(f"Abstracts extraidos: {extraidos}/{len(pdfs)}")
    return extraidos


def cargar_abstracts(carpeta_abstracts: Path) -> List[str]:
    """Lee los abstracts de los JSON, en el mismo orden que Java (1, 2, ..., 20)."""
    return [
        json.loads(archivo.read_text(encoding="utf-8"))["abstract"]
        for archivo in listar_archivos(carpeta_abstracts, "*.json")
    ]