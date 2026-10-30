package co.edu.uniquindio.ranking.model;

import co.edu.uniquindio.ranking.agrupamiento.Union;

import java.util.List;

/**
 * Resultado del agrupamiento con UN criterio.
 *
 * @param criterio              SINGLE, COMPLETE o AVERAGE
 * @param complejidad           complejidad del agrupamiento (Big O)
 * @param uniones               las n - 1 uniones del dendrograma, en orden
 * @param correlacionCofenetica que tan bien el dendrograma respeta las distancias (cerca de 1 = mejor)
 * @param tiempoMs              tiempo del agrupamiento, en milisegundos
 * @param tiempoCorrelacionMs   tiempo de la correlacion cofenetica, en milisegundos
 */
public record DendrogramaCriterio(String criterio, String complejidad, List<Union> uniones,
                                  double correlacionCofenetica, double tiempoMs,
                                  double tiempoCorrelacionMs) {
}