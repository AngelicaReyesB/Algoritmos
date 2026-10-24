package co.edu.uniquindio.ranking.algoritmos;

import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;

import java.util.List;

/**
 * Counting Sort (ordenamiento por conteo).
 *
 * No compara elementos entre si: usa la clave de cada elemento
 * como posicion dentro de un arreglo de conteo.
 *
 * Complejidad: Theta(n + k)
 *   n = cantidad de terminos
 *   k = clave maxima (aqui, la frecuencia maxima del corpus)
 *
 * Orden descendente: en lugar de ordenar por frecuencia y luego invertir
 * la lista (lo que romperia la estabilidad), se ordena de forma ASCENDENTE
 * por la clave  clave = frecuenciaMaxima - frecuencia.
 * Asi la frecuencia mas alta tiene clave 0 y queda de primera.
 */
public class CountingSort implements AlgoritmoOrdenamiento {

    @Override
    public String getNombre() {
        return "Counting Sort";
    }

    @Override
    public List<FrecuenciaTermino> ordenar(List<FrecuenciaTermino> terminos) {
        int n = terminos.size();
        if (n == 0) {
            return List.of();
        }

        // Paso 1: buscar la frecuencia maxima (un recorrido, n pasos)
        int frecuenciaMaxima = 0;
        for (FrecuenciaTermino termino : terminos) {
            if (termino.frecuencia() > frecuenciaMaxima) {
                frecuenciaMaxima = termino.frecuencia();
            }
        }

        // Paso 2: calcular la clave de cada termino (n pasos)
        int[] claves = new int[n];
        for (int i = 0; i < n; i++) {
            claves[i] = frecuenciaMaxima - terminos.get(i).frecuencia();
        }
        int k = frecuenciaMaxima; // ninguna clave supera este valor

        // Paso 3: contar cuantas veces aparece cada clave (n pasos)
        int[] conteo = new int[k + 1];
        for (int i = 0; i < n; i++) {
            conteo[claves[i]]++;
        }

        // Paso 4: sumas acumuladas (k pasos).
        // Despues de esto, conteo[c] = cantidad de elementos con clave <= c,
        // es decir, la posicion final (+1) del ultimo elemento con clave c.
        for (int c = 1; c <= k; c++) {
            conteo[c] += conteo[c - 1];
        }

        // Paso 5: colocar cada termino en su posicion final (n pasos).
        // Se recorre de atras hacia adelante para que sea ESTABLE:
        // entre terminos con la misma clave, el ultimo de la entrada
        // ocupa la ultima posicion disponible para esa clave.
        FrecuenciaTermino[] salida = new FrecuenciaTermino[n];
        for (int i = n - 1; i >= 0; i--) {
            conteo[claves[i]]--;
            salida[conteo[claves[i]]] = terminos.get(i);
        }

        return List.of(salida);
    }
}