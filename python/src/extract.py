"""Extraccion del abstract (y titulo) desde los PDFs del corpus."""

import json
import re
import sys
import unicodedata
from pathlib import Path
from typing import Optional

# Inicio del abstract
_START = re.compile(r"\babstract\b|\bresumen\b", re.IGNORECASE)

# Fin del abstract: palabras clave, introduccion, notas al pie, indice, figuras o bloque ACM
_END = re.compile(
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
_ARXIV_STAMP = re.compile(r"arXiv:\d{4}\.\d{4,5}(?:v\d+)?\s*\[[^\]]+\]\s*\d{1,2}\s+\w{3}\s+\d{4}")

# Aviso de copyright de ACM que interrumpe el abstract
_ACM_PERMISSION = re.compile(
    r"Permission to make digital or hard copies.*?https?://doi\.org/\S+",
    re.IGNORECASE | re.DOTALL,
)


def read_first_pages(pdf_path: Path, pages: int = 2) -> str:
    import pymupdf  # PyMuPDF (import perezoso: solo se necesita al leer PDFs)

    with pymupdf.open(pdf_path) as doc:
        return "\n".join(doc[i].get_text() for i in range(min(pages, len(doc))))


def normalize_text(text: str) -> str:
    """Convierte ligaduras tipograficas en letras normales: 'ﬁ' -> 'fi', 'ﬂ' -> 'fl'."""
    return unicodedata.normalize("NFKC", text)


def extract_abstract(text: str) -> Optional[str]:
    text = normalize_text(text)
    text = _ARXIV_STAMP.sub(" ", text)
    text = _ACM_PERMISSION.sub(" ", text)
    m = _START.search(text)
    if not m:
        return None
    rest = text[m.end():]
    e = _END.search(rest)
    abstract = rest[: e.start()] if e else rest[:2500]
    abstract = re.sub(r"-\n(?=[a-z])", "", abstract)      # une palabras cortadas
    abstract = re.sub(r"\s+", " ", abstract).strip(" :.-—∗*")
    return abstract or None


def extract_title(text: str) -> str:
    """Heuristica simple: primera linea no vacia. Revisar a mano (se usara en otros requerimientos)."""
    for line in normalize_text(text).splitlines():
        if line.strip():
            return line.strip()
    return ""


def extract_corpus(pdf_dir: Path, out_dir: Path) -> int:
    out_dir.mkdir(parents=True, exist_ok=True)
    pdfs = sorted(pdf_dir.glob("*.pdf"))
    if not pdfs:
        print(f"[!] No hay PDFs en {pdf_dir}")
        return 0
    ok = 0
    for pdf in pdfs:
        out_file = out_dir / f"{pdf.stem}.json"
        if out_file.exists() and json.loads(out_file.read_text(encoding="utf-8")).get("manual"):
            print(f"[=] {pdf.name}: corregido a mano, se conserva")
            ok += 1
            continue
        text = read_first_pages(pdf)
        abstract = extract_abstract(text)
        if abstract is None:
            print(f"[!] Sin abstract detectado: {pdf.name} (revisar manualmente)")
            continue
        record = {"file": pdf.name, "title": extract_title(text), "abstract": abstract}
        out_file.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
        ok += 1
    print(f"Abstracts extraidos: {ok}/{len(pdfs)}")
    return ok


def load_abstracts(abs_dir: Path):
    return [
        json.loads(p.read_text(encoding="utf-8"))["abstract"]
        for p in sorted(abs_dir.glob("*.json"))
    ]


if __name__ == "__main__":
    base = Path(__file__).resolve().parent.parent.parent
    sys.exit(0 if extract_corpus(base / "data" / "pdfs", base / "data" / "abstracts") else 1)