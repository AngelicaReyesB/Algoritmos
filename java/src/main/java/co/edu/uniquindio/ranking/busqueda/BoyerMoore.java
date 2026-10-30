package co.edu.uniquindio.ranking.busqueda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritmo de Boyer-Moore para buscar un patron dentro de un texto.
 *
 * Idea: el patron se alinea con el texto y se compara de DERECHA A IZQUIERDA.
 * Cuando hay un fallo, dos reglas calculan cuanto se puede desplazar el patron
 * sin perder ninguna ocurrencia, y se avanza con el MAYOR de los dos:
 *
 *   1. Regla del caracter malo: alinear el caracter del texto que fallo con su
 *      ultima aparicion en el patron.
 *   2. Regla del sufijo bueno: alinear el trozo final que si coincidio con otra
 *      aparicion de ese mismo trozo dentro del patron.
 *
 * Notacion: n = longitud del texto, m = longitud del patron.
 *
 * Costos:
 *  - Tabla del caracter malo: O(m).
 *  - Tabla del sufijo bueno: se construye directamente desde su definicion, lo que
 *    cuesta O(m^3) en el peor caso. Es aceptable porque el patron es corto; existe
 *    una construccion lineal O(m) mas dificil de explicar.
 *  - Busqueda: O(n / m) en el mejor caso, O(n * m) en el peor.
 *
 * El trabajo se divide en dos etapas que se pueden medir por separado:
 *  - Preprocesamiento: construirTablaCaracterMalo(patron) y construirTablaSufijoBueno(patron).
 *  - Busqueda: buscar(texto, patron, caracterMalo, sufijoBueno), con las tablas ya construidas.
 * buscar(texto, patron) hace las dos seguidas.
 *
 * Implementado a mano: no usa indexOf, contains ni funciones equivalentes.
 * La busqueda es exacta (distingue mayusculas y minusculas) y devuelve las
 * posiciones (empezando en 0) de todas las ocurrencias, incluidas las solapadas.
 */
public class BoyerMoore implements AlgoritmoBusqueda {

    @Override
    public String getNombre() {
        return "Boyer-Moore";
    }

    /**
     * Tabla del caracter malo: para cada caracter del patron, la posicion (desde 0)
     * de su ULTIMA aparicion. Un caracter que no esta en la tabla no aparece en el
     * patron.
     *
     * Ejemplo con "GCAGAGAG":
     *   G -> 7, C -> 1, A -> 6
     *
     * Se guarda en un LinkedHashMap para que, al imprimirla en el informe, los
     * caracteres salgan en el orden en que aparecen por primera vez en el patron.
     */
    public Map<Character, Integer> construirTablaCaracterMalo(String patron) {
        Map<Character, Integer> ultimaPosicion = new LinkedHashMap<>();
        for (int i = 0; i < patron.length(); i++) {
            ultimaPosicion.put(patron.charAt(i), i); // al repetirse, se queda la ultima
        }
        return ultimaPosicion;
    }

    /**
     * Tabla del sufijo bueno: sufijoBueno[k] es el desplazamiento que se aplica
     * cuando ya coincidieron los ultimos k caracteres del patron y el siguiente
     * (hacia la izquierda) fallo. k va de 0 a m; k = m significa que el patron
     * coincidio completo.
     *
     * sufijoBueno[k] es el MENOR desplazamiento s (1 <= s <= m) que cumple:
     *   (a) Al mover el patron s posiciones a la derecha, lo que queda encima de
     *       los k caracteres ya coincidentes sigue coincidiendo. Es decir,
     *       patron[i - s] == patron[i] para cada i entre m - k y m - 1 con i - s >= 0.
     *   (b) El caracter que quedaria en la posicion del fallo es distinto del que
     *       acaba de fallar: patron[j - s] != patron[j], con j = m - 1 - k, solo si
     *       j - s >= 0. Si fuera igual, fallaria otra vez.
     * Si ningun s menor sirve, s = m (se salta el patron completo).
     *
     * Ejemplo con "aaaa": sufijoBueno = [4, 3, 2, 1, 1]
     */
    public int[] construirTablaSufijoBueno(String patron) {
        int m = patron.length();
        int[] tabla = new int[m + 1];
        for (int k = 0; k <= m; k++) {
            int j = m - 1 - k; // posicion del fallo (-1 si coincidio el patron completo)
            int s = 1;
            while (s < m && !desplazamientoValido(patron, k, j, s)) {
                s++;
            }
            tabla[k] = s;
        }
        return tabla;
    }

    /** Comprueba las condiciones (a) y (b) de la tabla del sufijo bueno para un s. */
    private boolean desplazamientoValido(String patron, int k, int j, int s) {
        int m = patron.length();
        // (a) el patron desplazado sigue coincidiendo con el sufijo ya leido
        for (int i = m - k; i < m; i++) {
            if (i - s >= 0 && patron.charAt(i - s) != patron.charAt(i)) {
                return false;
            }
        }
        // (b) el caracter que fallo no puede volver a quedar en la posicion del fallo
        if (j >= 0 && j - s >= 0 && patron.charAt(j - s) == patron.charAt(j)) {
            return false;
        }
        return true;
    }

    /**
     * Preprocesamiento (las dos tablas) + busqueda.
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
        return buscar(texto, patron, construirTablaCaracterMalo(patron), construirTablaSufijoBueno(patron));
    }

    /**
     * Busqueda con las dos tablas ya construidas (construirTablaCaracterMalo y
     * construirTablaSufijoBueno), para medir la busqueda sin el preprocesamiento.
     */
    public List<Integer> buscar(String texto, String patron,
                                Map<Character, Integer> caracterMalo, int[] sufijoBueno) {
        comprobarNoNulos(texto, patron);

        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n) {
            return posiciones;
        }

        int s = 0; // posicion del texto donde esta alineado el inicio del patron
        while (s <= n - m) {
            // Se compara de derecha a izquierda
            int j = m - 1;
            while (j >= 0 && patron.charAt(j) == texto.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // Coincidio todo el patron: guardamos y desplazamos segun el sufijo bueno
                posiciones.add(s);
                s += sufijoBueno[m];
            } else {
                // Fallo en la posicion j del patron, tras k coincidencias
                int k = m - 1 - j;
                int ultima = caracterMalo.getOrDefault(texto.charAt(s + j), -1);
                int desplazamientoCaracterMalo = Math.max(1, j - ultima);
                int desplazamientoSufijoBueno = sufijoBueno[k];
                // Se avanza con el mayor de los dos desplazamientos
                s += Math.max(desplazamientoCaracterMalo, desplazamientoSufijoBueno);
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
