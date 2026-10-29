package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de Levenshtein con ejemplos que se pueden verificar a mano.
 */
class LevenshteinTest {

    /**
     * Tokenizador de prueba: solo separa por espacios.
     * Asi las pruebas no dependen de las palabras vacias del tokenizador propio.
     */
    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final Levenshtein levenshtein = new Levenshtein(porEspacios);

    /** Convierte una palabra en una lista de letras: "gato" -> [g, a, t, o]. */
    private static List<String> letras(String palabra) {
        return Arrays.asList(palabra.split(""));
    }

    @Test
    void ejemploClasicoKittenSitting() {
        // kitten -> sitten (sustituir k por s) -> sittin (sustituir e por i) -> sitting (insertar g)
        assertEquals(3, Levenshtein.distancia(letras("kitten"), letras("sitting")));
    }

    @Test
    void matrizCompletaDeGatoYPato() {
        int[][] esperada = {
                //      ""  p  a  t  o
                /* "" */ {0, 1, 2, 3, 4},
                /* g  */ {1, 1, 2, 3, 4},
                /* a  */ {2, 2, 1, 2, 3},
                /* t  */ {3, 3, 2, 1, 2},
                /* o  */ {4, 4, 3, 2, 1},
        };
        int[][] matriz = Levenshtein.construirMatriz(letras("gato"), letras("pato"));
        for (int i = 0; i < esperada.length; i++) {
            assertArrayEquals(esperada[i], matriz[i], "fila " + i);
        }
    }

    @Test
    void desdeUnaListaVaciaSonSoloInserciones() {
        assertEquals(3, Levenshtein.distancia(List.of(), List.of("a", "b", "c")));
        assertEquals(3, Levenshtein.distancia(List.of("a", "b", "c"), List.of()));
    }

    @Test
    void textosIgualesTienenSimilitudUno() {
        assertEquals(1.0, levenshtein.similitud("deep learning models", "deep learning models"), 1e-9);
    }

    @Test
    void textosSinPalabrasEnComunTienenSimilitudCero() {
        assertEquals(0.0, levenshtein.similitud("deep learning models", "students teachers schools"), 1e-9);
    }

    @Test
    void unaPalabraDistintaDeTres() {
        // Una sustitucion (models -> methods): similitud = 1 - 1/3
        assertEquals(1.0 - 1.0 / 3, levenshtein.similitud("deep learning models", "deep learning methods"), 1e-9);
    }

    @Test
    void usaElLargoDeLaListaMasLarga() {
        // "ai" -> "ai in education": 2 inserciones, lista mas larga = 3 -> 1 - 2/3
        assertEquals(1.0 - 2.0 / 3, levenshtein.similitud("ai", "ai in education"), 1e-9);
    }

    @Test
    void esSimetrica() {
        String a = "generative ai transforms assessment";
        String b = "ai assessment in higher education";
        assertEquals(levenshtein.similitud(a, b), levenshtein.similitud(b, a), 1e-9);
    }

    @Test
    void dosTextosVaciosSeConsideranIguales() {
        assertEquals(1.0, levenshtein.similitud("", ""), 1e-9);
    }
}