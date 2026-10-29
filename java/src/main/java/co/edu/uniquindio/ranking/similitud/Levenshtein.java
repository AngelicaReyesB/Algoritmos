package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;

import java.util.List;

/**
 * Distancia de Levenshtein (distancia de edicion) a nivel de PALABRAS.
 *
 * La distancia es el minimo numero de operaciones para convertir la lista A
 * en la lista B. Cada operacion cuesta 1:
 * - insertar una palabra,
 * - eliminar una palabra,
 * - sustituir una palabra por otra.
 *
 * Se calcula con programacion dinamica: matriz[i][j] es la distancia entre
 * las primeras i palabras de A y las primeras j palabras de B.
 *
 * Se trabaja con palabras y no con caracteres porque dos abstracts de unas
 * 200 palabras dan una matriz de 201 x 201; con caracteres serian unos
 * 1500 x 1500.
 */
public class Levenshtein implements AlgoritmoSimilitud {

    private final Tokenizador tokenizador;

    public Levenshtein(Tokenizador tokenizador) {
        this.tokenizador = tokenizador;
    }

    @Override
    public String getNombre() {
        return "Levenshtein";
    }

    /**
     * Similitud = 1 - distancia / largo de la lista mas larga.
     * Asi queda entre 0 (hay que cambiarlo todo) y 1 (textos iguales).
     */
    @Override
    public double similitud(String textoA, String textoB) {
        List<String> a = tokenizador.tokenizar(textoA);
        List<String> b = tokenizador.tokenizar(textoB);
        int largoMayor = Math.max(a.size(), b.size());
        if (largoMayor == 0) {
            return 1.0; // dos textos sin palabras se consideran iguales
        }
        return 1.0 - (double) distancia(a, b) / largoMayor;
    }

    /** La distancia es la ultima celda de la matriz. */
    public static int distancia(List<String> a, List<String> b) {
        int[][] matriz = construirMatriz(a, b);
        return matriz[a.size()][b.size()];
    }

    /**
     * Llena la matriz de programacion dinamica de (a.size() + 1) x (b.size() + 1).
     *
     * Es publica para poder imprimirla en el caso de estudio.
     */
    public static int[][] construirMatriz(List<String> a, List<String> b) {
        int filas = a.size() + 1;
        int columnas = b.size() + 1;
        int[][] matriz = new int[filas][columnas];

        // Fila 0: convertir una lista vacia en las primeras j palabras de B = j inserciones
        for (int j = 0; j < columnas; j++) {
            matriz[0][j] = j;
        }
        // Columna 0: convertir las primeras i palabras de A en una lista vacia = i eliminaciones
        for (int i = 0; i < filas; i++) {
            matriz[i][0] = i;
        }

        for (int i = 1; i < filas; i++) {
            for (int j = 1; j < columnas; j++) {
                // Si las palabras son iguales, la diagonal no cuesta nada
                int costoSustitucion = a.get(i - 1).equals(b.get(j - 1)) ? 0 : 1;

                int eliminar = matriz[i - 1][j] + 1;                    // celda de arriba
                int insertar = matriz[i][j - 1] + 1;                    // celda de la izquierda
                int sustituir = matriz[i - 1][j - 1] + costoSustitucion; // diagonal

                matriz[i][j] = Math.min(eliminar, Math.min(insertar, sustituir));
            }
        }
        return matriz;
    }
}