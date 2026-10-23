"""Algoritmos de ordenamiento NO basados en comparaciones directas entre elementos.

Todos reciben una lista de pares (termino, frecuencia) y devuelven una NUEVA lista
ordenada por frecuencia DESCENDENTE.

Decisiones de diseno (documentar en el informe):
  * Orden descendente: se ordena ascendentemente por la clave k' = max_freq - freq,
    de modo que la frecuencia mayor queda primero sin invertir la lista (invertir
    destruiria la estabilidad).
  * Estabilidad: los tres algoritmos son estables, por lo que los empates conservan
    el orden de entrada (orden de primera aparicion en el corpus). Asi los tres
    producen exactamente la misma salida y el Top 15 es comparable.
  * No se usan sorted(), list.sort() ni Counter.most_common().
"""

from typing import List, Tuple

Pair = Tuple[str, int]


def _keys_desc(pairs: List[Pair]) -> Tuple[List[int], int]:
    """Convierte frecuencias en claves ascendentes: k' = max_freq - freq (>= 0)."""
    max_freq = 0
    for _, f in pairs:
        if f > max_freq:
            max_freq = f
    return [max_freq - f for _, f in pairs], max_freq


# --------------------------------------------------------------------------- #
# Counting Sort: O(n + k), k = rango de claves
# --------------------------------------------------------------------------- #
def counting_sort(pairs: List[Pair]) -> List[Pair]:
    n = len(pairs)
    if n == 0:
        return []
    keys, max_key = _keys_desc(pairs)

    count = [0] * (max_key + 1)
    for key in keys:                      # 1) histograma de claves
        count[key] += 1
    for i in range(1, max_key + 1):       # 2) sumas acumuladas -> posiciones finales
        count[i] += count[i - 1]

    out: List[Pair] = [None] * n          # type: ignore[list-item]
    for i in range(n - 1, -1, -1):        # 3) recorrido inverso => estable
        count[keys[i]] -= 1
        out[count[keys[i]]] = pairs[i]
    return out


# --------------------------------------------------------------------------- #
# Radix Sort (LSD, base 10): O(d * (n + b)), d = digitos, b = base
# --------------------------------------------------------------------------- #
def _counting_by_digit(pairs: List[Pair], keys: List[int], exp: int, base: int):
    n = len(pairs)
    count = [0] * base
    for key in keys:
        count[(key // exp) % base] += 1
    for i in range(1, base):
        count[i] += count[i - 1]

    out_pairs: List[Pair] = [None] * n    # type: ignore[list-item]
    out_keys: List[int] = [0] * n
    for i in range(n - 1, -1, -1):
        d = (keys[i] // exp) % base
        count[d] -= 1
        out_pairs[count[d]] = pairs[i]
        out_keys[count[d]] = keys[i]
    return out_pairs, out_keys


def radix_sort(pairs: List[Pair], base: int = 10) -> List[Pair]:
    if not pairs:
        return []
    keys, max_key = _keys_desc(pairs)
    cur_pairs, cur_keys = list(pairs), keys
    exp = 1
    while max_key // exp > 0:             # una pasada estable por cada digito
        cur_pairs, cur_keys = _counting_by_digit(cur_pairs, cur_keys, exp, base)
        exp *= base
    return cur_pairs


# --------------------------------------------------------------------------- #
# Bucket Sort: O(n + k) en promedio con distribucion uniforme
# --------------------------------------------------------------------------- #
def _insertion_sort_stable(bucket: List[Tuple[int, Pair]]) -> None:
    """Insertion sort estable (desplaza solo si es estrictamente mayor)."""
    for i in range(1, len(bucket)):
        item = bucket[i]
        j = i - 1
        while j >= 0 and bucket[j][0] > item[0]:
            bucket[j + 1] = bucket[j]
            j -= 1
        bucket[j + 1] = item


def bucket_sort(pairs: List[Pair]) -> List[Pair]:
    n = len(pairs)
    if n == 0:
        return []
    keys, max_key = _keys_desc(pairs)

    num_buckets = n
    buckets: List[List[Tuple[int, Pair]]] = [[] for _ in range(num_buckets)]
    for key, pair in zip(keys, pairs):    # 1) distribuir por rango (preserva orden)
        idx = key * num_buckets // (max_key + 1)
        buckets[idx].append((key, pair))

    out: List[Pair] = []
    for bucket in buckets:                # 2) ordenar cada cubeta y 3) concatenar
        _insertion_sort_stable(bucket)
        out.extend(p for _, p in bucket)
    return out


ALGORITHMS = {
    "Counting Sort": counting_sort,
    "Radix Sort": radix_sort,
    "Bucket Sort": bucket_sort,
}
