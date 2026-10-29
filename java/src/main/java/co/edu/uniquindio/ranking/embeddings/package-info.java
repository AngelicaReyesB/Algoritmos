/**
 * Similitud con modelos preentrenados (Requerimiento 1, enfoques de IA).
 *
 * - GloVe: tabla fija palabra -> vector de 50 numeros. El vector de un
 *   abstract es el promedio de los vectores de sus palabras.
 * - MiniLM (all-MiniLM-L6-v2, con LangChain4j): recibe el abstract completo
 *   y devuelve un vector de 384 numeros que tiene en cuenta el contexto.
 *
 * En los dos casos la similitud del coseno entre vectores se calcula a mano.
 */
package co.edu.uniquindio.ranking.embeddings;