package co.edu.uniquindio.ranking.model;

import co.edu.uniquindio.ranking.medicion.ResultadoMedicion;

/**
 * Tiempos de un algoritmo de busqueda para un patron, medidos sobre los 20 abstracts.
 *
 * @param algoritmo        "KMP", "Boyer-Moore" o "Rabin-Karp"
 * @param preprocesamiento tabla de prefijos (KMP), tablas del caracter malo y sufijo bueno
 *                         (Boyer-Moore) o hashes iniciales de cada abstract (Rabin-Karp)
 * @param busqueda         el recorrido de los 20 abstracts, con el preprocesamiento ya hecho
 * @param colisiones       solo Rabin-Karp: veces que el hash coincidio y el texto era distinto
 *                         (null en los otros dos algoritmos)
 */
public record TiempoBusqueda(String algoritmo, ResultadoMedicion preprocesamiento,
                             ResultadoMedicion busqueda, Integer colisiones) {
}
