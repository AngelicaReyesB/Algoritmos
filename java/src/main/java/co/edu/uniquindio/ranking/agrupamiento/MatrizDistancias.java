package co.edu.uniquindio.ranking.agrupamiento;

import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;

import java.util.List;

/**
 * Matriz de distancias entre abstracts, que usa el agrupamiento jerarquico.
 *
 *   distancia(i, j) = 1 - similitud(i, j)
 *
 * - Similitud 1 (textos iguales)       -> distancia 0.
 * - Similitud 0 (nada en comun)        -> distancia 1.
 *
 * La similitud puede ser cualquiera de los algoritmos del Requerimiento 1.
 * El proyecto usa el coseno con TF-IDF (decision de CONF-08).
 */
public final class MatrizDistancias {

    private MatrizDistancias() {
    }

    /**
     * Calcula la matriz de distancias de n x n.
     *
     * - La diagonal es 0: cada texto esta a distancia 0 de si mismo.
     * - Es simetrica: solo se calcula la parte de arriba (j > i) y se copia abajo.
     *
     * @param textos los abstracts, en el orden en que quedaran las filas y columnas
     */
    public static double[][] calcular(AlgoritmoSimilitud similitud, List<String> textos) {
        int n = textos.size();
        if (n < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 textos para agrupar");
        }

        double[][] distancias = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double distancia = 1.0 - similitud.similitud(textos.get(i), textos.get(j));
                distancias[i][j] = distancia;
                distancias[j][i] = distancia;
            }
        }
        return distancias;
    }

    /**
     * Se calculan k(k - 1)/2 pares, y cada par cuesta lo que cueste la similitud.
     * Ejemplo con TF-IDF: "O(k²) × O(n + m)".
     */
    public static String complejidad(AlgoritmoSimilitud similitud) {
        return "O(k²) × " + similitud.getComplejidad();
    }
}