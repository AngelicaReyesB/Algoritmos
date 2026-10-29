package co.edu.uniquindio.ranking.busqueda;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class KnuthMorrisPrattTest {

    private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();

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

    // ---------- Busqueda ----------

    @Test
    void encuentraUnaOcurrencia() {
        assertEquals(List.of(4), kmp.buscar("los algoritmos de busqueda", "algoritmos"));
    }

    @Test
    void encuentraVariasOcurrencias() {
        assertEquals(List.of(0, 8, 16), kmp.buscar("modelo, modelo, modelo", "modelo"));
    }

    @Test
    void encuentraOcurrenciasSolapadas() {
        assertEquals(List.of(0, 1, 2), kmp.buscar("aaaa", "aa"));
        assertEquals(List.of(0, 2), kmp.buscar("ababa", "aba"));
    }

    @Test
    void ejemploClasicoConRetroceso() {
        assertEquals(List.of(10), kmp.buscar("ababdabacdababcabab", "ababcabab"));
    }

    @Test
    void sinCoincidenciaDevuelveListaVacia() {
        assertTrue(kmp.buscar("inteligencia artificial", "xyz").isEmpty());
    }

    @Test
    void patronAlInicioYAlFinal() {
        assertEquals(List.of(0, 9), kmp.buscar("modelo y modelo", "modelo"));
    }

    @Test
    void patronIgualAlTexto() {
        assertEquals(List.of(0), kmp.buscar("gpt", "gpt"));
    }

    @Test
    void patronVacioOMasLargoQueElTexto() {
        assertTrue(kmp.buscar("texto", "").isEmpty());
        assertTrue(kmp.buscar("ab", "abc").isEmpty());
        assertTrue(kmp.buscar("", "a").isEmpty());
    }

    @Test
    void distingueMayusculasDeMinusculas() {
        assertTrue(kmp.buscar("Models", "models").isEmpty());
        assertEquals(List.of(0), kmp.buscar("Models", "Models"));
    }

    @Test
    void funcionaConTildesYEnie() {
        assertEquals(List.of(3), kmp.buscar("el análisis de año", "análisis"));
        assertEquals(List.of(15), kmp.buscar("el análisis de año", "año"));
    }

    @Test
    void rechazaEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar("a", null));
    }

    // ---------- Comparacion con una referencia (solo en pruebas) ----------

    @Test
    void casosAleatoriosCoincidenConLaReferencia() {
        Random azar = new Random(42);
        for (int caso = 0; caso < 500; caso++) {
            String texto = cadenaAleatoria(azar, azar.nextInt(60));
            String patron = cadenaAleatoria(azar, 1 + azar.nextInt(5));
            assertEquals(referencia(texto, patron), kmp.buscar(texto, patron),
                    "texto=" + texto + " patron=" + patron);
        }
    }

    /** Alfabeto de 2 letras: fuerza muchos empates y solapamientos. */
    private String cadenaAleatoria(Random azar, int largo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < largo; i++) {
            sb.append(azar.nextBoolean() ? 'a' : 'b');
        }
        return sb.toString();
    }

    /** Busqueda ingenua: prueba cada posicion. Solo sirve para verificar a KMP. */
    private List<Integer> referencia(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i + patron.length() <= texto.length(); i++) {
            if (texto.startsWith(patron, i)) {
                posiciones.add(i);
            }
        }
        return posiciones;
    }
}
