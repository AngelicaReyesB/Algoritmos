"""Mismas pruebas que MedicionManualTest.java."""

import unittest

from src.medicion import medir


class PruebasMedicion(unittest.TestCase):

    def test_ejecuta_la_tarea_calentamiento_mas_repeticiones_veces(self):
        ejecuciones = []
        medir("prueba", 3, 10, lambda: ejecuciones.append(1))
        self.assertEqual(len(ejecuciones), 13)

    def test_las_estadisticas_son_coherentes(self):
        resultado = medir("suma", 5, 50, lambda: sum(range(10_000)))
        self.assertEqual(resultado.etapa, "suma")
        self.assertEqual(resultado.repeticiones, 50)
        self.assertGreaterEqual(resultado.minimo_ms, 0)
        self.assertLessEqual(resultado.minimo_ms, resultado.media_ms)
        self.assertGreaterEqual(resultado.desviacion_ms, 0)


if __name__ == "__main__":
    unittest.main()