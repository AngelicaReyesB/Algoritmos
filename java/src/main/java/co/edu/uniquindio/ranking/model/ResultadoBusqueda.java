package co.edu.uniquindio.ranking.model;

import java.util.List;

/**
 * Respuesta de GET /api/busqueda?patron=...
 *
 * @param patron                  el patron buscado, tal como llego
 * @param longitudPatron          cantidad de caracteres del patron
 * @param coinciden               true si, en TODOS los articulos, KMP, Boyer-Moore y Rabin-Karp
 *                                encontraron las mismas posiciones
 * @param totalOcurrencias        ocurrencias en todo el corpus
 * @param articulosConOcurrencias articulos donde aparece el patron al menos una vez
 * @param totalArticulos          articulos revisados
 * @param articulos               un elemento por articulo, en el orden en que se cargaron
 * @param tiempos                 un elemento por algoritmo
 */
public record ResultadoBusqueda(String patron, int longitudPatron, boolean coinciden,
                                int totalOcurrencias, int articulosConOcurrencias,
                                int totalArticulos, List<ArticuloEncontrado> articulos,
                                List<TiempoBusqueda> tiempos) {
}
