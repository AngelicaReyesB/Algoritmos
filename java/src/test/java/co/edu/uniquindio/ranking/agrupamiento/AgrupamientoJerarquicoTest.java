package co.edu.uniquindio.ranking.agrupamiento;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas con 4 puntos sobre una recta, para poder calcular a mano:
 *
 *   punto:      0    1         2                   3
 *   posicion:   0    1         4                  10
 *
 *   distancias:  d(0,1) = 1   d(0,2) = 4   d(0,3) = 10
 *                d(1,2) = 3   d(1,3) = 9   d(2,3) = 6
 *
 * Los tres criterios unen primero 0 y 1 (distancia 1) en el grupo 4.
 * Despues cambian las alturas:
 *
 *               grupo {0,1} con 2               grupo {0,1,2} con 3
 *   SINGLE      min(4, 3)     = 3               min(10, 9, 6)     = 6
 *   COMPLETE    max(4, 3)     = 4               max(10, 9, 6)     = 10
 *   AVERAGE     (4 + 3) / 2   = 3.5             (10 + 9 + 6) / 3  = 8.33
 */
class AgrupamientoJerarquicoTest {

    private static final double[][] DISTANCIAS = {
            {0, 1, 4, 10},
            {1, 0, 3, 9},
            {4, 3, 0, 6},
            {10, 9, 6, 0},
    };

    @Test
    void singleLinkage() {
        List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, Criterio.SINGLE);
        assertEquals(List.of(
                new Union(0, 1, 4, 1.0, 2),
                new Union(2, 4, 5, 3.0, 3),
                new Union(3, 5, 6, 6.0, 4)), uniones);
    }

    @Test
    void completeLinkage() {
        List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, Criterio.COMPLETE);
        assertEquals(List.of(
                new Union(0, 1, 4, 1.0, 2),
                new Union(2, 4, 5, 4.0, 3),
                new Union(3, 5, 6, 10.0, 4)), uniones);
    }

    @Test
    void averageLinkage() {
        List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, Criterio.AVERAGE);
        assertEquals(1.0, uniones.get(0).altura(), 1e-9);
        assertEquals(3.5, uniones.get(1).altura(), 1e-9);
        assertEquals(25.0 / 3, uniones.get(2).altura(), 1e-9);
    }

    @Test
    void conNArticulosHayNMenosUnoUniones() {
        for (Criterio criterio : Criterio.values()) {
            assertEquals(3, AgrupamientoJerarquico.agrupar(DISTANCIAS, criterio).size());
        }
    }

    @Test
    void laUltimaUnionContieneTodosLosArticulos() {
        for (Criterio criterio : Criterio.values()) {
            List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, criterio);
            assertEquals(4, uniones.get(uniones.size() - 1).tamano());
        }
    }

    @Test
    void lasAlturasNuncaBajan() {
        // En los tres criterios cada union ocurre a una altura igual o mayor que la anterior
        for (Criterio criterio : Criterio.values()) {
            List<Union> uniones = AgrupamientoJerarquico.agrupar(DISTANCIAS, criterio);
            for (int k = 1; k < uniones.size(); k++) {
                assertTrue(uniones.get(k).altura() >= uniones.get(k - 1).altura(),
                        criterio + ": la union " + k + " baja de altura");
            }
        }
    }

    @Test
    void conDosArticulosHayUnaSolaUnion() {
        double[][] dos = {{0, 0.7}, {0.7, 0}};
        assertEquals(List.of(new Union(0, 1, 2, 0.7, 2)), AgrupamientoJerarquico.agrupar(dos, Criterio.SINGLE));
    }
}