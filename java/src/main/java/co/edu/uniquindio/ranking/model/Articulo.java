package co.edu.uniquindio.ranking.model;

/**
 * Un articulo del corpus, tal como se guarda en la tabla "articulos".
 *
 * @param id      llave primaria (null si todavia no se ha guardado)
 * @param archivo nombre del PDF, por ejemplo "01_gan.pdf"
 * @param titulo  titulo del articulo (extraccion aproximada)
 * @param autores autores del articulo (null por ahora)
 * @param resumen el abstract
 * @param manual  true si el abstract se corrigio a mano
 */
public record Articulo(Integer id, String archivo, String titulo, String autores,
                       String resumen, boolean manual) {
}