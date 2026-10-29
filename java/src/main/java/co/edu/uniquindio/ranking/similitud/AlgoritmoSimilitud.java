package co.edu.uniquindio.ranking.similitud;

/**
 * Contrato comun de los seis algoritmos de similitud textual:
 *
 * - Clasicos (paquete similitud): Levenshtein, Needleman-Wunsch,
 *   coseno con TF-IDF y Jaccard.
 * - Con IA (paquete embeddings): GloVe y MiniLM.
 *
 * Cada implementacion decide como preparar el texto: los clasicos y GloVe
 * usan el tokenizador propio; MiniLM recibe el abstract completo.
 */
public interface AlgoritmoSimilitud {

    /** Nombre del algoritmo para mostrar en tablas y graficos. */
    String getNombre();

    /**
     * Similitud entre dos textos.
     *
     * @return un valor entre 0 y 1: 1 = textos iguales, 0 = nada en comun.
     */
    double similitud(String textoA, String textoB);
}