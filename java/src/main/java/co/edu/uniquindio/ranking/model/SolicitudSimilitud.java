package co.edu.uniquindio.ranking.model;

import java.util.List;

/**
 * Cuerpo de la peticion POST /api/similitud.
 *
 * Ejemplo: { "ids": [3, 7, 12] }
 *
 * @param ids ids de los articulos que se quieren comparar (minimo 2)
 */
public record SolicitudSimilitud(List<Integer> ids) {
}