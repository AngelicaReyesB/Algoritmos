package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas con un corpus de 3 documentos que se puede calcular a mano:
 *
 *   d1 = "ai education ai"
 *   d2 = "ai students"
 *   d3 = "students education teachers"
 *
 *   N = 3
 *   df: ai = 2, education = 2, students = 2, teachers = 1
 *   IDF: ai = education = students = ln(3/2) = 0.405
 *        teachers = ln(3/1) = 1.099
 */
class CosenoTfIdfTest {

    private static final String D1 = "ai education ai";
    private static final String D2 = "ai students";
    private static final String D3 = "students education teachers";

    /** Tokenizador de prueba: solo separa por espacios. */
    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final CosenoTfIdf cosenoTfIdf = new CosenoTfIdf(porEspacios, List.of(D1, D2, D3));

    @Test
    void idfDeCadaTermino() {
        assertEquals(Math.log(3.0 / 2), cosenoTfIdf.idf("ai"), 1e-9);
        assertEquals(Math.log(3.0 / 2), cosenoTfIdf.idf("education"), 1e-9);
        assertEquals(Math.log(3.0), cosenoTfIdf.idf("teachers"), 1e-9);
        assertEquals(0.0, cosenoTfIdf.idf("robots"), 1e-9); // no esta en el corpus
    }

    @Test
    void tfCuentaLasRepeticiones() {
        Map<String, Integer> tf = cosenoTfIdf.tf(D1);
        assertEquals(2, tf.get("ai").intValue());
        assertEquals(1, tf.get("education").intValue());
    }

    @Test
    void vectorTfIdfMultiplicaTfPorIdf() {
        Map<String, Double> vector = cosenoTfIdf.vectorTfIdf(D1);
        assertEquals(2 * Math.log(3.0 / 2), vector.get("ai"), 1e-9);
        assertEquals(1 * Math.log(3.0 / 2), vector.get("education"), 1e-9);
    }

    @Test
    void cosenoEntreD1yD2() {
        // Con w = ln(3/2):  d1 = (ai: 2w, education: w)   d2 = (ai: w, students: w)
        // A . B = 2w * w = 2w^2     |A| = w * raiz(5)     |B| = w * raiz(2)
        // coseno = 2w^2 / (w^2 * raiz(10)) = 2 / raiz(10) = 0.632
        assertEquals(2 / Math.sqrt(10), cosenoTfIdf.similitud(D1, D2), 1e-9);
    }

    @Test
    void cosenoEntreD1yD3() {
        // Solo comparten "education":  A . B = w * w
        // |A| = w * raiz(5)    |B| = raiz(w^2 + w^2 + t^2)  con t = ln(3)
        double w = Math.log(3.0 / 2);
        double t = Math.log(3.0);
        double esperado = (w * w) / (w * Math.sqrt(5) * Math.sqrt(2 * w * w + t * t));
        assertEquals(esperado, cosenoTfIdf.similitud(D1, D3), 1e-9);
    }

    @Test
    void textosIgualesTienenSimilitudUno() {
        assertEquals(1.0, cosenoTfIdf.similitud(D3, D3), 1e-9);
    }

    @Test
    void sinTerminosEnComunTienenSimilitudCero() {
        assertEquals(0.0, cosenoTfIdf.similitud("ai", "teachers"), 1e-9);
    }

    @Test
    void terminoQueEstaEnTodosLosDocumentosNoPesa() {
        // "ai" esta en los 3 documentos: IDF = ln(3/3) = 0
        CosenoTfIdf conAiEnTodos = new CosenoTfIdf(porEspacios, List.of("ai x", "ai y", "ai z"));
        assertEquals(0.0, conAiEnTodos.idf("ai"), 1e-9);
        // "ai" no pesa y "x" / "y" son distintos: no hay nada en comun
        assertEquals(0.0, conAiEnTodos.similitud("ai x", "ai y"), 1e-9);
    }

    @Test
    void esSimetrica() {
        assertEquals(cosenoTfIdf.similitud(D1, D3), cosenoTfIdf.similitud(D3, D1), 1e-9);
    }
}