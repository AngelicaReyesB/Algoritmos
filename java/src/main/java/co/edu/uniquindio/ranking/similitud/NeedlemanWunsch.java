package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Alineamiento global de Needleman-Wunsch a nivel de PALABRAS.
 *
 * Coloca las dos listas de palabras una debajo de la otra, insertando
 * espacios ("-") donde haga falta, para que coincidan la mayor cantidad
 * de palabras posible. Cada posicion del alineamiento suma o resta puntos:
 * - las dos palabras son iguales:   +1 (COINCIDENCIA)
 * - las dos palabras son distintas: -1 (DESAJUSTE)
 * - una palabra frente a un espacio: -1 (ESPACIO)
 *
 * Se calcula con programacion dinamica, igual que Levenshtein, pero
 * buscando el MAXIMO puntaje en vez de la minima distancia.
 *
 * La similitud se calcula con el puntaje (no con el alineamiento), porque
 * puede haber varios alineamientos con el mismo puntaje maximo; el puntaje
 * en cambio es unico y da lo mismo comparar A con B que B con A.
 */
public class NeedlemanWunsch implements AlgoritmoSimilitud {

    public static final int COINCIDENCIA = 1;
    public static final int DESAJUSTE = -1;
    public static final int ESPACIO = -1;

    /** Lo que se escribe en el alineamiento donde hay un espacio. */
    public static final String HUECO = "-";

    /** Las dos filas del alineamiento, del mismo largo. */
    public record Alineamiento(List<String> filaA, List<String> filaB) {

        /** Cuantas posiciones tienen la misma palabra arriba y abajo. */
        public int coincidencias() {
            int total = 0;
            for (int k = 0; k < filaA.size(); k++) {
                if (filaA.get(k).equals(filaB.get(k))) {
                    total++;
                }
            }
            return total;
        }
    }

    private final Tokenizador tokenizador;

    public NeedlemanWunsch(Tokenizador tokenizador) {
        this.tokenizador = tokenizador;
    }

    @Override
    public String getNombre() {
        return "Needleman-Wunsch";
    }

    /**
     * Lleva el puntaje del mejor alineamiento a un valor entre 0 y 1.
     *
     * Con n = largo de la lista mas corta y M = largo de la mas larga:
     * - Si todo coincide, el puntaje maximo posible es +n (n coincidencias).
     * - Si nada coincide, el puntaje es -M (cada posicion resta 1).
     * Entonces: similitud = (puntaje + M) / (n + M).
     *
     * Ejemplo: [ai, in, education] con [ai, education]
     * puntaje = +1 (ai) -1 (in frente a un espacio) +1 (education) = 1
     * similitud = (1 + 3) / (2 + 3) = 0.8
     */
    @Override
    public double similitud(String textoA, String textoB) {
        List<String> a = tokenizador.tokenizar(textoA);
        List<String> b = tokenizador.tokenizar(textoB);
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0; // dos textos sin palabras se consideran iguales
        }
        int largoCorto = Math.min(a.size(), b.size());
        int largoMayor = Math.max(a.size(), b.size());
        return (double) (puntaje(a, b) + largoMayor) / (largoCorto + largoMayor);
    }

    /** Puntos que suma comparar dos palabras: +1 si son iguales, -1 si no. */
    private static int puntos(String palabraA, String palabraB) {
        return palabraA.equals(palabraB) ? COINCIDENCIA : DESAJUSTE;
    }

    /** El puntaje del mejor alineamiento es la ultima celda de la matriz. */
    public static int puntaje(List<String> a, List<String> b) {
        int[][] matriz = construirMatriz(a, b);
        return matriz[a.size()][b.size()];
    }

    /**
     * Llena la matriz de programacion dinamica de (a.size() + 1) x (b.size() + 1).
     * matriz[i][j] = mejor puntaje al alinear las primeras i palabras de A
     * con las primeras j palabras de B.
     *
     * Es publica para poder imprimirla en el caso de estudio.
     */
    public static int[][] construirMatriz(List<String> a, List<String> b) {
        int filas = a.size() + 1;
        int columnas = b.size() + 1;
        int[][] matriz = new int[filas][columnas];

        // Fila 0 y columna 0: alinear contra una lista vacia = solo espacios
        for (int j = 0; j < columnas; j++) {
            matriz[0][j] = j * ESPACIO;
        }
        for (int i = 0; i < filas; i++) {
            matriz[i][0] = i * ESPACIO;
        }

        for (int i = 1; i < filas; i++) {
            for (int j = 1; j < columnas; j++) {
                int diagonal = matriz[i - 1][j - 1] + puntos(a.get(i - 1), b.get(j - 1)); // palabra con palabra
                int arriba = matriz[i - 1][j] + ESPACIO;     // palabra de A frente a un espacio
                int izquierda = matriz[i][j - 1] + ESPACIO;  // espacio frente a palabra de B

                matriz[i][j] = Math.max(diagonal, Math.max(arriba, izquierda));
            }
        }
        return matriz;
    }

    /**
     * Recorrido hacia atras: parte de la ultima celda y en cada paso mira de
     * cual de las tres celdas vecinas salio el valor. Asi reconstruye el
     * alineamiento de derecha a izquierda.
     */
    public static Alineamiento alinear(List<String> a, List<String> b) {
        int[][] matriz = construirMatriz(a, b);
        List<String> filaA = new ArrayList<>();
        List<String> filaB = new ArrayList<>();

        int i = a.size();
        int j = b.size();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && matriz[i][j] == matriz[i - 1][j - 1] + puntos(a.get(i - 1), b.get(j - 1))) {
                // Vino de la diagonal: palabra con palabra
                filaA.add(a.get(i - 1));
                filaB.add(b.get(j - 1));
                i--;
                j--;
            } else if (i > 0 && matriz[i][j] == matriz[i - 1][j] + ESPACIO) {
                // Vino de arriba: la palabra de A queda frente a un espacio
                filaA.add(a.get(i - 1));
                filaB.add(HUECO);
                i--;
            } else {
                // Vino de la izquierda: un espacio queda frente a la palabra de B
                filaA.add(HUECO);
                filaB.add(b.get(j - 1));
                j--;
            }
        }

        // Se armo de derecha a izquierda: hay que darle la vuelta
        Collections.reverse(filaA);
        Collections.reverse(filaB);
        return new Alineamiento(filaA, filaB);
    }
}