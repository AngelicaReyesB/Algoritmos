package co.edu.uniquindio.ranking.busqueda;

import java.util.ArrayList;
import java.util.List;

/**
 * Algoritmo de Rabin-Karp para buscar un patron dentro de un texto.
 *
 * Idea: en lugar de comparar el patron caracter por caracter en cada posicion, se
 * compara un numero (hash) que resume cada cadena. Solo cuando los hash son
 * iguales se comparan los caracteres.
 *
 * Hash de una cadena c[0..m-1] (aritmetica modular, base B y modulo primo q):
 *
 *   hash = (c[0]*B^(m-1) + c[1]*B^(m-2) + ... + c[m-1]) mod q
 *
 * Hash rodante: el hash de la ventana siguiente se obtiene del anterior en O(1),
 * quitando el primer caracter y agregando el nuevo:
 *
 *   siguiente = ((anterior - c[i] * h) * B + c[i+m]) mod q,   con h = B^(m-1) mod q
 *
 * Como muchas cadenas distintas comparten un mismo hash, un hash igual NO prueba
 * que haya una ocurrencia. Por eso se verifica caracter por caracter. Si el hash
 * es igual pero el texto es distinto, se cuenta una COLISION.
 *
 * Parametros por defecto: base 256 y modulo primo 101.
 *
 * Notacion: n = longitud del texto, m = longitud del patron, q = modulo.
 *
 * Costos:
 *  - Preprocesamiento: O(m) (hash del patron, hash de la primera ventana y h).
 *  - Busqueda: O(n + m) en promedio, cuando hay pocas colisiones.
 *  - Peor caso: O(n * m), si casi todas las ventanas colisionan (por ejemplo con
 *    un modulo muy pequeno) y cada verificacion recorre casi todo el patron.
 *
 * El trabajo se divide en dos etapas que se pueden medir por separado:
 *  - Preprocesamiento: preprocesar(texto, patron).
 *  - Busqueda: buscarContandoColisiones(texto, patron, preprocesamiento).
 * buscar(texto, patron) hace las dos seguidas.
 *
 * Implementado a mano: no usa indexOf, contains, equals ni funciones equivalentes.
 * La busqueda es exacta (distingue mayusculas y minusculas) y devuelve las
 * posiciones (empezando en 0) de todas las ocurrencias, incluidas las solapadas.
 */
public class RabinKarp implements AlgoritmoBusqueda {

    /** Base del hash: el numero de valores que suele tomar un caracter ASCII. */
    public static final int BASE = 256;

    /** Modulo primo por defecto. */
    public static final int MODULO_POR_DEFECTO = 101;

    /** Resultado de una busqueda: donde esta el patron y cuantas colisiones hubo. */
    public record Resultado(List<Integer> posiciones, int colisiones) {
    }

    /**
     * Lo que se calcula antes de recorrer el texto:
     *  - pesoPrimero: h = BASE^(m-1) mod modulo, el peso del primer caracter de la ventana.
     *  - hashPatron: hash del patron.
     *  - hashPrimeraVentana: hash de los primeros m caracteres del texto.
     */
    public record Preprocesamiento(long pesoPrimero, int hashPatron, int hashPrimeraVentana) {
    }

    private final int modulo;

    /** Rabin-Karp con los parametros por defecto: base 256 y modulo 101. */
    public RabinKarp() {
        this(MODULO_POR_DEFECTO);
    }

    /**
     * Rabin-Karp con un modulo propio. Un modulo pequeno provoca colisiones a
     * proposito, lo que sirve para probar que no se reportan como ocurrencias.
     */
    public RabinKarp(int modulo) {
        if (modulo < 1) {
            throw new IllegalArgumentException("El modulo debe ser mayor o igual a 1");
        }
        this.modulo = modulo;
    }

    public int getModulo() {
        return modulo;
    }

    @Override
    public String getNombre() {
        return "Rabin-Karp";
    }

    /**
     * Complejidad real del codigo (n = largo del texto, m = largo del patron).
     * Preprocesamiento: h, hash del patron y hash de la primera ventana, O(m) por texto.
     * Busqueda: hash rodante, O(n) en el promedio; O(nm) en el peor caso, cuando casi
     * todas las ventanas tienen el mismo hash que el patron y hay que verificarlas.
     */
    @Override
    public String getComplejidadPreprocesamiento() {
        return "O(m)";
    }

    @Override
    public String getComplejidadBusqueda() {
        return "peor caso O(nm); promedio O(n)";
    }

    /**
     * Hash de texto[inicio .. inicio + largo - 1], calculado de izquierda a derecha:
     * hash = (hash * BASE + caracter) mod modulo.
     *
     * Ejemplo con base 256 y modulo 101: "ab" -> (97 * 256 + 98) mod 101 = 84
     */
    public int calcularHash(String texto, int inicio, int largo) {
        long hash = 0;
        for (int i = inicio; i < inicio + largo; i++) {
            hash = (hash * BASE + texto.charAt(i)) % modulo;
        }
        return (int) hash;
    }

    /** Etapa 1: calcula h, el hash del patron y el hash de la primera ventana. */
    public Preprocesamiento preprocesar(String texto, String patron) {
        comprobarNoNulos(texto, patron);
        int m = patron.length();
        if (m == 0 || m > texto.length()) {
            return new Preprocesamiento(1, 0, 0); // no habra busqueda: no se usa
        }

        // h = BASE^(m-1) mod modulo
        long h = 1;
        for (int i = 1; i < m; i++) {
            h = (h * BASE) % modulo;
        }
        return new Preprocesamiento(h, calcularHash(patron, 0, m), calcularHash(texto, 0, m));
    }

    /** Preprocesamiento + busqueda. */
    @Override
    public List<Integer> buscar(String texto, String patron) {
        return buscarContandoColisiones(texto, patron).posiciones();
    }

    /** Etapa 2 de buscar: recibe el preprocesamiento ya calculado. */
    public List<Integer> buscar(String texto, String patron, Preprocesamiento pre) {
        return buscarContandoColisiones(texto, patron, pre).posiciones();
    }

    /**
     * Preprocesamiento + busqueda, contando las colisiones.
     *
     * @return posiciones (desde 0) donde empieza cada ocurrencia, y el numero de
     *         veces que el hash coincidio pero el texto era distinto. Sin
     *         posiciones y con 0 colisiones si el patron esta vacio o es mas largo
     *         que el texto.
     */
    public Resultado buscarContandoColisiones(String texto, String patron) {
        comprobarNoNulos(texto, patron);
        if (patron.isEmpty() || patron.length() > texto.length()) {
            return new Resultado(new ArrayList<>(), 0);
        }
        return buscarContandoColisiones(texto, patron, preprocesar(texto, patron));
    }

    /** Etapa 2: recorre el texto con el hash rodante, usando el preprocesamiento ya calculado. */
    public Resultado buscarContandoColisiones(String texto, String patron, Preprocesamiento pre) {
        comprobarNoNulos(texto, patron);

        List<Integer> posiciones = new ArrayList<>();
        int colisiones = 0;
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n) {
            return new Resultado(posiciones, 0);
        }

        long h = pre.pesoPrimero();
        int hashPatron = pre.hashPatron();
        long hashVentana = pre.hashPrimeraVentana();

        for (int i = 0; i <= n - m; i++) {
            if (hashVentana == hashPatron) {
                // Mismo hash: puede ser una ocurrencia o una colision. Se verifica.
                if (coincideEn(texto, patron, i)) {
                    posiciones.add(i);
                } else {
                    colisiones++;
                }
            }
            // Hash rodante: quitar el primer caracter de la ventana y agregar el siguiente
            if (i < n - m) {
                long aporteDelPrimero = (texto.charAt(i) * h) % modulo;
                hashVentana = ((hashVentana - aporteDelPrimero + modulo) * BASE + texto.charAt(i + m)) % modulo;
            }
        }
        return new Resultado(posiciones, colisiones);
    }

    /** Compara el patron con el texto desde la posicion dada, caracter por caracter. */
    private boolean coincideEn(String texto, String patron, int inicio) {
        for (int j = 0; j < patron.length(); j++) {
            if (texto.charAt(inicio + j) != patron.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static void comprobarNoNulos(String texto, String patron) {
        if (texto == null || patron == null) {
            throw new IllegalArgumentException("El texto y el patron no pueden ser nulos");
        }
    }
}
