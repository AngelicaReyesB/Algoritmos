package co.edu.uniquindio.ranking.model;

import java.util.List;
import java.util.Map;

/**
 * Resultado de buscar un patron en el abstract de UN articulo.
 *
 * @param id           identificador del articulo en la base de datos
 * @param archivo      nombre del PDF, por ejemplo "01_gan.pdf"
 * @param titulo       titulo del articulo
 * @param resumen      el abstract completo (la web lo muestra con el patron resaltado)
 * @param posiciones   donde empieza cada ocurrencia (desde 0). Se envian una sola vez
 *                     porque el servidor ya comprobo que los tres algoritmos las encuentran igual
 * @param ocurrencias  cuantas ocurrencias encontro cada algoritmo, en el orden KMP, Boyer-Moore, Rabin-Karp
 * @param coinciden    true si los tres algoritmos encontraron exactamente las mismas posiciones
 */
public record ArticuloEncontrado(Integer id, String archivo, String titulo, String resumen,
                                 List<Integer> posiciones, Map<String, Integer> ocurrencias,
                                 boolean coinciden) {
}
