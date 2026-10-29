"""Mismas pruebas que ExtractorAbstractTest.java."""

import unittest

from src.extraccion import extraer_abstract, numero_del_archivo


class PruebasExtraccion(unittest.TestCase):

    def test_sin_la_palabra_abstract_devuelve_none(self):
        self.assertIsNone(extraer_abstract("Titulo\nAutores\n1. Introduction\nEl texto no tiene esa seccion"))

    def test_springer_termina_en_palabras_clave(self):
        texto = ("REVIEW ARTICLE\nTitulo\nAbstract \nGenerative AI provides opportunities.\n"
                 "Keywords:  Generative artificial intelligence, Scoping review")
        self.assertEqual(extraer_abstract(texto), "Generative AI provides opportunities")

    def test_mdpi_con_dos_puntos_despues_de_abstract(self):
        texto = "Review\nTitulo\nAbstract: The use of GenAI is debated.\nKeywords: artificial intelligence"
        self.assertEqual(extraer_abstract(texto), "The use of GenAI is debated")

    def test_la_palabra_keywords_dentro_del_abstract_no_lo_corta(self):
        texto = "Abstract: Trends in citations, keywords, and co-authorship.\nKeywords: x"
        self.assertEqual(extraer_abstract(texto), "Trends in citations, keywords, and co-authorship")

    def test_elsevier_con_abstract_espaciado(self):
        texto = ("A R T I C L E  I N F O   \nKeywords: \nAI literacy \nA B S T R A C T   \n"
                 "Since AI is finding its way into everyday life. \nMore text.   \n1. Introduction \nx")
        self.assertEqual(extraer_abstract(texto), "Since AI is finding its way into everyday life. More text")

    def test_elsevier_termina_en_tres_espacios_sin_introduccion(self):
        texto = ("A B S T R A C T   \nThis article reports a review. \nOf 40 studies.   \n"
                 "Since Alan Turing first articulated")
        self.assertEqual(extraer_abstract(texto), "This article reports a review. Of 40 studies")

    def test_elsevier_salta_las_palabras_clave_despues_del_titulo(self):
        texto = ("A R T I C L E I N F O A B S T R A C T\nKeywords:\nHuman-centered AI\nAI in education\n"
                 "The rapid expansion of Learning Analytics offers new systems for teachers and students.\n"
                 "1. Introduction")
        self.assertEqual(extraer_abstract(texto),
                         "The rapid expansion of Learning Analytics offers new systems for teachers and students")

    def test_introduccion_pegada_al_final_del_abstract(self):
        texto = "A B S T R A C T\nWe highlight the new journal Computers & Education.1. Introduction\nx"
        self.assertEqual(extraer_abstract(texto), "We highlight the new journal Computers & Education")

    def test_wiley_termina_en_palabras_clave_espaciadas(self):
        texto = "Abstract\nRecent literature underscores the need.\nK E Y W O R D S\nAI competence"
        self.assertEqual(extraer_abstract(texto), "Recent literature underscores the need")

    def test_une_palabras_cortadas_con_guion(self):
        texto = "Abstract\nAn eﬃcient gener-\native model.\nKeywords: x"
        self.assertEqual(extraer_abstract(texto), "An efficient generative model")

    def test_elsevier_une_palabras_cortadas_sin_guion(self):
        texto = ("A B S T R A C T   \nFifty-nine studies met the in\nclusion criteria. \nWe coded them. \n"
                 "Then we wrote.   \n1. Introduction")
        self.assertEqual(extraer_abstract(texto),
                         "Fifty-nine studies met the inclusion criteria. We coded them. Then we wrote")

    def test_numero_del_archivo(self):
        self.assertEqual(numero_del_archivo("12. A meta systematic review.pdf"), 12)
        self.assertEqual(numero_del_archivo("1. A.pdf"), 1)
        self.assertEqual(numero_del_archivo("sin_numero.pdf"), -1)


if __name__ == "__main__":
    unittest.main()
