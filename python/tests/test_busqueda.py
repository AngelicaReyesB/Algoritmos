"""Mismas pruebas que la version Java (PruebasAlgoritmoBusqueda, KnuthMorrisPrattTest,
BoyerMooreTest y RabinKarpTest)."""

import random
import unittest

from src.busqueda import ALGORITMOS, BoyerMoore, KnuthMorrisPratt, RabinKarp


def fuerza_bruta(texto, patron):
    """Referencia para VERIFICAR: probar el patron en cada posicion del texto."""
    posiciones = []
    if len(patron) == 0:
        return posiciones
    for i in range(len(texto) - len(patron) + 1):
        j = 0
        while j < len(patron) and texto[i + j] == patron[j]:
            j += 1
        if j == len(patron):
            posiciones.append(i)
    return posiciones


def cadena_aleatoria(azar, largo):
    """Alfabeto pequeno (a, b, c) para que haya muchas coincidencias parciales."""
    return "".join(azar.choice("abc") for _ in range(largo))


class PruebasComunes(unittest.TestCase):
    """Las mismas pruebas para los tres algoritmos."""

    def comprobar(self, texto, patron, esperado):
        for algoritmo in ALGORITMOS:
            with self.subTest(algoritmo.nombre):
                self.assertEqual(algoritmo.buscar(texto, patron), esperado)

    def test_patron_al_inicio(self):
        self.comprobar("generative models", "gen", [0])

    def test_patron_al_final(self):
        self.comprobar("generative models", "els", [14])

    def test_varias_ocurrencias(self):
        self.comprobar("ai helps; ai in schools ai", "ai", [0, 10, 24])

    def test_ocurrencias_solapadas(self):
        self.comprobar("aaaa", "aa", [0, 1, 2])

    def test_sin_ocurrencias(self):
        self.comprobar("generative models", "xyz", [])

    def test_patron_mas_largo_que_el_texto(self):
        self.comprobar("ai", "artificial", [])

    def test_texto_vacio(self):
        self.comprobar("", "ai", [])

    def test_patron_vacio(self):
        self.comprobar("generative models", "", [])

    def test_texto_igual_al_patron(self):
        self.comprobar("education", "education", [0])

    def test_distingue_mayusculas_y_minusculas(self):
        self.comprobar("AI and ai", "AI", [0])

    def test_ejemplo_clasico_de_libro(self):
        self.comprobar("abababacababacab", "ababaca", [2, 8])

    def test_rechaza_entradas_nulas(self):
        for algoritmo in ALGORITMOS:
            with self.subTest(algoritmo.nombre):
                self.assertRaises(ValueError, algoritmo.buscar, None, "a")
                self.assertRaises(ValueError, algoritmo.buscar, "a", None)

    def test_coincide_con_fuerza_bruta_en_textos_aleatorios(self):
        azar = random.Random(42)
        for algoritmo in ALGORITMOS:
            for _ in range(500):
                texto = cadena_aleatoria(azar, azar.randrange(60))
                patron = cadena_aleatoria(azar, 1 + azar.randrange(5))
                self.assertEqual(algoritmo.buscar(texto, patron), fuerza_bruta(texto, patron),
                                 f"{algoritmo.nombre}: texto={texto!r} patron={patron!r}")


class PruebasKmp(unittest.TestCase):

    kmp = KnuthMorrisPratt()

    def test_tabla_de_ejemplo_del_informe(self):
        self.assertEqual(self.kmp.construir_tabla_prefijos("ababaca"), [0, 0, 1, 2, 3, 0, 1])

    def test_tabla_con_todos_los_caracteres_iguales(self):
        self.assertEqual(self.kmp.construir_tabla_prefijos("aaaa"), [0, 1, 2, 3])

    def test_tabla_sin_repeticiones(self):
        self.assertEqual(self.kmp.construir_tabla_prefijos("abcd"), [0, 0, 0, 0])

    def test_tabla_con_retroceso_en_cadena(self):
        self.assertEqual(self.kmp.construir_tabla_prefijos("aabaaab"), [0, 1, 0, 1, 2, 2, 3])

    def test_buscar_con_la_tabla_ya_construida_da_lo_mismo(self):
        tabla = self.kmp.construir_tabla_prefijos("ababca")
        self.assertEqual(self.kmp.buscar_con_tabla("abababca", "ababca", tabla), [2])


class PruebasBoyerMoore(unittest.TestCase):

    bm = BoyerMoore()

    def test_caracter_malo_guarda_la_ultima_posicion(self):
        self.assertEqual(self.bm.construir_tabla_caracter_malo("GCAGAGAG"), {"G": 7, "C": 1, "A": 6})

    def test_caracter_malo_de_una_palabra_real(self):
        # 'o' aparece en 3 y en 8: se queda la ultima
        self.assertEqual(self.bm.construir_tabla_caracter_malo("algoritmos"),
                         {"a": 0, "l": 1, "g": 2, "o": 8, "r": 4, "i": 5, "t": 6, "m": 7, "s": 9})

    def test_sufijo_bueno_de_ejemplo_con_repeticiones(self):
        self.assertEqual(self.bm.construir_tabla_sufijo_bueno("GCAGAGAG"), [1, 7, 4, 7, 2, 7, 7, 7, 7])

    def test_sufijo_bueno_con_todos_los_caracteres_iguales(self):
        self.assertEqual(self.bm.construir_tabla_sufijo_bueno("aaaa"), [4, 3, 2, 1, 1])

    def test_sufijo_bueno_cuando_solo_ayuda_el_prefijo(self):
        self.assertEqual(self.bm.construir_tabla_sufijo_bueno("ababca"), [1, 3, 5, 5, 5, 5, 5])

    def test_sufijo_bueno_sin_sufijos_repetidos(self):
        self.assertEqual(self.bm.construir_tabla_sufijo_bueno("algoritmos"), [1] + [10] * 10)

    def test_sufijo_bueno_de_un_solo_caracter(self):
        self.assertEqual(self.bm.construir_tabla_sufijo_bueno("a"), [1, 1])

    def test_buscar_con_las_tablas_ya_construidas_da_lo_mismo(self):
        patron = "ababca"
        resultado = self.bm.buscar_con_tablas("abababca", patron,
                                              self.bm.construir_tabla_caracter_malo(patron),
                                              self.bm.construir_tabla_sufijo_bueno(patron))
        self.assertEqual(resultado, [2])


class PruebasRabinKarp(unittest.TestCase):

    rk = RabinKarp()

    def test_usa_base_256_y_modulo_primo_101_por_defecto(self):
        self.assertEqual(RabinKarp.BASE, 256)
        self.assertEqual(self.rk.modulo, 101)

    def test_rechaza_un_modulo_invalido(self):
        self.assertRaises(ValueError, RabinKarp, 0)
        self.assertRaises(ValueError, RabinKarp, -7)

    def test_hash_de_un_caracter_es_su_codigo_modulo_el_primo(self):
        self.assertEqual(self.rk.calcular_hash("a", 0, 1), 97)  # 97 mod 101
        self.assertEqual(self.rk.calcular_hash("e", 0, 1), 0)   # 101 mod 101

    def test_hash_de_dos_caracteres(self):
        # (97 * 256 + 98) mod 101 = 84
        self.assertEqual(self.rk.calcular_hash("ab", 0, 2), 84)

    def test_hash_de_una_ventana_dentro_del_texto(self):
        self.assertEqual(self.rk.calcular_hash("xxabxx", 2, 2), 84)

    def test_con_modulo_pequeno_dos_caracteres_distintos_tienen_el_mismo_hash(self):
        pequeno = RabinKarp(7)
        # 'a' = 97 y 'h' = 104: ambos dan 6 al dividir entre 7
        self.assertEqual(pequeno.calcular_hash("a", 0, 1), 6)
        self.assertEqual(pequeno.calcular_hash("h", 0, 1), 6)

    def test_una_colision_no_se_reporta_como_ocurrencia(self):
        resultado = RabinKarp(7).buscar_contando_colisiones("h", "a")
        self.assertEqual(resultado.posiciones, [])
        self.assertEqual(resultado.colisiones, 1)

    def test_una_colision_y_una_ocurrencia_real_conviven(self):
        resultado = RabinKarp(7).buscar_contando_colisiones("ha", "a")
        self.assertEqual(resultado.posiciones, [1])
        self.assertEqual(resultado.colisiones, 1)

    def test_cada_ventana_que_colisiona_se_cuenta(self):
        resultado = RabinKarp(7).buscar_contando_colisiones("hhhh", "a")
        self.assertEqual(resultado.posiciones, [])
        self.assertEqual(resultado.colisiones, 4)

    def test_las_ocurrencias_reales_no_cuentan_como_colisiones(self):
        resultado = self.rk.buscar_contando_colisiones("aaaa", "aa")
        self.assertEqual(resultado.posiciones, [0, 1, 2])
        self.assertEqual(resultado.colisiones, 0)

    def test_sin_busqueda_posible_no_hay_colisiones(self):
        self.assertEqual(self.rk.buscar_contando_colisiones("ai", "artificial").colisiones, 0)
        self.assertEqual(self.rk.buscar_contando_colisiones("texto", "").colisiones, 0)

    def test_con_cualquier_modulo_el_resultado_es_el_de_fuerza_bruta(self):
        azar = random.Random(7)
        for modulo in [1, 2, 3, 7, 101, 10007]:
            rk = RabinKarp(modulo)
            for _ in range(300):
                texto = cadena_aleatoria(azar, azar.randrange(60))
                patron = cadena_aleatoria(azar, 1 + azar.randrange(5))
                self.assertEqual(rk.buscar(texto, patron), fuerza_bruta(texto, patron),
                                 f"modulo={modulo} texto={texto!r} patron={patron!r}")

    def test_preprocesar_calcula_el_peso_del_primero_y_los_dos_hashes(self):
        pre = self.rk.preprocesar("xxab", "ab")
        self.assertEqual(pre.peso_primero, 54)          # 256^1 mod 101
        self.assertEqual(pre.hash_patron, 84)           # hash de "ab"
        self.assertEqual(pre.hash_primera_ventana, 35)  # hash de "xx"

    def test_buscar_con_el_preprocesamiento_ya_calculado_da_lo_mismo(self):
        pre = self.rk.preprocesar("xxabxxab", "ab")
        self.assertEqual(self.rk.buscar_contando_colisiones("xxabxxab", "ab", pre).posiciones, [2, 6])


if __name__ == "__main__":
    unittest.main()