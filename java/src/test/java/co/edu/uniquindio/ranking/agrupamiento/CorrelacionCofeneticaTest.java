package co.edu.uniquindio.ranking.agrupamiento;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mismo ejemplo de AgrupamientoJerarquicoTest: 4 puntos en las posiciones 0, 1, 4 y 10.
 *
 *   Pares (i, j):         (0,1) (0,2) (0,3) (1,2) (1,3) (2,3)
 *   Distancia original:     1     4    10     3     9     6
 *   Cofenetica SINGLE:      1     3     6     3     6     6
 *   Cofenetica COMPLETE:    1     4    10     4    10    10
 *   Cofenetica AVERAGE:     1    3.5  8.33   3.5  8.33  8.33
 *
 * Los valores esperados de Pearson se verificaron con numpy (corrcoef).
 */
class CorrelacionCofeneticaTest {

    private static final double[][] DISTANCIAS = {
            {0, 1, 4, 10},
            {1, 0, 3, 9},
            {4, 3, 0, 6},
            {10, 9, 6, 0},
    };

    @Test
    void matrizCofeneticaDeSingle() {
        List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, Criterio.SINGLE);
        double[][] cofenetica = CorrelacionCofenetica.matrizCofenetica(uniones, 4);
        assertArrayEquals(new double[]{0, 1, 3, 6}, cofenetica[0], 1e-9);
        assertArrayEquals(new double[]{1, 0, 3, 6}, cofenetica[1], 1e-9);
        assertArrayEquals(new double[]{3, 3, 0, 6}, cofenetica[2], 1e-9);
        assertArrayEquals(new double[]{6, 6, 6, 0}, cofenetica[3], 1e-9);
    }

    @Test
    void pearsonDeListasProporcionalesEsUno() {
        assertEquals(1.0, CorrelacionCofenetica.pearson(List.of(1.0, 2.0, 3.0), List.of(2.0, 4.0, 6.0)), 1e-9);
    }

    @Test
    void pearsonDeListasOpuestasEsMenosUno() {
        assertEquals(-1.0, CorrelacionCofenetica.pearson(List.of(1.0, 2.0, 3.0), List.of(3.0, 2.0, 1.0)), 1e-9);
    }

    @Test
    void pearsonSinVariacionDevuelveCero() {
        assertEquals(0.0, CorrelacionCofenetica.pearson(List.of(1.0, 2.0, 3.0), List.of(5.0, 5.0, 5.0)), 1e-9);
    }

    @Test
    void correlacionDeLosTresCriterios() {
        assertEquals(0.920655, calcular(Criterio.SINGLE), 1e-6);
        assertEquals(0.922441, calcular(Criterio.COMPLETE), 1e-6);
        assertEquals(0.922469, calcular(Criterio.AVERAGE), 1e-6);
    }

    @Test
    void enEsteEjemploAverageEsElMasCoherente() {
        assertTrue(calcular(Criterio.AVERAGE) > calcular(Criterio.COMPLETE));
        assertTrue(calcular(Criterio.COMPLETE) > calcular(Criterio.SINGLE));
    }

    private static double calcular(Criterio criterio) {
        return CorrelacionCofenetica.calcular(DISTANCIAS, AgrupamientoJerarquico.agrupar(DISTANCIAS, criterio));
    }
}