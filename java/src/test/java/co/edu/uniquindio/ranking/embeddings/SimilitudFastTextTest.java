package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas con un modelo de juguete de 2 dimensiones, para poder calcular a mano:
 *
 *   teacher  = (1, 0)
 *   educator = (1, 0)     <- mismo vector que teacher: significan lo mismo
 *   student  = (0, 1)
 *   robot    = (-1, 0)
 */
class SimilitudFastTextTest {

    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final ModeloFastText modelo = new ModeloFastText(Map.of(
            "teacher", new double[]{1, 0},
            "educator", new double[]{1, 0},
            "student", new double[]{0, 1},
            "robot", new double[]{-1, 0}));

    private final SimilitudFastText fastText = new SimilitudFastText(porEspacios, modelo);

    @Test
    void leeElArchivoVecSaltandoElEncabezado() throws Exception {
        Path archivo = Files.createTempFile("modelo", ".vec");
        Files.writeString(archivo, "2 3\nai 0.1 0.2 0.3\neducation -1 0 1 \n", StandardCharsets.UTF_8);

        ModeloFastText leido = new ModeloFastText(archivo);
        assertEquals(2, leido.getCantidadPalabras());
        assertEquals(3, leido.getDimension());
        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, leido.vector("ai"), 1e-9);
        assertArrayEquals(new double[]{-1, 0, 1}, leido.vector("education"), 1e-9);
        assertNull(leido.vector("chatgpt"));

        Files.delete(archivo);
    }

    @Test
    void elVectorDelTextoEsElPromedio() {
        // (teacher + student) / 2 = ((1, 0) + (0, 1)) / 2 = (0.5, 0.5)
        assertArrayEquals(new double[]{0.5, 0.5}, fastText.vectorDelTexto("teacher student"), 1e-9);
    }

    @Test
    void lasPalabrasQueNoEstanSeIgnoran() {
        // "chatgpt" no esta: el promedio es solo el de teacher
        assertArrayEquals(new double[]{1, 0}, fastText.vectorDelTexto("teacher chatgpt"), 1e-9);
        assertEquals(List.of("chatgpt"), fastText.palabrasNoEncontradas("teacher chatgpt"));
    }

    @Test
    void sinonimosTienenSimilitudUno() {
        // TF-IDF y Jaccard darian 0 porque las palabras son distintas
        assertEquals(1.0, fastText.similitud("teacher", "educator"), 1e-9);
    }

    @Test
    void vectoresPerpendicularesTienenSimilitudCero() {
        // (1, 0) y (0, 1) forman un angulo de 90 grados: coseno = 0
        assertEquals(0.0, fastText.similitud("teacher", "student"), 1e-9);
    }

    @Test
    void cosenoCalculadoAMano() {
        // A = teacher = (1, 0)   B = (teacher + student) / 2 = (0.5, 0.5)
        // A . B = 0.5    |A| = 1    |B| = raiz(0.5)    coseno = 0.5 / raiz(0.5) = 0.707
        assertEquals(0.5 / Math.sqrt(0.5), fastText.similitud("teacher", "teacher student"), 1e-9);
    }

    @Test
    void vectoresOpuestosDanCosenoNegativo() {
        // (1, 0) y (-1, 0): angulo de 180 grados, coseno = -1
        assertEquals(-1.0, Vectores.coseno(new double[]{1, 0}, new double[]{-1, 0}), 1e-9);
    }

    @Test
    void unCosenoNegativoSeTomaComoSimilitudCero() {
        // teacher = (1, 0) y robot = (-1, 0): coseno -1, similitud 0
        assertEquals(0.0, fastText.similitud("teacher", "robot"), 1e-9);
    }

    @Test
    void textoSinPalabrasConocidasTieneSimilitudCero() {
        assertEquals(0.0, fastText.similitud("chatgpt", "teacher"), 1e-9);
    }

    @Test
    void esSimetrica() {
        assertEquals(fastText.similitud("teacher student", "educator robot"),
                fastText.similitud("educator robot", "teacher student"), 1e-9);
    }
}