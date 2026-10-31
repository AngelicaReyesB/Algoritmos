"""Algoritmos de busqueda de patrones (Requerimiento 3): KMP, Boyer-Moore y Rabin-Karp.

Misma logica que la version Java (java/.../busqueda), para que la comparacion
de tiempos entre lenguajes sea justa:
- Busqueda exacta: distingue mayusculas y minusculas.
- Devuelven las posiciones (desde 0) de TODAS las ocurrencias, incluidas las solapadas.
- Patron vacio o mas largo que el texto: lista vacia.
- Preprocesamiento y busqueda separados, para medirlos por separado.
- Implementados a mano: no usan find, index, count, `in` sobre cadenas ni re.

Notacion: n = largo del texto, m = largo del patron.
"""

from typing import Dict, List, NamedTuple


def _comprobar_no_nulos(texto: str, patron: str) -> None:
    if texto is None or patron is None:
        raise ValueError("El texto y el patron no pueden ser nulos")


# --------------------------------------------------------------------------- #
# Knuth-Morris-Pratt: preprocesamiento O(m), busqueda O(n)
# --------------------------------------------------------------------------- #
class KnuthMorrisPratt:
    """Cuando una comparacion falla, el texto NUNCA retrocede: la tabla de
    prefijos indica a que posicion del patron saltar."""

    nombre = "KMP"

    def construir_tabla_prefijos(self, patron: str) -> List[int]:
        """prefijo[i] = largo del borde mas largo de patron[0..i]
        (prefijo que tambien es sufijo, sin ser el fragmento completo).

        Ejemplo con "ababaca": [0, 0, 1, 2, 3, 0, 1]
        """
        m = len(patron)
        prefijo = [0] * m
        k = 0  # largo del borde que llevamos hasta ahora
        for i in range(1, m):
            # Si no extiende el borde actual, probamos con el borde mas corto
            while k > 0 and patron[i] != patron[k]:
                k = prefijo[k - 1]
            # Si extiende el borde, crece en uno
            if patron[i] == patron[k]:
                k += 1
            prefijo[i] = k
        return prefijo

    def buscar(self, texto: str, patron: str) -> List[int]:
        """Preprocesamiento + busqueda."""
        _comprobar_no_nulos(texto, patron)
        if len(patron) == 0 or len(patron) > len(texto):
            return []
        return self.buscar_con_tabla(texto, patron, self.construir_tabla_prefijos(patron))

    def buscar_con_tabla(self, texto: str, patron: str, prefijo: List[int]) -> List[int]:
        """Recorre el texto una sola vez con la tabla ya construida."""
        _comprobar_no_nulos(texto, patron)
        posiciones: List[int] = []
        n, m = len(texto), len(patron)
        if m == 0 or m > n:
            return posiciones

        j = 0  # cuantos caracteres del patron coinciden en este momento
        for i in range(n):
            # Fallo: en vez de retroceder en el texto, retrocedemos j en el patron
            while j > 0 and texto[i] != patron[j]:
                j = prefijo[j - 1]
            # Acierto: avanzamos en el patron
            if texto[i] == patron[j]:
                j += 1
            # Patron completo: guardamos donde empezo y seguimos (permite solapamiento)
            if j == m:
                posiciones.append(i - m + 1)
                j = prefijo[j - 1]
        return posiciones


# --------------------------------------------------------------------------- #
# Boyer-Moore: preprocesamiento O(m^3), busqueda peor O(nm) y mejor O(n/m)
# --------------------------------------------------------------------------- #
class BoyerMoore:
    """Compara de DERECHA A IZQUIERDA. Al fallar, avanza con el mayor de dos
    desplazamientos: regla del caracter malo y regla del sufijo bueno."""

    nombre = "Boyer-Moore"

    def construir_tabla_caracter_malo(self, patron: str) -> Dict[str, int]:
        """Para cada caracter del patron, la posicion de su ULTIMA aparicion.

        Ejemplo con "GCAGAGAG": {G: 7, C: 1, A: 6}
        """
        ultima_posicion: Dict[str, int] = {}
        for i in range(len(patron)):
            ultima_posicion[patron[i]] = i  # al repetirse, se queda la ultima
        return ultima_posicion

    def construir_tabla_sufijo_bueno(self, patron: str) -> List[int]:
        """sufijo_bueno[k] = desplazamiento cuando ya coincidieron los ultimos k
        caracteres y el siguiente (hacia la izquierda) fallo. k va de 0 a m.

        Es el MENOR s (1 <= s <= m) que cumple:
          (a) el patron movido s posiciones sigue coincidiendo con los k caracteres ya leidos;
          (b) el caracter que quedaria en la posicion del fallo es distinto del que fallo.
        Si ninguno sirve, s = m. Se construye desde la definicion: O(m^3).

        Ejemplo con "aaaa": [4, 3, 2, 1, 1]
        """
        m = len(patron)
        tabla = [0] * (m + 1)
        for k in range(m + 1):
            j = m - 1 - k  # posicion del fallo (-1 si coincidio el patron completo)
            s = 1
            while s < m and not self._desplazamiento_valido(patron, k, j, s):
                s += 1
            tabla[k] = s
        return tabla

    @staticmethod
    def _desplazamiento_valido(patron: str, k: int, j: int, s: int) -> bool:
        """Comprueba las condiciones (a) y (b) de la tabla del sufijo bueno."""
        m = len(patron)
        # (a) el patron desplazado sigue coincidiendo con el sufijo ya leido
        for i in range(m - k, m):
            if i - s >= 0 and patron[i - s] != patron[i]:
                return False
        # (b) el caracter que fallo no puede volver a quedar en la posicion del fallo
        if j >= 0 and j - s >= 0 and patron[j - s] == patron[j]:
            return False
        return True

    def buscar(self, texto: str, patron: str) -> List[int]:
        """Preprocesamiento (las dos tablas) + busqueda."""
        _comprobar_no_nulos(texto, patron)
        if len(patron) == 0 or len(patron) > len(texto):
            return []
        return self.buscar_con_tablas(texto, patron,
                                      self.construir_tabla_caracter_malo(patron),
                                      self.construir_tabla_sufijo_bueno(patron))

    def buscar_con_tablas(self, texto: str, patron: str,
                          caracter_malo: Dict[str, int], sufijo_bueno: List[int]) -> List[int]:
        """Busqueda con las dos tablas ya construidas."""
        _comprobar_no_nulos(texto, patron)
        posiciones: List[int] = []
        n, m = len(texto), len(patron)
        if m == 0 or m > n:
            return posiciones

        s = 0  # posicion del texto donde esta alineado el inicio del patron
        while s <= n - m:
            # Se compara de derecha a izquierda
            j = m - 1
            while j >= 0 and patron[j] == texto[s + j]:
                j -= 1

            if j < 0:
                # Coincidio todo el patron: guardamos y desplazamos segun el sufijo bueno
                posiciones.append(s)
                s += sufijo_bueno[m]
            else:
                # Fallo en la posicion j del patron, tras k coincidencias
                k = m - 1 - j
                ultima = caracter_malo.get(texto[s + j], -1)
                desplazamiento_caracter_malo = max(1, j - ultima)
                desplazamiento_sufijo_bueno = sufijo_bueno[k]
                # Se avanza con el mayor de los dos desplazamientos
                s += max(desplazamiento_caracter_malo, desplazamiento_sufijo_bueno)
        return posiciones


# --------------------------------------------------------------------------- #
# Rabin-Karp: preprocesamiento O(m), busqueda promedio O(n) y peor O(nm)
# --------------------------------------------------------------------------- #
class Preprocesamiento(NamedTuple):
    peso_primero: int          # h = BASE^(m-1) mod modulo
    hash_patron: int
    hash_primera_ventana: int


class ResultadoRabinKarp(NamedTuple):
    posiciones: List[int]
    colisiones: int            # hash igual pero texto distinto


class RabinKarp:
    """Compara un numero (hash) que resume cada ventana del texto. Solo si el
    hash es igual al del patron se comparan los caracteres; si el texto es
    distinto, se cuenta una COLISION.

    Hash rodante: siguiente = ((anterior - c[i] * h) * BASE + c[i+m]) mod modulo
    """

    nombre = "Rabin-Karp"
    BASE = 256
    MODULO_POR_DEFECTO = 101

    def __init__(self, modulo: int = MODULO_POR_DEFECTO):
        if modulo < 1:
            raise ValueError("El modulo debe ser mayor o igual a 1")
        self.modulo = modulo

    def calcular_hash(self, texto: str, inicio: int, largo: int) -> int:
        """hash = (hash * BASE + codigo del caracter) mod modulo, de izquierda a derecha.

        Ejemplo con base 256 y modulo 101: "ab" -> (97 * 256 + 98) mod 101 = 84
        """
        valor = 0
        for i in range(inicio, inicio + largo):
            valor = (valor * self.BASE + ord(texto[i])) % self.modulo
        return valor

    def preprocesar(self, texto: str, patron: str) -> Preprocesamiento:
        """Etapa 1: h, hash del patron y hash de la primera ventana."""
        _comprobar_no_nulos(texto, patron)
        m = len(patron)
        if m == 0 or m > len(texto):
            return Preprocesamiento(1, 0, 0)  # no habra busqueda: no se usa

        h = 1
        for _ in range(1, m):
            h = (h * self.BASE) % self.modulo
        return Preprocesamiento(h, self.calcular_hash(patron, 0, m), self.calcular_hash(texto, 0, m))

    def buscar(self, texto: str, patron: str) -> List[int]:
        """Preprocesamiento + busqueda."""
        return self.buscar_contando_colisiones(texto, patron).posiciones

    def buscar_contando_colisiones(self, texto: str, patron: str,
                                   pre: Preprocesamiento = None) -> ResultadoRabinKarp:
        """Etapa 2 (si se pasa pre) o las dos etapas (si no se pasa)."""
        _comprobar_no_nulos(texto, patron)
        n, m = len(texto), len(patron)
        if m == 0 or m > n:
            return ResultadoRabinKarp([], 0)
        if pre is None:
            pre = self.preprocesar(texto, patron)

        posiciones: List[int] = []
        colisiones = 0
        h = pre.peso_primero
        hash_patron = pre.hash_patron
        hash_ventana = pre.hash_primera_ventana

        for i in range(n - m + 1):
            if hash_ventana == hash_patron:
                # Mismo hash: puede ser una ocurrencia o una colision. Se verifica.
                if self._coincide_en(texto, patron, i):
                    posiciones.append(i)
                else:
                    colisiones += 1
            # Hash rodante: quitar el primer caracter de la ventana y agregar el siguiente
            if i < n - m:
                aporte_del_primero = (ord(texto[i]) * h) % self.modulo
                hash_ventana = ((hash_ventana - aporte_del_primero + self.modulo) * self.BASE
                                + ord(texto[i + m])) % self.modulo
        return ResultadoRabinKarp(posiciones, colisiones)

    @staticmethod
    def _coincide_en(texto: str, patron: str, inicio: int) -> bool:
        """Compara el patron con el texto desde la posicion dada, caracter por caracter."""
        for j in range(len(patron)):
            if texto[inicio + j] != patron[j]:
                return False
        return True


# Los tres algoritmos, en el mismo orden que en Java
ALGORITMOS = [KnuthMorrisPratt(), BoyerMoore(), RabinKarp()]