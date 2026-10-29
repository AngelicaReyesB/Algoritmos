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
 * Implementado a mano: no usa indexOf, contains ni funciones equivalentes.
 * La busqueda es exacta (distingue mayusculas y minusculas) y devuelve las
 * posiciones (empezando en 0) de todas las ocurrencias, incluidas las solapadas.
 */
public class KnuthMorrisPratt {

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
     * Paso 2: recorre el texto una sola vez usando la tabla de prefijos.
     *
     * @return posiciones (desde 0) donde empieza cada ocurrencia; lista vacia si
     *         no hay ninguna, si el patron esta vacio o si es mas largo que el texto
     */
    public List<Integer> buscar(String texto, String patron) {
        if (texto == null || patron == null) {
            throw new IllegalArgumentException("El texto y el patron no pueden ser nulos");
        }

        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n) {
            return posiciones;
        }

        int[] prefijo = construirTablaPrefijos(patron);
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
}
