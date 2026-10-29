package co.edu.uniquindio.ranking.modelo;

/**
 * Resultado de UN algoritmo de similitud sobre los articulos elegidos.
 *
 * @param algoritmo nombre del algoritmo
 * @param matriz    matriz[i][j] = similitud entre el articulo i y el articulo j (entre 0 y 1)
 * @param tiempoMs  tiempo que tomo calcular toda la matriz, en milisegundos
 */
public record ResultadoSimilitud(String algoritmo, double[][] matriz, double tiempoMs) {
}