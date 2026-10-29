package co.edu.uniquindio.ranking.model;

import java.util.List;

/**
 * Respuesta de POST /api/similitud.
 *
 * @param articulos  los articulos comparados, en el mismo orden que las filas y columnas de las matrices
 * @param resultados una matriz por cada uno de los 6 algoritmos
 */
public record ComparacionSimilitud(List<Articulo> articulos, List<ResultadoSimilitud> resultados) {
}