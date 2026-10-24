package co.edu.uniquindio.ranking.texto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizadorPropioTest {

    private final Tokenizador tokenizador = new TokenizadorPropio();

    @Test
    void pasaAMinusculasYEliminaPalabrasVacias() {
        assertEquals(List.of("generative", "models", "images"),
                tokenizador.tokenizar("The Generative Models of Images"));
    }

    @Test
    void conservaTerminosDeDosLetras() {
        assertEquals(List.of("ai", "llm", "agents"),
                tokenizador.tokenizar("AI and LLM agents"));
    }

    @Test
    void descartaPalabrasDeUnaLetra() {
        assertEquals(List.of("vector", "space"),
                tokenizador.tokenizar("x vector y space z"));
    }

    @Test
    void eliminaPalabrasVaciasDelDominio() {
        assertEquals(List.of("diffusion"),
                tokenizador.tokenizar("In this paper we propose diffusion results"));
    }

    @Test
    void numerosYGuionesSeparanPalabras() {
        assertEquals(List.of("gpt", "state", "art", "fine", "tuned"),
                tokenizador.tokenizar("GPT-4 is state-of-the-art and fine-tuned"));
    }

    @Test
    void eliminaUrls() {
        assertEquals(List.of("code", "available"),
                tokenizador.tokenizar("Code available at https://github.com/openai/guided-diffusion"));
        assertEquals(List.of("see", "overview"),
                tokenizador.tokenizar("See imagen.research.google for an overview"));
    }

    @Test
    void normalizaLigaduras() {
        // "ﬁ" es un solo caracter (ligadura) en el texto de entrada
        assertEquals(List.of("efficient"), tokenizador.tokenizar("eﬃcient"));
        assertEquals(List.of("reflected"), tokenizador.tokenizar("reﬂected"));
    }

    @Test
    void textoVacioDevuelveListaVacia() {
        assertTrue(tokenizador.tokenizar("").isEmpty());
        assertTrue(tokenizador.tokenizar("the of and 123 !!").isEmpty());
    }
}