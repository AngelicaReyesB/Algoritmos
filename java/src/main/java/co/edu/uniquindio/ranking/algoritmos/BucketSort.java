package co.edu.uniquindio.ranking.algoritmos;

import co.edu.uniquindio.ranking.model.FrecuenciaTermino;

import java.util.ArrayList;
import java.util.List;

/**
 * Bucket Sort (ordenamiento por cubetas).
 *
 * 1. Reparte los elementos en m cubetas segun el rango de su clave.
 * 2. Ordena cada cubeta por separado con Insertion Sort.
 * 3. Concatena las cubetas en orden.
 *
 * Complejidad:
 *   - Promedio Theta(n) si las claves se distribuyen de forma uniforme
 *     y se usan m = n cubetas (cada cubeta queda con ~1 elemento).
 *   - Peor caso O(n^2): todas las claves caen en la misma cubeta.
 *
 * Usa la misma clave que Counting y Radix Sort:
 *   clave = frecuenciaMaxima - frecuencia
 * para que el resultado quede de mayor a menor frecuencia.
 */
public class BucketSort implements AlgoritmoOrdenamiento {

    @Override
    public String getNombre() {
        return "Bucket Sort";
    }

    @Override
    public List<FrecuenciaTermino> ordenar(List<FrecuenciaTermino> terminos) {
        int n = terminos.size();
        if (n == 0) {
            return List.of();
        }

        // Paso 1: buscar la frecuencia maxima
        int frecuenciaMaxima = 0;
        for (FrecuenciaTermino termino : terminos) {
            if (termino.frecuencia() > frecuenciaMaxima) {
                frecuenciaMaxima = termino.frecuencia();
            }
        }

        // Paso 2: calcular la clave de cada termino y la clave maxima
        int[] claves = new int[n];
        int claveMaxima = 0;
        for (int i = 0; i < n; i++) {
            claves[i] = frecuenciaMaxima - terminos.get(i).frecuencia();
            if (claves[i] > claveMaxima) {
                claveMaxima = claves[i];
            }
        }

        // Paso 3: crear m = n cubetas vacias
        int m = n;
        List<List<FrecuenciaTermino>> cubetas = new ArrayList<>(m);
        for (int j = 0; j < m; j++) {
            cubetas.add(new ArrayList<>());
        }

        // Paso 4: repartir cada termino en su cubeta.
        // La formula convierte la clave (0..claveMaxima) en un indice (0..m-1):
        //   indice = clave * m / (claveMaxima + 1)
        // Claves pequenas van a las primeras cubetas y claves grandes a las ultimas.
        // Se recorre la entrada en orden, asi cada cubeta conserva el orden de entrada.
        for (int i = 0; i < n; i++) {
            int indice = claves[i] * m / (claveMaxima + 1);
            cubetas.get(indice).add(terminos.get(i));
        }

        // Paso 5: ordenar cada cubeta y Paso 6: concatenarlas en orden
        List<FrecuenciaTermino> resultado = new ArrayList<>(n);
        for (List<FrecuenciaTermino> cubeta : cubetas) {
            insertionSort(cubeta);
            resultado.addAll(cubeta);
        }
        return resultado;
    }

    /**
     * Insertion Sort ESTABLE de una cubeta, de mayor a menor frecuencia
     * (equivale a ordenar de menor a mayor clave).
     *
     * Solo mueve un elemento hacia atras si el anterior tiene frecuencia
     * ESTRICTAMENTE menor. Con frecuencias iguales no lo mueve, y por eso
     * los empates conservan su orden (estabilidad).
     */
    private void insertionSort(List<FrecuenciaTermino> cubeta) {
        for (int i = 1; i < cubeta.size(); i++) {
            FrecuenciaTermino actual = cubeta.get(i);
            int j = i - 1;
            while (j >= 0 && cubeta.get(j).frecuencia() < actual.frecuencia()) {
                cubeta.set(j + 1, cubeta.get(j)); // desplazar a la derecha
                j--;
            }
            cubeta.set(j + 1, actual);
        }
    }
}