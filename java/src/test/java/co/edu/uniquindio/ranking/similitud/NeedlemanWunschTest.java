package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de Needleman-Wunsch con ejemplos que se pueden verificar a mano.
 * Puntajes: coincidencia +1, desajuste -1, espacio -1.
 */
class NeedlemanWunschTest {

    /** Tokenizador de prueba: solo separa por espacios. */
    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final NeedlemanWunsch needlemanWunsch = new NeedlemanWunsch(porEspacios);

    /** Convierte una palabra en una lista de letras: "gato" -> [g, a, t, o]. */
    private static List<String> letras(String palabra) {
        return Arrays.asList(palabra.split(""));
    }

    @Test
    void matrizCompletaDeGatoYPato() {
        int[][] esperada = {
                //       ""   p   a   t   o
                /* "" */ { 0, -1, -2, -3, -4},
                /* g  */ {-1, -1, -2, -3, -4},
                /* a  */ {-2, -2,  0, -1, -2},
                /* t  */ {-3, -3, -1,  1,  0},
                /* o  */ {-4, -4, -2,  0,  2},
        };
        int[][] matriz = NeedlemanWunsch.construirMatriz(letras("gato"), letras("pato"));
        for (int i = 0; i < esperada.length; i++) {
            assertArrayEquals(esperada[i], matriz[i], "fila " + i);
        }
    }

    @Test
    void puntajeDeGatoYPato() {
        // 3 coincidencias (a, t, o) y 1 desajuste (g/p): 3 - 1 = 2
        assertEquals(2, NeedlemanWunsch.puntaje(letras("gato"), letras("pato")));
    }

    @Test
    void alineamientoConUnEspacio() {
        NeedlemanWunsch.Alineamiento alineamiento =
                NeedlemanWunsch.alinear(List.of("ai", "in", "education"), List.of("ai", "education"));
        assertEquals(List.of("ai", "in", "education"), alineamiento.filaA());
        assertEquals(List.of("ai", "-", "education"), alineamiento.filaB());
        assertEquals(2, alineamiento.coincidencias());
    }

    @Test
    void desdeUnaListaVaciaSonSoloEspacios() {
        assertEquals(-3, NeedlemanWunsch.puntaje(List.of(), List.of("a", "b", "c")));
        NeedlemanWunsch.Alineamiento alineamiento = NeedlemanWunsch.alinear(List.of(), List.of("a", "b"));
        assertEquals(List.of("-", "-"), alineamiento.filaA());
        assertEquals(List.of("a", "b"), alineamiento.filaB());
    }

    @Test
    void textosIgualesTienenSimilitudUno() {
        assertEquals(1.0, needlemanWunsch.similitud("deep learning models", "deep learning models"), 1e-9);
    }

    @Test
    void textosSinPalabrasEnComunTienenSimilitudCero() {
        assertEquals(0.0, needlemanWunsch.similitud("deep learning models", "students teachers schools"), 1e-9);
    }

    @Test
    void similitudNormalizaElPuntaje() {
        // puntaje = +1 (ai) -1 (in frente a un espacio) +1 (education) = 1
        // similitud = (puntaje + largo mayor) / (largo corto + largo mayor) = (1 + 3) / (2 + 3)
        assertEquals(0.8, needlemanWunsch.similitud("ai in education", "ai education"), 1e-9);
    }

    @Test
    void esSimetrica() {
        String a = "generative ai transforms assessment";
        String b = "ai assessment in higher education";
        assertEquals(needlemanWunsch.similitud(a, b), needlemanWunsch.similitud(b, a), 1e-9);
    }

    @Test
    void dosTextosVaciosSeConsideranIguales() {
        assertEquals(1.0, needlemanWunsch.similitud("", ""), 1e-9);
    }
}