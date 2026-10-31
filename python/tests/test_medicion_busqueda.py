"""Pruebas de la medicion de busqueda (texto, escenarios y CSV), con pocas repeticiones."""

import csv
import tempfile
import unittest
from pathlib import Path

from src.medicion_busqueda import (AUSENTE_CORTO, AUSENTE_LARGO, COLUMNAS, CORTO, LARGO,
                                   crear_escenarios, escribir_csv, medir_busqueda)

# Texto de prueba: contiene "model" y es mas largo que 2 * 40 caracteres
TEXTO = ("Generative AI models change assessment in higher education. "
         "Teachers need training to use learning analytics tools with each model.")


class PruebasMedicionBusqueda(unittest.TestCase):

    def test_crea_los_cuatro_escenarios_en_orden(self):
        escenarios = crear_escenarios(TEXTO)
        self.assertEqual(list(escenarios), [CORTO, LARGO, AUSENTE_CORTO, AUSENTE_LARGO])
        self.assertEqual(escenarios[CORTO], "model")
        self.assertEqual(len(escenarios[LARGO]), 40)

    def test_el_patron_largo_empieza_en_una_palabra_despues_de_la_mitad(self):
        largo = crear_escenarios(TEXTO)[LARGO]
        inicio = TEXTO.index(largo)  # solo para VERIFICAR
        self.assertGreater(inicio, len(TEXTO) // 2)
        self.assertEqual(TEXTO[inicio - 1], " ")

    def test_el_ausente_largo_es_el_largo_con_el_ultimo_caracter_cambiado(self):
        escenarios = crear_escenarios(TEXTO)
        self.assertEqual(escenarios[AUSENTE_LARGO], escenarios[LARGO][:-1] + "#")

    def test_texto_demasiado_corto_falla(self):
        self.assertRaises(ValueError, crear_escenarios, "model")

    def test_si_el_patron_corto_no_aparece_falla(self):
        self.assertRaises(ValueError, crear_escenarios, TEXTO.replace("model", "xxxxx"))

    def test_mide_dos_etapas_por_algoritmo_y_escenario(self):
        filas = medir_busqueda(TEXTO, calentamiento=1, repeticiones=3)
        self.assertEqual(len(filas), 4 * 3 * 2)  # escenarios x algoritmos x etapas
        for fila in filas:
            self.assertEqual(fila.medicion.repeticiones, 3)
            self.assertGreaterEqual(fila.medicion.media_ms, 0)

    def test_ocurrencias_y_colisiones_solo_en_la_busqueda(self):
        for fila in medir_busqueda(TEXTO, calentamiento=1, repeticiones=3):
            if fila.etapa == "preprocesamiento":
                self.assertIsNone(fila.ocurrencias)
                self.assertIsNone(fila.colisiones)
            else:
                self.assertIsNotNone(fila.ocurrencias)
                self.assertEqual(fila.colisiones is not None, fila.algoritmo == "Rabin-Karp")

    def test_el_csv_tiene_las_mismas_columnas_que_java(self):
        filas = medir_busqueda(TEXTO, calentamiento=1, repeticiones=3)
        with tempfile.TemporaryDirectory() as carpeta:
            destino = Path(carpeta) / "busqueda.csv"
            escribir_csv(filas, destino)
            with open(destino, encoding="utf-8") as entrada:
                lineas = list(csv.reader(entrada))

        self.assertEqual(lineas[0], COLUMNAS)
        self.assertEqual(len(lineas), 1 + len(filas))
        primera = lineas[1]
        self.assertEqual(primera[:5], ["KMP", "corto", "5", str(len(TEXTO)), "preprocesamiento"])
        self.assertEqual(len(primera[6].split(".")[1]), 6)  # 6 decimales, como Java
        self.assertEqual(primera[9:], ["", ""])             # sin ocurrencias ni colisiones


if __name__ == "__main__":
    unittest.main()