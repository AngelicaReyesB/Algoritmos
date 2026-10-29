package co.edu.uniquindio.ranking.algoritmos;

import co.edu.uniquindio.ranking.model.FrecuenciaTermino;

import java.util.List;

/**
 * Contrato comun de los tres algoritmos de ordenamiento.
 *
 * Permite que el resto del programa (medicion de tiempos, API, pruebas)
 * trate igual a Counting Sort, Radix Sort y Bucket Sort.
 */
public interface AlgoritmoOrdenamiento {

    /** Nombre del algoritmo para mostrar en tablas y graficos. */
    String getNombre();

    /**
     * Ordena los terminos de MAYOR a MENOR frecuencia.
     *
     * Reglas que deben cumplir las tres implementaciones:
     * - Devuelven una lista NUEVA; la lista de entrada no se modifica.
     * - Son estables: si dos terminos tienen la misma frecuencia,
     *   conservan el orden en que venian en la entrada.
     */
    List<FrecuenciaTermino> ordenar(List<FrecuenciaTermino> terminos);
}