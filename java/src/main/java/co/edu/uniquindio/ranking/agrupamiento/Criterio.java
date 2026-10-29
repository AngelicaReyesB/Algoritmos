package co.edu.uniquindio.ranking.agrupamiento;

/**
 * Regla para medir la distancia entre dos GRUPOS de articulos.
 *
 * Se comparan todos los pares (un articulo de cada grupo) y:
 * - SINGLE:   se toma la distancia MINIMA  (los dos articulos mas cercanos).
 * - COMPLETE: se toma la distancia MAXIMA  (los dos articulos mas lejanos).
 * - AVERAGE:  se toma el PROMEDIO de todas las distancias.
 */
public enum Criterio {
    SINGLE,
    COMPLETE,
    AVERAGE
}