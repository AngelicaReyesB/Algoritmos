package co.edu.uniquindio.ranking.agrupamiento;

/**
 * Un paso del agrupamiento: dos grupos se unen en uno nuevo.
 *
 * Numeracion de los grupos (la misma que usan scipy y R):
 * - Los n articulos son los grupos 0, 1, ..., n-1.
 * - Cada union crea un grupo nuevo con el siguiente numero: n, n+1, ...
 *
 * @param grupoA     uno de los grupos que se unen
 * @param grupoB     el otro grupo
 * @param grupoNuevo numero del grupo que resulta de la union
 * @param altura     distancia entre los dos grupos al unirse (altura en el dendrograma)
 * @param tamano     cuantos articulos tiene el grupo nuevo
 */
public record Union(int grupoA, int grupoB, int grupoNuevo, double altura, int tamano) {
}