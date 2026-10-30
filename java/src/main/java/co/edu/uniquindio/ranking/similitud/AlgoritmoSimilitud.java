package co.edu.uniquindio.ranking.similitud;

/**
 * Contrato comun de los seis algoritmos de similitud textual:
 *
 * - Clasicos (paquete similitud): Levenshtein, Needleman-Wunsch,
 *   coseno con TF-IDF y Jaccard.
 * - Con IA (paquete embeddings): FastText y MiniLM.
 *
 * Cada implementacion decide como preparar el texto: los clasicos y FastText
 * usan el tokenizador propio; MiniLM recibe el abstract completo.
 */
public interface AlgoritmoSimilitud {

    /** Nombre del algoritmo para mostrar en tablas y graficos. */
    String getNombre();

    /**
     * Complejidad temporal de comparar UN par de textos, en notacion Big O.
     * n y m = cantidad de palabras de cada texto; d = dimension del vector.
     */
    String getComplejidad();

    /**
     * Similitud entre dos textos.
     *
     * @return un valor entre 0 y 1: 1 = textos iguales, 0 = nada en comun.
     */
    double similitud(String textoA, String textoB);
}