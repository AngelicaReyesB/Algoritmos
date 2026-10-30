package co.edu.uniquindio.ranking.agrupamiento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Correlacion cofenetica: metrica interna para saber que tan bien un
 * dendrograma respeta las distancias originales.
 *
 * 1. Distancia cofenetica entre dos articulos = altura a la que el
 *    dendrograma los junta por primera vez en el mismo grupo.
 * 2. Se comparan, par por par, la distancia original y la cofenetica
 *    con la correlacion de Pearson.
 *
 * Resultado entre -1 y 1. Cuanto mas cerca de 1, mejor representa el
 * dendrograma las distancias reales. El criterio (Single, Complete o
 * Average) con el valor mas alto es el que agrupa de forma mas coherente.
 */
public final class CorrelacionCofenetica {

    /**
     * La matriz cofenetica llena k x k celdas y Pearson recorre k(k - 1)/2 pares.
     */
    public static final String COMPLEJIDAD = "O(k²)";

    private CorrelacionCofenetica() {
    }

    /** Distancias originales y uniones de un criterio -> correlacion cofenetica. */
    public static double calcular(double[][] distancias, List<Union> uniones) {
        double[][] cofenetica = matrizCofenetica(uniones, distancias.length);
        return pearson(paresDeArriba(distancias), paresDeArriba(cofenetica));
    }

    /**
     * Matriz cofenetica de n x n.
     *
     * Se recorren las uniones en orden. Cuando se unen los grupos A y B, todos
     * los pares (un articulo de A y uno de B) quedan juntos por primera vez,
     * asi que su distancia cofenetica es la altura de esa union.
     */
    public static double[][] matrizCofenetica(List<Union> uniones, int n) {
        // Articulos de cada grupo. Al inicio el grupo i solo tiene al articulo i.
        Map<Integer, List<Integer>> articulosDelGrupo = new HashMap<>();
        for (int i = 0; i < n; i++) {
            articulosDelGrupo.put(i, List.of(i));
        }

        double[][] cofenetica = new double[n][n];
        for (Union union : uniones) {
            List<Integer> articulosA = articulosDelGrupo.get(union.grupoA());
            List<Integer> articulosB = articulosDelGrupo.get(union.grupoB());

            for (int i : articulosA) {
                for (int j : articulosB) {
                    cofenetica[i][j] = union.altura();
                    cofenetica[j][i] = union.altura();
                }
            }

            List<Integer> articulosNuevo = new ArrayList<>(articulosA);
            articulosNuevo.addAll(articulosB);
            articulosDelGrupo.put(union.grupoNuevo(), articulosNuevo);
        }
        return cofenetica;
    }

    /**
     * Valores de arriba de la diagonal (j > i), en orden: cada par una sola vez.
     * La diagonal (siempre 0) y la parte de abajo (repetida) no se usan.
     */
    static List<Double> paresDeArriba(double[][] matriz) {
        List<Double> valores = new ArrayList<>();
        for (int i = 0; i < matriz.length; i++) {
            for (int j = i + 1; j < matriz.length; j++) {
                valores.add(matriz[i][j]);
            }
        }
        return valores;
    }

    /**
     * Correlacion de Pearson entre dos listas del mismo tamano:
     *
     *   r = suma((x - mediaX) * (y - mediaY)) / raiz(suma((x - mediaX)^2) * suma((y - mediaY)^2))
     *
     * Si una de las listas tiene todos sus valores iguales no hay variacion
     * que comparar y se devuelve 0.
     */
    public static double pearson(List<Double> x, List<Double> y) {
        int n = x.size();
        double mediaX = 0;
        double mediaY = 0;
        for (int k = 0; k < n; k++) {
            mediaX += x.get(k);
            mediaY += y.get(k);
        }
        mediaX /= n;
        mediaY /= n;

        double covarianza = 0;
        double varianzaX = 0;
        double varianzaY = 0;
        for (int k = 0; k < n; k++) {
            double dx = x.get(k) - mediaX;
            double dy = y.get(k) - mediaY;
            covarianza += dx * dy;
            varianzaX += dx * dx;
            varianzaY += dy * dy;
        }

        if (varianzaX == 0 || varianzaY == 0) {
            return 0.0;
        }
        return covarianza / Math.sqrt(varianzaX * varianzaY);
    }
}