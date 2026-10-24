"""Mismas pruebas que ExtractorAbstractTest.java."""

import unittest

from src.extraccion import extraer_abstract, extraer_titulo


class PruebasExtraccion(unittest.TestCase):

    def test_extrae_el_texto_entre_abstract_e_introduccion(self):
        texto = "Titulo del articulo\nAutores\nAbstract\nWe propose a new model.\n1 Introduction\nTexto"
        self.assertEqual(extraer_abstract(texto), "We propose a new model")

    def test_termina_en_palabras_clave(self):
        texto = "Abstract: Diffusion models work well.\nKeywords: diffusion, images"
        self.assertEqual(extraer_abstract(texto), "Diffusion models work well")

    def test_sin_la_palabra_abstract_devuelve_none(self):
        self.assertIsNone(extraer_abstract("Titulo\nAutores\n1 Introduction\nEl texto no tiene esa seccion"))

    def test_elimina_el_sello_de_arxiv(self):
        texto = "Abstract\nLanguage models scale.\narXiv:2001.08361v1 [cs.LG] 23 Jan 2020\n1 Introduction"
        self.assertEqual(extraer_abstract(texto), "Language models scale")

    def test_termina_en_nota_al_pie_de_contribucion(self):
        texto = "Abstract\nThe dominant models are recurrent.\n∗Equal contribution. Listing order is random."
        self.assertEqual(extraer_abstract(texto), "The dominant models are recurrent")

    def test_termina_en_ejemplos_de_figura(self):
        texto = "Abstract\nChain of thought improves reasoning.\nA: The cafeteria had 23 apples.\nQ: Roger"
        self.assertEqual(extraer_abstract(texto), "Chain of thought improves reasoning")

    def test_elimina_el_aviso_de_copyright_de_acm(self):
        texto = ("ABSTRACT\nartists paint, while\nPermission to make digital or hard copies of part\n"
                 "of this work. https://doi.org/10.1145/3586183.3606763\nauthors write.\nCCS CONCEPTS\nx")
        self.assertEqual(extraer_abstract(texto), "artists paint, while authors write")

    def test_normaliza_ligaduras_y_une_palabras_cortadas(self):
        texto = "Abstract\nAn eﬃcient gener-\native model.\nKeywords: x"
        self.assertEqual(extraer_abstract(texto), "An efficient generative model")

    def test_titulo_es_la_primera_linea_no_vacia(self):
        self.assertEqual(extraer_titulo("\n\n  Generative Adversarial Nets \nIan Goodfellow"),
                         "Generative Adversarial Nets")


if __name__ == "__main__":
    unittest.main()