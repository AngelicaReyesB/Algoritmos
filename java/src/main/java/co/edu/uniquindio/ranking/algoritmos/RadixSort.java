package co.edu.uniquindio.ranking.algoritmos;

import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;

import java.util.List;

/**
 * Radix Sort LSD (Least Significant Digit: del digito menos significativo al mas significativo).
 *
 * Ordena las claves digito por digito, empezando por las unidades,
 * luego las decenas, luego las centenas, etc. En cada pasada usa un
 * Counting Sort ESTABLE sobre ese digito (valores 0 a 9).
 *
 * Complejidad: Theta(d * (n + b))
 *   n = cantidad de terminos
 *   b = base (10)
 *   d = cantidad de digitos de la clave maxima
 * Como b es fijo y d es pequeno, en la practica es Theta(n).
 *
 * Usa la misma clave que Counting Sort:  clave = frecuenciaMaxima - frecuencia,
 * para que el resultado quede de mayor a menor frecuencia.
 */
public class RadixSort implements AlgoritmoOrdenamiento {

    private static final int BASE = 10;

    @Override
    public String getNombre() {
        return "Radix Sort";
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

        // Paso 2: copiar los terminos a un arreglo propio y calcular sus claves.
        // Se trabaja sobre esta copia para no modificar la lista de entrada.
        FrecuenciaTermino[] actuales = new FrecuenciaTermino[n];
        int[] claves = new int[n];
        int claveMaxima = 0;
        for (int i = 0; i < n; i++) {
            actuales[i] = terminos.get(i);
            claves[i] = frecuenciaMaxima - terminos.get(i).frecuencia();
            if (claves[i] > claveMaxima) {
                claveMaxima = claves[i];
            }
        }

        // Paso 3: una pasada por cada digito de la clave maxima.
        // exp = 1 (unidades), 10 (decenas), 100 (centenas), ...
        // El ciclo termina cuando la clave maxima ya no tiene mas digitos.
        for (int exp = 1; claveMaxima / exp > 0; exp *= BASE) {
            ordenarPorDigito(actuales, claves, exp);
        }

        return List.of(actuales);
    }

    /**
     * Counting Sort ESTABLE sobre un solo digito de las claves.
     * Reordena los arreglos "terminos" y "claves" al mismo tiempo.
     *
     * Es el mismo procedimiento de CountingSort, pero el arreglo de conteo
     * siempre tiene 10 posiciones (digitos 0 a 9).
     */
    private void ordenarPorDigito(FrecuenciaTermino[] terminos, int[] claves, int exp) {
        int n = terminos.length;

        // Contar cuantas claves tienen cada digito (0 a 9)
        int[] conteo = new int[BASE];
        for (int i = 0; i < n; i++) {
            int digito = (claves[i] / exp) % BASE;
            conteo[digito]++;
        }

        // Sumas acumuladas: posicion final de cada digito
        for (int d = 1; d < BASE; d++) {
            conteo[d] += conteo[d - 1];
        }

        // Colocar de atras hacia adelante para que sea ESTABLE.
        // La estabilidad es lo que hace funcionar a Radix Sort: el orden
        // logrado con los digitos anteriores no se pierde en esta pasada.
        FrecuenciaTermino[] salidaTerminos = new FrecuenciaTermino[n];
        int[] salidaClaves = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            int digito = (claves[i] / exp) % BASE;
            conteo[digito]--;
            salidaTerminos[conteo[digito]] = terminos[i];
            salidaClaves[conteo[digito]] = claves[i];
        }

        // Copiar el resultado de vuelta para la siguiente pasada
        for (int i = 0; i < n; i++) {
            terminos[i] = salidaTerminos[i];
            claves[i] = salidaClaves[i];
        }
    }
}