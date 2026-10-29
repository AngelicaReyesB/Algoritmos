package co.edu.uniquindio.ranking.busqueda;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas comunes para los tres algoritmos de busqueda de patrones.
 *
 * Cada algoritmo tiene su propia clase de prueba que hereda de esta
 * y solo indica que algoritmo se prueba (metodo crearAlgoritmo).
 * Asi las mismas pruebas se aplican a KMP, Boyer-Moore y Rabin-Karp.
 */
abstract class PruebasAlgoritmoBusqueda {

    /** Cada clase hija devuelve el algoritmo que quiere probar. */
    protected abstract AlgoritmoBusqueda crearAlgoritmo();

    /**
     * Resultado esperado, calculado con fuerza bruta: probar el patron en
     * cada posicion del texto y comparar caracter por caracter.
     * Solo se usa en las PRUEBAS como referencia.
     */
    private static List<Integer> fuerzaBruta(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        if (patron.isEmpty()) {
            return posiciones;
        }
        for (int i = 0; i + patron.length() <= texto.length(); i++) {
            int j = 0;
            while (j < patron.length() && texto.charAt(i + j) == patron.charAt(j)) {
                j++;
            }
            if (j == patron.length()) {
                posiciones.add(i);
            }
        }
        return posiciones;
    }

    @Test
    void patronAlInicio() {
        assertEquals(List.of(0), crearAlgoritmo().buscar("generative models", "gen"));
    }

    @Test
    void patronAlFinal() {
        assertEquals(List.of(14), crearAlgoritmo().buscar("generative models", "els"));
    }

    @Test
    void variasOcurrencias() {
        // "ai" esta en las posiciones 0, 10 y 24
        assertEquals(List.of(0, 10, 24), crearAlgoritmo().buscar("ai helps; ai in schools ai", "ai"));
    }

    @Test
    void ocurrenciasSolapadas() {
        assertEquals(List.of(0, 1, 2), crearAlgoritmo().buscar("aaaa", "aa"));
    }

    @Test
    void sinOcurrencias() {
        assertEquals(List.of(), crearAlgoritmo().buscar("generative models", "xyz"));
    }

    @Test
    void patronMasLargoQueElTexto() {
        assertEquals(List.of(), crearAlgoritmo().buscar("ai", "artificial"));
    }

    @Test
    void textoVacio() {
        assertEquals(List.of(), crearAlgoritmo().buscar("", "ai"));
    }

    @Test
    void patronVacio() {
        assertEquals(List.of(), crearAlgoritmo().buscar("generative models", ""));
    }

    @Test
    void textoIgualAlPatron() {
        assertEquals(List.of(0), crearAlgoritmo().buscar("education", "education"));
    }

    @Test
    void distingueMayusculasYMinusculas() {
        // La busqueda es exacta: "AI" no es lo mismo que "ai"
        assertEquals(List.of(0), crearAlgoritmo().buscar("AI and ai", "AI"));
    }

    @Test
    void ejemploClasicoDeLibro() {
        // Patron con repeticiones internas: obliga a usar bien las tablas
        assertEquals(List.of(2, 8), crearAlgoritmo().buscar("abababacababacab", "ababaca"));
    }

    @Test
    void coincideConFuerzaBrutaEnTextosAleatorios() {
        // Alfabeto pequeno (a, b, c) para que haya muchas coincidencias parciales
        Random aleatorio = new Random(42);
        AlgoritmoBusqueda algoritmo = crearAlgoritmo();
        for (int prueba = 0; prueba < 500; prueba++) {
            String texto = cadenaAleatoria(aleatorio, aleatorio.nextInt(60));
            String patron = cadenaAleatoria(aleatorio, 1 + aleatorio.nextInt(5));
            assertEquals(fuerzaBruta(texto, patron), algoritmo.buscar(texto, patron),
                    "texto=\"" + texto + "\" patron=\"" + patron + "\"");
        }
    }

    private static String cadenaAleatoria(Random aleatorio, int largo) {
        StringBuilder cadena = new StringBuilder();
        for (int i = 0; i < largo; i++) {
            cadena.append((char) ('a' + aleatorio.nextInt(3)));
        }
        return cadena.toString();
    }
}