"""Extraccion del abstract (y titulo) desde los PDFs del corpus."""

import json
import re
import sys
from pathlib import Path
from typing import Optional

_START = re.compile(r"\babstract\b|\bresumen\b", re.IGNORECASE)
_END = re.compile(
    r"\bkey\s?words?\b|\bindex terms\b|\bpalabras clave\b|"
    r"\n\s*(?:1|I)\s*[\.\-]?\s*introduction\b|\bintroduction\b|\bintroducci[oó]n\b",
    re.IGNORECASE,
)


def read_first_pages(pdf_path: Path, pages: int = 2) -> str:
    import fitz  # PyMuPDF (import perezoso: solo se necesita al leer PDFs)

    with fitz.open(pdf_path) as doc:
        return "\n".join(doc[i].get_text() for i in range(min(pages, len(doc))))


def extract_abstract(text: str) -> Optional[str]:
    m = _START.search(text)
    if not m:
        return None
    rest = text[m.end():]
    e = _END.search(rest)
    abstract = rest[: e.start()] if e else rest[:2500]
    abstract = re.sub(r"-\n(?=[a-z])", "", abstract)      # une palabras cortadas
    abstract = re.sub(r"\s+", " ", abstract).strip(" :.-—")
    return abstract or None


def extract_title(text: str) -> str:
    """Heuristica simple: primera linea no vacia. Revisar a mano (se usara en otros requerimientos)."""
    for line in text.splitlines():
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
        text = read_first_pages(pdf)
        abstract = extract_abstract(text)
        if abstract is None:
            print(f"[!] Sin abstract detectado: {pdf.name} (revisar manualmente)")
            continue
        record = {"file": pdf.name, "title": extract_title(text), "abstract": abstract}
        (out_dir / f"{pdf.stem}.json").write_text(
            json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8"
        )
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
