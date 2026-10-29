package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de Jaccard con ejemplos que se pueden verificar a mano.
 */
class JaccardTest {

    /** Tokenizador de prueba: solo separa por espacios. */
    private final Tokenizador porEspacios = texto -> texto.isBlank()
            ? List.of()
            : Arrays.asList(texto.trim().split("\\s+"));

    private final Jaccard jaccard = new Jaccard(porEspacios);

    @Test
    void interseccionYUnion() {
        // A = {ai, education, students}   B = {ai, students, teachers, schools}
        // A interseccion B = {ai, students} -> 2
        // A union B = {ai, education, students, teachers, schools} -> 5
        Set<String> a = Set.of("ai", "education", "students");
        Set<String> b = Set.of("ai", "students", "teachers", "schools");
        assertEquals(2, Jaccard.interseccion(a, b));
        assertEquals(5, Jaccard.union(a, b));
        assertEquals(2.0 / 5, Jaccard.jaccard(a, b), 1e-9);
    }

    @Test
    void lasRepeticionesNoCuentan() {
        // "ai ai ai education" y "ai education" tienen el mismo conjunto {ai, education}
        assertEquals(1.0, jaccard.similitud("ai ai ai education", "ai education"), 1e-9);
    }

    @Test
    void elOrdenNoImporta() {
        assertEquals(1.0, jaccard.similitud("deep learning models", "models learning deep"), 1e-9);
    }

    @Test
    void sinTerminosEnComunEsCero() {
        assertEquals(0.0, jaccard.similitud("deep learning models", "students teachers schools"), 1e-9);
    }

    @Test
    void textosIgualesEsUno() {
        assertEquals(1.0, jaccard.similitud("generative ai", "generative ai"), 1e-9);
    }

    @Test
    void esSimetrica() {
        String a = "generative ai transforms assessment";
        String b = "ai assessment in higher education";
        assertEquals(jaccard.similitud(a, b), jaccard.similitud(b, a), 1e-9);
    }

    @Test
    void dosTextosVaciosSeConsideranIguales() {
        assertEquals(1.0, jaccard.similitud("", ""), 1e-9);
    }
}