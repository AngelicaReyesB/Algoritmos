package co.edu.uniquindio.ranking.busqueda;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de KMP.
 *
 * Hereda las pruebas comunes de PruebasAlgoritmoBusqueda (inicio, final, varias
 * ocurrencias, solapadas, vacios, mayusculas, comparacion con fuerza bruta...) y
 * agrega las propias: la tabla de prefijos y la busqueda con la tabla ya construida.
 */
class KnuthMorrisPrattTest extends PruebasAlgoritmoBusqueda {

    private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();

    @Override
    protected AlgoritmoBusqueda crearAlgoritmo() {
        return kmp;
    }

    // ---------- Tabla de prefijos ----------

    @Test
    void tablaDeEjemploDelInforme() {
        assertArrayEquals(new int[] {0, 0, 1, 2, 3, 0, 1}, kmp.construirTablaPrefijos("ababaca"));
    }

    @Test
    void tablaConTodosLosCaracteresIguales() {
        assertArrayEquals(new int[] {0, 1, 2, 3}, kmp.construirTablaPrefijos("aaaa"));
    }

    @Test
    void tablaSinRepeticiones() {
        assertArrayEquals(new int[] {0, 0, 0, 0}, kmp.construirTablaPrefijos("abcd"));
    }

    @Test
    void tablaConRetrocesoEnCadena() {
        // En "aabaaab", al llegar a la ultima 'b' hay que retroceder dos veces
        assertArrayEquals(new int[] {0, 1, 0, 1, 2, 2, 3}, kmp.construirTablaPrefijos("aabaaab"));
    }

    // ---------- Preprocesamiento y busqueda por separado ----------

    @Test
    void buscarConLaTablaYaConstruidaDaLoMismo() {
        String texto = "abababca";
        String patron = "ababca";
        int[] tabla = kmp.construirTablaPrefijos(patron);

        assertEquals(kmp.buscar(texto, patron), kmp.buscar(texto, patron, tabla));
        assertEquals(List.of(2), kmp.buscar(texto, patron, tabla));
    }

    @Test
    void buscarConTablaYaConstruidaYPatronVacioDevuelveListaVacia() {
        assertEquals(List.of(), kmp.buscar("texto", "", new int[0]));
    }

    // ---------- Entradas invalidas ----------

    @Test
    void rechazaEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar("a", null));
    }

    @Test
    void sabeSuNombre() {
        assertEquals("KMP", kmp.getNombre());
    }
}
