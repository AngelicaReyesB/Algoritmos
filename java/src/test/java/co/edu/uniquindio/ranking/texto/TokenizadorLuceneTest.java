package co.edu.uniquindio.ranking.texto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El tokenizador con Lucene debe aplicar las mismas reglas que el propio.
 * Varias pruebas comparan directamente los dos.
 */
class TokenizadorLuceneTest {

    private final Tokenizador lucene = new TokenizadorLucene();
    private final Tokenizador propio = new TokenizadorPropio();

    @Test
    void pasaAMinusculasYEliminaPalabrasVacias() {
        assertEquals(List.of("generative", "models", "images"),
                lucene.tokenizar("The Generative Models of Images"));
    }

    @Test
    void conservaTerminosDeDosLetras() {
        assertEquals(List.of("ai", "llm", "agents"), lucene.tokenizar("AI and LLM agents"));
    }

    @Test
    void descartaNumeros() {
        assertEquals(List.of("trained", "tokens"), lucene.tokenizar("Trained on 1,000 tokens in 2020"));
    }

    @Test
    void eliminaUrlsYNormalizaLigaduras() {
        // "ﬃ" es un solo caracter (ligadura): debe quedar "efficient"
        assertEquals(List.of("efficient", "code"),
                lucene.tokenizar("Eﬃcient code at https://github.com/openai/guided-diffusion"));
    }

    @Test
    void daLoMismoQueElPropioEnUnTextoSencillo() {
        String texto = "Large language models generate text, images and code from natural language.";
        assertEquals(propio.tokenizar(texto), lucene.tokenizar(texto));
    }

    @Test
    void textoVacioDevuelveListaVacia() {
        assertTrue(lucene.tokenizar("").isEmpty());
    }
}