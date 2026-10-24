"""Mismas pruebas que TokenizadorPropioTest.java y ContadorFrecuenciasTest.java."""

import unittest

from src.texto import contar_frecuencias, tokenizar


class PruebasTokenizador(unittest.TestCase):

    def test_pasa_a_minusculas_y_elimina_palabras_vacias(self):
        self.assertEqual(tokenizar("The Generative Models of Images"), ["generative", "models", "images"])

    def test_conserva_terminos_de_dos_letras(self):
        self.assertEqual(tokenizar("AI and LLM agents"), ["ai", "llm", "agents"])

    def test_descarta_palabras_de_una_letra(self):
        self.assertEqual(tokenizar("x vector y space z"), ["vector", "space"])

    def test_elimina_palabras_vacias_del_dominio(self):
        self.assertEqual(tokenizar("In this paper we propose diffusion results"), ["diffusion"])

    def test_numeros_y_guiones_separan_palabras(self):
        self.assertEqual(tokenizar("GPT-4 is state-of-the-art and fine-tuned"),
                         ["gpt", "state", "art", "fine", "tuned"])

    def test_elimina_urls(self):
        self.assertEqual(tokenizar("Code available at https://github.com/openai/guided-diffusion"),
                         ["code", "available"])
        self.assertEqual(tokenizar("See imagen.research.google for an overview"), ["see", "overview"])

    def test_normaliza_ligaduras(self):
        self.assertEqual(tokenizar("eﬃcient"), ["efficient"])
        self.assertEqual(tokenizar("reﬂected"), ["reflected"])

    def test_texto_vacio_devuelve_lista_vacia(self):
        self.assertEqual(tokenizar(""), [])
        self.assertEqual(tokenizar("the of and 123 !!"), [])


class PruebasConteo(unittest.TestCase):

    def test_cuenta_terminos_en_varios_textos(self):
        self.assertEqual(contar_frecuencias(["language models generate text", "large language models"]),
                         [("language", 2), ("models", 2), ("generate", 1), ("text", 1), ("large", 1)])

    def test_conserva_el_orden_de_primera_aparicion(self):
        self.assertEqual(contar_frecuencias(["zeta alfa zeta beta alfa"]),
                         [("zeta", 2), ("alfa", 2), ("beta", 1)])

    def test_model_y_models_son_terminos_distintos(self):
        self.assertEqual(contar_frecuencias(["model models model"]), [("model", 2), ("models", 1)])

    def test_sin_textos_devuelve_lista_vacia(self):
        self.assertEqual(contar_frecuencias([]), [])


if __name__ == "__main__":
    unittest.main()