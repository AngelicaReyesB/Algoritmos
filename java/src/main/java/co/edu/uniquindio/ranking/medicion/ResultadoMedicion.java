package co.edu.uniquindio.ranking.medicion;

/**
 * Resultado de medir una etapa varias veces.
 *
 * @param etapa         nombre de lo que se midio, por ejemplo "Counting Sort"
 * @param repeticiones  cuantas veces se midio
 * @param mediaMs       tiempo promedio en milisegundos
 * @param desviacionMs  desviacion estandar en milisegundos
 * @param minimoMs      tiempo mas rapido en milisegundos
 */
public record ResultadoMedicion(String etapa, int repeticiones,
                                double mediaMs, double desviacionMs, double minimoMs) {
}