package co.edu.uniquindio.ranking.model;

import java.util.List;

/**
 * Respuesta de GET /api/agrupamiento.
 *
 * @param similitud              similitud usada para la distancia ("tfidf" o "minilm")
 * @param articulos              los articulos; la posicion en esta lista es el numero de hoja (0, 1, ...)
 * @param complejidadMatriz      complejidad de calcular la matriz de distancias (Big O)
 * @param tiempoMatrizMs         tiempo de la matriz de distancias, en milisegundos
 * @param complejidadCorrelacion complejidad de la correlacion cofenetica (Big O)
 * @param dendrogramas           uno por cada criterio
 * @param mejorCriterio          el criterio con la correlacion cofenetica mas alta
 */
public record ResultadoAgrupamiento(String similitud, List<Articulo> articulos,
                                    String complejidadMatriz, double tiempoMatrizMs,
                                    String complejidadCorrelacion,
                                    List<DendrogramaCriterio> dendrogramas, String mejorCriterio) {
}