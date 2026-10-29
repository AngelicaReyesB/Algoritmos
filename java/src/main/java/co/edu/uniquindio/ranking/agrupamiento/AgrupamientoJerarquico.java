package co.edu.uniquindio.ranking.agrupamiento;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupamiento jerarquico aglomerativo, implementado desde cero.
 *
 * 1. Al inicio cada articulo es un grupo de un solo elemento.
 * 2. Se buscan los dos grupos mas cercanos, segun el criterio
 *    (SINGLE, COMPLETE o AVERAGE).
 * 3. Se unen en un grupo nuevo y se guarda la union (que grupos y a que altura).
 * 4. Se repite hasta que queda un solo grupo con todos los articulos.
 *
 * Con n articulos siempre hay n - 1 uniones. Esa lista de uniones es
 * el dendrograma.
 *
 * Los tres criterios usan exactamente el mismo ciclo: lo unico que cambia
 * es como se mide la distancia entre dos grupos (distanciaEntreGrupos).
 */
public final class AgrupamientoJerarquico {

    private AgrupamientoJerarquico() {
    }

    /** Un grupo: su numero y los articulos (posiciones en la matriz) que contiene. */
    private record Grupo(int numero, List<Integer> articulos) {
    }

    /**
     * @param distancias matriz de distancias de n x n (de MatrizDistancias)
     * @param criterio   SINGLE, COMPLETE o AVERAGE
     * @return las n - 1 uniones, en el orden en que ocurrieron
     */
    public static List<Union> agrupar(double[][] distancias, Criterio criterio) {
        int n = distancias.length;

        // Paso 1: un grupo por articulo
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            grupos.add(new Grupo(i, List.of(i)));
        }

        List<Union> uniones = new ArrayList<>();
        int siguienteNumero = n;

        while (grupos.size() > 1) {
            // Paso 2: buscar el par de grupos mas cercano
            int mejorA = 0;
            int mejorB = 1;
            double menorDistancia = Double.MAX_VALUE;
            for (int a = 0; a < grupos.size(); a++) {
                for (int b = a + 1; b < grupos.size(); b++) {
                    double distancia = distanciaEntreGrupos(grupos.get(a), grupos.get(b), distancias, criterio);
                    if (distancia < menorDistancia) {
                        menorDistancia = distancia;
                        mejorA = a;
                        mejorB = b;
                    }
                }
            }

            // Paso 3: unir los dos grupos y guardar la union
            Grupo grupoA = grupos.get(mejorA);
            Grupo grupoB = grupos.get(mejorB);
            List<Integer> articulosUnidos = new ArrayList<>(grupoA.articulos());
            articulosUnidos.addAll(grupoB.articulos());
            Grupo nuevo = new Grupo(siguienteNumero, articulosUnidos);

            uniones.add(new Union(grupoA.numero(), grupoB.numero(), nuevo.numero(),
                    menorDistancia, articulosUnidos.size()));
            siguienteNumero++;

            // Se quita primero el de mayor posicion para no correr la otra posicion
            grupos.remove(mejorB);
            grupos.remove(mejorA);
            grupos.add(nuevo);
        }
        return uniones;
    }

    /**
     * Distancia entre dos grupos segun el criterio.
     * Recorre todos los pares (un articulo de cada grupo) en la matriz original.
     */
    static double distanciaEntreGrupos(Grupo a, Grupo b, double[][] distancias, Criterio criterio) {
        double minima = Double.MAX_VALUE;
        double maxima = 0;
        double suma = 0;

        for (int i : a.articulos()) {
            for (int j : b.articulos()) {
                double d = distancias[i][j];
                minima = Math.min(minima, d);
                maxima = Math.max(maxima, d);
                suma += d;
            }
        }

        return switch (criterio) {
            case SINGLE -> minima;
            case COMPLETE -> maxima;
            case AVERAGE -> suma / (a.articulos().size() * b.articulos().size());
        };
    }
}