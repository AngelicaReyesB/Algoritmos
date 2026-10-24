"""Mismas 7 pruebas que PruebasAlgoritmoOrdenamiento.java, aplicadas a los tres algoritmos."""

import random
import unittest

from src.algoritmos import ALGORITMOS


def orden_esperado(terminos):
    # Solo para VERIFICAR: sorted es estable, los empates conservan el orden de entrada
    return sorted(terminos, key=lambda par: -par[1])


class PruebasAlgoritmos(unittest.TestCase):

    def test_lista_vacia_devuelve_lista_vacia(self):
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                self.assertEqual(algoritmo([]), [])

    def test_un_solo_termino_queda_igual(self):
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                self.assertEqual(algoritmo([("modelo", 5)]), [("modelo", 5)])

    def test_ordena_de_mayor_a_menor_frecuencia(self):
        entrada = [("texto", 2), ("modelo", 9), ("imagen", 5), ("datos", 1)]
        esperado = [("modelo", 9), ("imagen", 5), ("texto", 2), ("datos", 1)]
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                self.assertEqual(algoritmo(entrada), esperado)

    def test_empates_conservan_el_orden_de_entrada(self):
        entrada = [("a", 1), ("b", 3), ("c", 7), ("d", 3), ("e", 3)]
        esperado = [("c", 7), ("b", 3), ("d", 3), ("e", 3), ("a", 1)]
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                self.assertEqual(algoritmo(entrada), esperado)

    def test_todas_las_frecuencias_iguales_no_cambia_el_orden(self):
        entrada = [(f"t{i}", 4) for i in range(10)]
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                self.assertEqual(algoritmo(entrada), entrada)

    def test_no_modifica_la_lista_de_entrada(self):
        entrada = [("a", 2), ("b", 9), ("c", 2)]
        copia = list(entrada)
        for nombre, algoritmo in ALGORITMOS.items():
            with self.subTest(nombre):
                algoritmo(entrada)
                self.assertEqual(entrada, copia)

    def test_coincide_con_la_referencia_en_casos_aleatorios(self):
        aleatorio = random.Random(42)  # semilla fija: siempre los mismos casos
        for caso in range(200):
            n = aleatorio.randint(1, 300)
            maxima = aleatorio.choice([3, 10, 100, 5000])
            entrada = [(f"t{i}", aleatorio.randint(1, maxima)) for i in range(n)]
            esperado = orden_esperado(entrada)
            for nombre, algoritmo in ALGORITMOS.items():
                self.assertEqual(algoritmo(entrada), esperado, f"{nombre}, caso {caso}")


if __name__ == "__main__":
    unittest.main()