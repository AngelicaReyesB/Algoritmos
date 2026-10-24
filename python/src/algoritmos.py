"""Algoritmos de ordenamiento NO basados en comparaciones.

Misma logica que la version Java (java/.../algoritmos):
- Reciben una lista de pares (termino, frecuencia) y devuelven una lista NUEVA,
  ordenada de MAYOR a MENOR frecuencia.
- Orden descendente con la clave  clave = frecuencia_maxima - frecuencia
  (ordenar ascendente por esa clave deja primero la frecuencia mas alta,
  sin invertir la lista, lo que romperia la estabilidad).
- Son estables: los empates conservan el orden de entrada.
- No usan sorted() ni list.sort().
"""

from typing import Callable, Dict, List, Tuple

Par = Tuple[str, int]  # (termino, frecuencia)


def _calcular_claves(terminos: List[Par]) -> Tuple[List[int], int]:
    """Devuelve las claves (frecuencia_maxima - frecuencia) y la clave maxima."""
    frecuencia_maxima = 0
    for _, frecuencia in terminos:
        if frecuencia > frecuencia_maxima:
            frecuencia_maxima = frecuencia
    claves = [frecuencia_maxima - frecuencia for _, frecuencia in terminos]
    clave_maxima = 0
    for clave in claves:
        if clave > clave_maxima:
            clave_maxima = clave
    return claves, clave_maxima


# --------------------------------------------------------------------------- #
# Counting Sort: Theta(n + k)
# --------------------------------------------------------------------------- #
def counting_sort(terminos: List[Par]) -> List[Par]:
    n = len(terminos)
    if n == 0:
        return []
    claves, k = _calcular_claves(terminos)

    conteo = [0] * (k + 1)
    for clave in claves:                 # contar cada clave
        conteo[clave] += 1
    for c in range(1, k + 1):            # sumas acumuladas -> posiciones finales
        conteo[c] += conteo[c - 1]

    salida: List[Par] = [None] * n       # type: ignore[list-item]
    for i in range(n - 1, -1, -1):       # de atras hacia adelante: estable
        conteo[claves[i]] -= 1
        salida[conteo[claves[i]]] = terminos[i]
    return salida


# --------------------------------------------------------------------------- #
# Radix Sort LSD, base 10: Theta(d * (n + b))
# --------------------------------------------------------------------------- #
BASE = 10


def _ordenar_por_digito(terminos: List[Par], claves: List[int], exp: int):
    """Counting Sort estable sobre un solo digito de las claves."""
    n = len(terminos)
    conteo = [0] * BASE
    for clave in claves:
        conteo[(clave // exp) % BASE] += 1
    for d in range(1, BASE):
        conteo[d] += conteo[d - 1]

    salida_terminos: List[Par] = [None] * n  # type: ignore[list-item]
    salida_claves = [0] * n
    for i in range(n - 1, -1, -1):
        digito = (claves[i] // exp) % BASE
        conteo[digito] -= 1
        salida_terminos[conteo[digito]] = terminos[i]
        salida_claves[conteo[digito]] = claves[i]
    return salida_terminos, salida_claves


def radix_sort(terminos: List[Par]) -> List[Par]:
    if not terminos:
        return []
    claves, clave_maxima = _calcular_claves(terminos)
    actuales = list(terminos)            # copia: no se modifica la entrada
    exp = 1
    while clave_maxima // exp > 0:       # una pasada por cada digito
        actuales, claves = _ordenar_por_digito(actuales, claves, exp)
        exp *= BASE
    return actuales


# --------------------------------------------------------------------------- #
# Bucket Sort: Theta(n) en promedio, O(n^2) en el peor caso
# --------------------------------------------------------------------------- #
def _insertion_sort(cubeta: List[Par]) -> None:
    """Insertion Sort estable: solo mueve si la frecuencia anterior es ESTRICTAMENTE menor."""
    for i in range(1, len(cubeta)):
        actual = cubeta[i]
        j = i - 1
        while j >= 0 and cubeta[j][1] < actual[1]:
            cubeta[j + 1] = cubeta[j]
            j -= 1
        cubeta[j + 1] = actual


def bucket_sort(terminos: List[Par]) -> List[Par]:
    n = len(terminos)
    if n == 0:
        return []
    claves, clave_maxima = _calcular_claves(terminos)

    m = n                                         # m = n cubetas
    cubetas: List[List[Par]] = [[] for _ in range(m)]
    for clave, termino in zip(claves, terminos):  # repartir en orden de entrada
        indice = clave * m // (clave_maxima + 1)
        cubetas[indice].append(termino)

    resultado: List[Par] = []
    for cubeta in cubetas:                        # ordenar cada cubeta y concatenar
        _insertion_sort(cubeta)
        resultado.extend(cubeta)
    return resultado


ALGORITMOS: Dict[str, Callable[[List[Par]], List[Par]]] = {
    "Counting Sort": counting_sort,
    "Radix Sort": radix_sort,
    "Bucket Sort": bucket_sort,
}