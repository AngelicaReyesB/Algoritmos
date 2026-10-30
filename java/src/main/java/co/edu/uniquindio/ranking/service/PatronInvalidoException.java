package co.edu.uniquindio.ranking.service;

/**
 * El patron que se quiere buscar no es valido (vacio o demasiado largo).
 * El controlador la convierte en una respuesta 400.
 */
public class PatronInvalidoException extends IllegalArgumentException {

    public PatronInvalidoException(String mensaje) {
        super(mensaje);
    }
}
