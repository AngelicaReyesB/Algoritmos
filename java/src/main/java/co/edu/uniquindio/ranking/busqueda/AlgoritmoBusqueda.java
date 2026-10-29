package co.edu.uniquindio.ranking.busqueda;

import java.util.List;

/**
 * Contrato comun de los tres algoritmos de busqueda de patrones
 * (KMP, Boyer-Moore y Rabin-Karp).
 *
 * Permite que el resto del programa (medicion de tiempos, API, pruebas)
 * trate igual a los tres algoritmos.
 *
 * Regla del proyecto: no se pueden usar indexOf(), contains(), matches()
 * ni expresiones regulares para buscar. La comparacion se hace
 * caracter por caracter.
 */
public interface AlgoritmoBusqueda {

    /** Nombre del algoritmo para mostrar en tablas y graficos. */
    String getNombre();

    /**
     * Busca todas las ocurrencias exactas del patron en el texto.
     *
     * @return las posiciones (desde 0) donde empieza cada ocurrencia, en orden.
     *         Incluye ocurrencias solapadas: "aa" en "aaaa" devuelve [0, 1, 2].
     *         Si el patron esta vacio o es mas largo que el texto, devuelve una lista vacia.
     */
    List<Integer> buscar(String texto, String patron);
}