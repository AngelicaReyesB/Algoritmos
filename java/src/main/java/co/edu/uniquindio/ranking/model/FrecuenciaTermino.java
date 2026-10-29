package co.edu.uniquindio.ranking.model;

/**
 * Un termino del corpus y la cantidad de veces que aparece.
 *
 * Es un "record": Java genera automaticamente el constructor,
 * los metodos termino() y frecuencia(), equals, hashCode y toString.
 *
 * @param termino    la palabra, por ejemplo "modelo"
 * @param frecuencia cuantas veces aparece en todos los abstracts (siempre >= 1)
 */
public record FrecuenciaTermino(String termino, int frecuencia) {
}