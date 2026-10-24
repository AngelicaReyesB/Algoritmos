package co.edu.uniquindio.ranking.algoritmos;

import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas comunes para los tres algoritmos de ordenamiento.
 *
 * Cada algoritmo tiene su propia clase de prueba que hereda de esta
 * y solo indica que algoritmo se prueba (metodo crearAlgoritmo).
 * Asi las mismas pruebas se aplican a Counting, Radix y Bucket Sort.
 */
abstract class PruebasAlgoritmoOrdenamiento {

    /** Cada clase hija devuelve el algoritmo que quiere probar. */
    protected abstract AlgoritmoOrdenamiento crearAlgoritmo();

    private static FrecuenciaTermino ft(String termino, int frecuencia) {
        return new FrecuenciaTermino(termino, frecuencia);
    }

    /**
     * Resultado esperado, calculado con List.sort de Java.
     * Solo se usa en las PRUEBAS como referencia: List.sort es estable,
     * asi que los empates conservan el orden de entrada.
     */
    private static List<FrecuenciaTermino> ordenEsperado(List<FrecuenciaTermino> terminos) {
        List<FrecuenciaTermino> copia = new ArrayList<>(terminos);
        copia.sort(Comparator.comparingInt(FrecuenciaTermino::frecuencia).reversed());
        return copia;
    }

    @Test
    void listaVaciaDevuelveListaVacia() {
        List<FrecuenciaTermino> resultado = crearAlgoritmo().ordenar(List.of());
        assertTrue(resultado.isEmpty());
    }

    @Test
    void unSoloTerminoQuedaIgual() {
        List<FrecuenciaTermino> entrada = List.of(ft("modelo", 5));
        assertEquals(entrada, crearAlgoritmo().ordenar(entrada));
    }

    @Test
    void ordenaDeMayorAMenorFrecuencia() {
        List<FrecuenciaTermino> entrada = List.of(
                ft("texto", 2), ft("modelo", 9), ft("imagen", 5), ft("datos", 1));
        List<FrecuenciaTermino> esperado = List.of(
                ft("modelo", 9), ft("imagen", 5), ft("texto", 2), ft("datos", 1));
        assertEquals(esperado, crearAlgoritmo().ordenar(entrada));
    }

    @Test
    void empatesConservanElOrdenDeEntrada() {
        // "b", "d" y "e" tienen frecuencia 3: deben salir en ese mismo orden
        List<FrecuenciaTermino> entrada = List.of(
                ft("a", 1), ft("b", 3), ft("c", 7), ft("d", 3), ft("e", 3));
        List<FrecuenciaTermino> esperado = List.of(
                ft("c", 7), ft("b", 3), ft("d", 3), ft("e", 3), ft("a", 1));
        assertEquals(esperado, crearAlgoritmo().ordenar(entrada));
    }

    @Test
    void todasLasFrecuenciasIgualesNoCambiaElOrden() {
        List<FrecuenciaTermino> entrada = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entrada.add(ft("t" + i, 4));
        }
        assertEquals(entrada, crearAlgoritmo().ordenar(entrada));
    }

    @Test
    void noModificaLaListaDeEntrada() {
        List<FrecuenciaTermino> entrada = new ArrayList<>(List.of(
                ft("a", 2), ft("b", 9), ft("c", 2)));
        List<FrecuenciaTermino> copia = new ArrayList<>(entrada);
        crearAlgoritmo().ordenar(entrada);
        assertEquals(copia, entrada);
    }

    @Test
    void coincideConLaReferenciaEnCasosAleatorios() {
        Random aleatorio = new Random(42); // semilla fija: siempre los mismos casos
        int[] frecuenciasMaximas = {3, 10, 100, 5000};
        for (int caso = 0; caso < 200; caso++) {
            int n = 1 + aleatorio.nextInt(300);
            int maxima = frecuenciasMaximas[aleatorio.nextInt(frecuenciasMaximas.length)];
            List<FrecuenciaTermino> entrada = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                entrada.add(ft("t" + i, 1 + aleatorio.nextInt(maxima)));
            }
            assertEquals(ordenEsperado(entrada), crearAlgoritmo().ordenar(entrada), "caso " + caso);
        }
    }
}