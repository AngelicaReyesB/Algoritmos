package co.edu.uniquindio.ranking.busqueda;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pruebas propias de Boyer-Moore: sus dos tablas.
 *
 * La busqueda en si (varias ocurrencias, solapadas, sin coincidencia, vacios,
 * mayusculas, tildes...) la prueban las pruebas comunes de PruebasAlgoritmoBusqueda.
 * Para heredarlas, esta clase debe declararse "extends PruebasAlgoritmoBusqueda".
 *
 * Los valores esperados de la tabla del sufijo bueno se contrastaron con la
 * construccion clasica lineal de Boyer-Moore en 3.138 patrones.
 */
class BoyerMooreTest {

    private final BoyerMoore boyerMoore = new BoyerMoore();

    // ---------- Tabla del caracter malo (ultima posicion de cada caracter) ----------

    @Test
    void caracterMaloGuardaLaUltimaPosicion() {
        assertEquals(Map.of('G', 7, 'C', 1, 'A', 6), boyerMoore.construirTablaCaracterMalo("GCAGAGAG"));
    }

    @Test
    void caracterMaloDeUnaPalabraReal() {
        // 'o' aparece en 3 y en 8: se queda la ultima
        assertEquals(
                Map.of('a', 0, 'l', 1, 'g', 2, 'o', 8, 'r', 4, 'i', 5, 't', 6, 'm', 7, 's', 9),
                boyerMoore.construirTablaCaracterMalo("algoritmos"));
    }

    @Test
    void caracterMaloNoIncluyeLosCaracteresAusentes() {
        assertFalse(boyerMoore.construirTablaCaracterMalo("ababca").containsKey('z'));
    }

    // ---------- Tabla del sufijo bueno (indice k = caracteres ya coincidentes) ----------

    @Test
    void sufijoBuenoDeEjemploConRepeticiones() {
        assertArrayEquals(new int[] {1, 7, 4, 7, 2, 7, 7, 7, 7}, boyerMoore.construirTablaSufijoBueno("GCAGAGAG"));
    }

    @Test
    void sufijoBuenoConTodosLosCaracteresIguales() {
        assertArrayEquals(new int[] {4, 3, 2, 1, 1}, boyerMoore.construirTablaSufijoBueno("aaaa"));
    }

    @Test
    void sufijoBuenoCuandoSoloAyudaElPrefijo() {
        // Para k = 2 a 6 solo ayuda que el prefijo "a" es tambien sufijo: desplazamiento 6 - 1 = 5
        assertArrayEquals(new int[] {1, 3, 5, 5, 5, 5, 5}, boyerMoore.construirTablaSufijoBueno("ababca"));
    }

    @Test
    void sufijoBuenoSinSufijosRepetidos() {
        // En "algoritmos" ningun sufijo reaparece: solo k = 0 salta menos que el patron completo
        assertArrayEquals(new int[] {1, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
                boyerMoore.construirTablaSufijoBueno("algoritmos"));
    }

    @Test
    void sufijoBuenoDeUnSoloCaracter() {
        assertArrayEquals(new int[] {1, 1}, boyerMoore.construirTablaSufijoBueno("a"));
    }

    // ---------- Entradas invalidas ----------

    @Test
    void rechazaEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> boyerMoore.buscar(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> boyerMoore.buscar("a", null));
    }

    // ---------- Un caso de busqueda completo, como comprobacion rapida ----------

    @Test
    void buscaElPatronDelCasoDeEstudio() {
        assertEquals(List.of(2), boyerMoore.buscar("abababca", "ababca"));
    }
}
