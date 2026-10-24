package co.edu.uniquindio.ranking.texto;

import java.util.List;

/**
 * Convierte un texto en la lista de terminos que se van a contar.
 *
 * Hay dos implementaciones:
 * - TokenizadorPropio: escrito por el equipo (version sin librerias).
 * - TokenizadorLucene: usa Apache Lucene (version con librerias).
 */
public interface Tokenizador {

    List<String> tokenizar(String texto);
}