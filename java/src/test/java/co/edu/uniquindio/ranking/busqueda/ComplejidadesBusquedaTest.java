package co.edu.uniquindio.ranking.busqueda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * La web (tabla del analisis critico) muestra estas complejidades tal cual. Si alguien
 * cambia el codigo de un algoritmo, esta prueba obliga a revisar tambien lo que se afirma.
 */
class ComplejidadesBusquedaTest {

    @Test
    void kmpDeclaraSuComplejidad() {
        KnuthMorrisPratt kmp = new KnuthMorrisPratt();

        assertEquals("O(m)", kmp.getComplejidadPreprocesamiento());
        assertEquals("O(n)", kmp.getComplejidadBusqueda());
    }

    @Test
    void boyerMooreDeclaraElSufijoBuenoCubicoComoEnElCodigo() {
        BoyerMoore boyerMoore = new BoyerMoore();

        // La tabla del sufijo bueno se construye desde su definicion: O(m^3), no la version lineal
        assertEquals("O(m^3)", boyerMoore.getComplejidadPreprocesamiento());
        assertEquals("peor caso O(nm); mejor caso O(n/m)", boyerMoore.getComplejidadBusqueda());
    }

    @Test
    void rabinKarpDeclaraSuComplejidad() {
        RabinKarp rabinKarp = new RabinKarp();

        assertEquals("O(m)", rabinKarp.getComplejidadPreprocesamiento());
        assertEquals("peor caso O(nm); promedio O(n)", rabinKarp.getComplejidadBusqueda());
    }

    @Test
    void todasUsanLaNotacionO() {
        List<AlgoritmoBusqueda> algoritmos = List.of(new KnuthMorrisPratt(), new BoyerMoore(), new RabinKarp());

        for (AlgoritmoBusqueda algoritmo : algoritmos) {
            assertTrue(algoritmo.getComplejidadPreprocesamiento().contains("O("), algoritmo.getNombre());
            assertTrue(algoritmo.getComplejidadBusqueda().contains("O("), algoritmo.getNombre());
        }
    }
}
