package co.edu.uniquindio.ranking.embeddings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas con el modelo real. No se puede calcular el resultado a mano,
 * asi que se prueba el comportamiento esperado.
 *
 * El modelo se carga una sola vez (static) porque tarda unos segundos.
 */
class SimilitudMiniLmTest {

    private static final SimilitudMiniLm MINI_LM = new SimilitudMiniLm();

    private static final String EVALUACION_IA =
            "Artificial intelligence is transforming assessment in higher education.";
    private static final String EVALUACION_IA_OTRAS_PALABRAS =
            "Generative AI is changing how universities evaluate their students.";
    private static final String RECETA =
            "The recipe needs two cups of flour, three eggs and a little sugar.";

    @Test
    void elVectorTiene384Numeros() {
        assertEquals(384, MINI_LM.vectorDelTexto(EVALUACION_IA).length);
    }

    @Test
    void textosIgualesTienenSimilitudUno() {
        assertEquals(1.0, MINI_LM.similitud(EVALUACION_IA, EVALUACION_IA), 1e-6);
    }

    @Test
    void mismoTemaConOtrasPalabrasEsMasParecidoQueOtroTema() {
        // Las dos primeras frases casi no comparten palabras, pero hablan de lo mismo
        double mismoTema = MINI_LM.similitud(EVALUACION_IA, EVALUACION_IA_OTRAS_PALABRAS);
        double otroTema = MINI_LM.similitud(EVALUACION_IA, RECETA);
        assertTrue(mismoTema > otroTema,
                "mismo tema = " + mismoTema + ", otro tema = " + otroTema);
    }

    @Test
    void laSimilitudQuedaEntreCeroYUno() {
        double similitud = MINI_LM.similitud(EVALUACION_IA, RECETA);
        assertTrue(similitud >= 0 && similitud <= 1, "similitud = " + similitud);
    }

    @Test
    void esSimetrica() {
        assertEquals(MINI_LM.similitud(EVALUACION_IA, RECETA),
                MINI_LM.similitud(RECETA, EVALUACION_IA), 1e-9);
    }

    @Test
    void cadaTextoSeCalculaUnaSolaVez() {
        // La segunda vez devuelve el MISMO arreglo guardado, no uno nuevo
        double[] primeraVez = MINI_LM.vectorDelTexto(RECETA);
        double[] segundaVez = MINI_LM.vectorDelTexto(RECETA);
        assertSame(primeraVez, segundaVez);
    }
}