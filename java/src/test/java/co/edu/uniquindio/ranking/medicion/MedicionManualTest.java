package co.edu.uniquindio.ranking.medicion;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicionManualTest {

    private final MedicionManual medicion = new MedicionManual();

    @Test
    void ejecutaLaTareaCalentamientoMasRepeticionesVeces() {
        AtomicInteger ejecuciones = new AtomicInteger();
        medicion.medir("prueba", 3, 10, ejecuciones::incrementAndGet);
        assertEquals(13, ejecuciones.get());
    }

    @Test
    void lasEstadisticasSonCoherentes() {
        ResultadoMedicion resultado = medicion.medir("suma", 5, 50, () -> {
            long suma = 0;
            for (int i = 0; i < 10_000; i++) {
                suma += i;
            }
            return suma;
        });
        assertEquals("suma", resultado.etapa());
        assertEquals(50, resultado.repeticiones());
        assertTrue(resultado.minimoMs() >= 0);
        assertTrue(resultado.minimoMs() <= resultado.mediaMs());
        assertTrue(resultado.desviacionMs() >= 0);
    }
}