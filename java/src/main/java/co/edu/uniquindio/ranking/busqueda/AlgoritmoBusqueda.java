package co.edu.uniquindio.ranking.busqueda;

import java.util.List;

public interface AlgoritmoBusqueda {
    String getNombre();

    /**
     * Complejidad del preprocesamiento (lo que se calcula antes de recorrer el texto),
     * escrita como la tiene el codigo: n = largo del texto, m = largo del patron.
     * Ejemplo: "O(m)". Usa ^ para los exponentes: "O(m^3)".
     */
    String getComplejidadPreprocesamiento();

    /**
     * Complejidad de la busqueda (el recorrido del texto), con el preprocesamiento ya hecho.
     * Si el peor caso y el promedio difieren, lo dice: "peor caso O(nm); promedio O(n)".
     */
    String getComplejidadBusqueda();
    List<Integer> buscar(String texto, String patron);
}
