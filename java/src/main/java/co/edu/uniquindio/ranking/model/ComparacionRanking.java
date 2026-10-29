package co.edu.uniquindio.ranking.modelo;

import java.util.List;
import java.util.Map;

/**
 * Resultado de ordenar las frecuencias con los tres algoritmos.
 *
 * @param terminosDistintos cantidad de terminos distintos del corpus (n)
 * @param frecuenciaMaxima  frecuencia del termino mas comun (k)
 * @param coinciden         true si los tres algoritmos dieron el mismo ranking
 * @param rankings          Top N de cada algoritmo; la llave es el nombre del algoritmo
 */
public record ComparacionRanking(int terminosDistintos, int frecuenciaMaxima, boolean coinciden,
                                 Map<String, List<FrecuenciaTermino>> rankings) {
}