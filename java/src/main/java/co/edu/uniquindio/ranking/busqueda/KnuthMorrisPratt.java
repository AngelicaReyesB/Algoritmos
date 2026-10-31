package co.edu.uniquindio.ranking.busqueda;

import java.util.ArrayList;
import java.util.List;

/**
 * Algoritmo de Knuth-Morris-Pratt (KMP) para buscar un patron dentro de un texto.
 *
 * Idea: cuando una comparacion falla, el texto NUNCA retrocede. Lo que ya sabemos
 * del patron (que parte coincidio) indica a cual posicion del patron saltar.
 *
 * Costo: preprocesamiento O(m) + busqueda O(n) = O(n + m), con n = longitud del
 * texto y m = longitud del patron.
 *
 * El trabajo se divide en dos etapas que se pueden medir por separado:
 *  - Preprocesamiento: construirTablaPrefijos(patron).
 *  - Busqueda: buscar(texto, patron, tablaPrefijos), con la tabla ya construida.
 * buscar(texto, patron) hace las dos seguidas.
 *
 * Implementado a mano: no usa indexOf, contains ni funciones equivalentes.
 * La busqueda es exacta (distingue mayusculas y minusculas) y devuelve las
 * posiciones (empezando en 0) de todas las ocurrencias, incluidas las solapadas.
 */
public class KnuthMorrisPratt implements AlgoritmoBusqueda {

    @Override
    public String getNombre() {
        return "KMP";
    }

    /**
     * Complejidad real del codigo (n = largo del texto, m = largo del patron).
     * Tabla de prefijos: un solo recorrido del patron, O(m).
     * Busqueda: el texto nunca retrocede, O(n) en el peor caso y en el promedio.
     */
    @Override
    public String getComplejidadPreprocesamiento() {
        return "O(m)";
    }

    @Override
    public String getComplejidadBusqueda() {
        return "O(n)";
    }

    /**
     * Paso 1: tabla de prefijos (tambien llamada tabla LPS o funcion de fallo).
     *
     * prefijo[i] = longitud del prefijo mas largo de patron[0..i] que ademas es
     * sufijo de patron[0..i], sin ser el fragmento completo (el "borde" mas largo).
     *
     * Ejemplo con "ababaca":
     *   i        0 1 2 3 4 5 6
     *   patron   a b a b a c a
     *   prefijo  0 0 1 2 3 0 1
     */
    public int[] construirTablaPrefijos(String patron) {
        int m = patron.length();
        int[] prefijo = new int[m];
        int k = 0; // longitud del borde que llevamos hasta ahora

        for (int i = 1; i < m; i++) {
            // Si no extiende el borde actual, probamos con el borde mas corto
            while (k > 0 && patron.charAt(i) != patron.charAt(k)) {
                k = prefijo[k - 1];
            }
            // Si extiende el borde, crece en uno
            if (patron.charAt(i) == patron.charAt(k)) {
                k++;
            }
            prefijo[i] = k;
        }
        return prefijo;
    }

    /**
     * Preprocesamiento + busqueda.
     *
     * @return posiciones (desde 0) donde empieza cada ocurrencia; lista vacia si
     *         no hay ninguna, si el patron esta vacio o si es mas largo que el texto
     */
    @Override
    public List<Integer> buscar(String texto, String patron) {
        comprobarNoNulos(texto, patron);
        if (patron.isEmpty() || patron.length() > texto.length()) {
            return new ArrayList<>();
        }
        return buscar(texto, patron, construirTablaPrefijos(patron));
    }

    /**
     * Paso 2: recorre el texto una sola vez usando la tabla de prefijos ya
     * construida con construirTablaPrefijos(patron).
     */
    public List<Integer> buscar(String texto, String patron, int[] prefijo) {
        comprobarNoNulos(texto, patron);

        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n) {
            return posiciones;
        }

        int j = 0; // cuantos caracteres del patron coinciden en este momento

        for (int i = 0; i < n; i++) {
            // Fallo: en vez de retroceder en el texto, retrocedemos j en el patron
            while (j > 0 && texto.charAt(i) != patron.charAt(j)) {
                j = prefijo[j - 1];
            }
            // Acierto: avanzamos en el patron
            if (texto.charAt(i) == patron.charAt(j)) {
                j++;
            }
            // Patron completo: guardamos donde empezo y seguimos (permite solapamiento)
            if (j == m) {
                posiciones.add(i - m + 1);
                j = prefijo[j - 1];
            }
        }
        return posiciones;
    }

    private static void comprobarNoNulos(String texto, String patron) {
        if (texto == null || patron == null) {
            throw new IllegalArgumentException("El texto y el patron no pueden ser nulos");
        }
    }
}
