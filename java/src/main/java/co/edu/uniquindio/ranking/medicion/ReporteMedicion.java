package co.edu.uniquindio.ranking.medicion;

import java.util.List;

/**
 * Todo lo que produce una medicion completa: tiempos por etapa,
 * datos del corpus, validacion del ranking y datos del equipo.
 *
 * @param resultados        tiempos de cada etapa
 * @param terminosTotales   terminos despues del preprocesamiento (con repeticiones)
 * @param terminosDistintos n: terminos distintos (tokenizador propio)
 * @param terminosDistintosLucene terminos distintos con el tokenizador de Lucene
 * @param frecuenciaMaxima  k: frecuencia del termino mas comun
 * @param coinciden         true si los tres algoritmos dieron el mismo ranking
 * @param equipo            caracteristicas del equipo
 */
public record ReporteMedicion(List<ResultadoMedicion> resultados, int terminosTotales,
                              int terminosDistintos, int terminosDistintosLucene,
                              int frecuenciaMaxima, boolean coinciden,
                              InformacionEquipo equipo) {
}