package co.edu.uniquindio.ranking.agrupamiento;

import co.edu.uniquindio.ranking.similitud.Jaccard;
import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas con Jaccard y 3 textos pequenos, para poder calcular a mano:
 *
 *   t0 = "ai education"     t1 = "ai students"     t2 = "robots factories"
 *
 *   Jaccard(t0, t1) = |{ai}| / |{ai, education, students}| = 1/3  -> distancia 2/3
 *   Jaccard(t0, t2) = 0                                          -> distancia 1
 *   Jaccard(t1, t2) = 0                                          -> distancia 1
 */
class MatrizDistanciasTest {

    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final Jaccard jaccard = new Jaccard(porEspacios);

    private final List<String> textos = List.of("ai education", "ai students", "robots factories");

    @Test
    void matrizCalculadaAMano() {
        double[][] distancias = MatrizDistancias.calcular(jaccard, textos);
        assertArrayEquals(new double[]{0, 2.0 / 3, 1}, distancias[0], 1e-9);
        assertArrayEquals(new double[]{2.0 / 3, 0, 1}, distancias[1], 1e-9);
        assertArrayEquals(new double[]{1, 1, 0}, distancias[2], 1e-9);
    }

    @Test
    void laDiagonalEsCero() {
        double[][] distancias = MatrizDistancias.calcular(jaccard, textos);
        for (int i = 0; i < distancias.length; i++) {
            assertEquals(0.0, distancias[i][i], 1e-9);
        }
    }

    @Test
    void esSimetrica() {
        double[][] distancias = MatrizDistancias.calcular(jaccard, textos);
        for (int i = 0; i < distancias.length; i++) {
            for (int j = 0; j < distancias.length; j++) {
                assertEquals(distancias[i][j], distancias[j][i], 1e-9);
            }
        }
    }

    @Test
    void textosIgualesEstanADistanciaCero() {
        double[][] distancias = MatrizDistancias.calcular(jaccard, List.of("ai education", "ai education"));
        assertEquals(0.0, distancias[0][1], 1e-9);
    }

    @Test
    void conMenosDeDosTextosNoSePuedeAgrupar() {
        assertThrows(IllegalArgumentException.class,
                () -> MatrizDistancias.calcular(jaccard, List.of("ai education")));
    }
}