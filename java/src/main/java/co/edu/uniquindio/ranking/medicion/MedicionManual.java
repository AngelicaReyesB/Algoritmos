package co.edu.uniquindio.ranking.medicion;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Mide el tiempo de una tarea con System.nanoTime() (version sin librerias).
 *
 * Procedimiento:
 * 1. Calentamiento: ejecuta la tarea varias veces SIN medir. La maquina
 *    virtual de Java compila y optimiza el codigo mientras se usa (JIT),
 *    asi que las primeras ejecuciones son mas lentas y no son representativas.
 * 2. Medicion: ejecuta la tarea "repeticiones" veces y mide cada una.
 * 3. Calcula media, desviacion estandar y minimo.
 */
@Component
public class MedicionManual {

    private static final double NANOS_POR_MILISEGUNDO = 1_000_000.0;

    /**
     * Guarda el resultado de la ultima ejecucion. Si el resultado de la tarea
     * no se usara para nada, el compilador JIT podria eliminar el calculo
     * completo y mediriamos cero. "volatile" obliga a escribirlo de verdad.
     * (JMH resuelve este mismo problema con su clase Blackhole.)
     */
    private volatile Object sumidero;

    public ResultadoMedicion medir(String etapa, int calentamiento, int repeticiones, Supplier<?> tarea) {
        // Paso 1: calentamiento, sin medir
        for (int i = 0; i < calentamiento; i++) {
            sumidero = tarea.get();
        }

        // Paso 2: medir cada repeticion
        double[] tiemposMs = new double[repeticiones];
        for (int i = 0; i < repeticiones; i++) {
            long inicio = System.nanoTime();
            sumidero = tarea.get();
            long fin = System.nanoTime();
            tiemposMs[i] = (fin - inicio) / NANOS_POR_MILISEGUNDO;
        }

        // Paso 3: estadisticas
        double suma = 0;
        double minimo = Double.MAX_VALUE;
        for (double tiempo : tiemposMs) {
            suma += tiempo;
            if (tiempo < minimo) {
                minimo = tiempo;
            }
        }
        double media = suma / repeticiones;

        // Desviacion estandar: raiz del promedio de (tiempo - media)^2
        double sumaCuadrados = 0;
        for (double tiempo : tiemposMs) {
            sumaCuadrados += (tiempo - media) * (tiempo - media);
        }
        double desviacion = Math.sqrt(sumaCuadrados / repeticiones);

        return new ResultadoMedicion(etapa, repeticiones, media, desviacion, minimo);
    }
}