package co.edu.uniquindio.ranking.agrupamiento;

/**
 * Regla para medir la distancia entre dos GRUPOS de articulos.
 *
 * Se comparan todos los pares (un articulo de cada grupo) y:
 * - SINGLE:   se toma la distancia MINIMA  (los dos articulos mas cercanos).
 * - COMPLETE: se toma la distancia MAXIMA  (los dos articulos mas lejanos).
 * - AVERAGE:  se toma el PROMEDIO de todas las distancias.
 *
 * Complejidad (k = cantidad de articulos), igual para los tres criterios
 * porque usan el mismo ciclo en AgrupamientoJerarquico:
 * - Hay k - 1 uniones.
 * - En cada union se revisan todos los pares de grupos, y cada par compara
 *   todos los articulos de un grupo con todos los del otro. Sumando todos
 *   los pares de grupos, eso es a lo sumo k^2 comparaciones.
 * - Total: (k - 1) * k^2 = O(k^3).
 */
public enum Criterio {
    SINGLE,
    COMPLETE,
    AVERAGE;

    public String getComplejidad() {
        return "O(k³)";
    }
}