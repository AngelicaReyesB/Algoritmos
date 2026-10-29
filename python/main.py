"""Version Python del ranking de terminos (comparacion con la version Java).

Uso (desde la carpeta python/):
    python main.py extraer    PDF de data/pdfs -> abstracts JSON en data/abstracts
    python main.py medir      tiempos por etapa, validacion del Top 15 y graficos
"""

import argparse
from pathlib import Path

from src.extraccion import extraer_corpus
from src.medicion import medir_todo

RAIZ = Path(__file__).resolve().parent.parent  # raiz del repositorio
CARPETA_PDFS = RAIZ / "data" / "pdfs"
CARPETA_ABSTRACTS = RAIZ / "data" / "abstracts"
ARCHIVO_METADATOS = RAIZ / "data" / "metadatos.json"
CARPETA_RESULTADOS = RAIZ / "results" / "python"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("comando", choices=["extraer", "medir"])
    argumentos = parser.parse_args()

    if argumentos.comando == "extraer":
        extraer_corpus(CARPETA_PDFS, CARPETA_ABSTRACTS, ARCHIVO_METADATOS)
    else:
        coinciden = medir_todo(CARPETA_PDFS, CARPETA_ABSTRACTS, CARPETA_RESULTADOS)
        if not coinciden:
            raise SystemExit("Los rankings NO coinciden")


if __name__ == "__main__":
    main()