package co.edu.uniquindio.ranking.medicion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicionBusquedaTest {

    private static final String TEXTO =
            "generative models learn patterns from data and large language models generate text ".repeat(40);

    private static final List<String> ALGORITMOS = List.of("KMP", "Boyer-Moore", "Rabin-Karp");
    private static final List<String> ESCENARIOS = List.of("corto", "largo", "ausente_corto", "ausente_largo");

    /** Pocas repeticiones: aqui se prueba la estructura, no los tiempos. */
    private final MedicionBusqueda medicion = new MedicionBusqueda(new MedicionManual(), 2, 5);

    @Test
    void midePreprocesamientoYBusquedaDeCadaAlgoritmoEnCadaEscenario() {
        List<MedicionBusqueda.Fila> filas = medicion.medir(TEXTO);

        assertEquals(24, filas.size()); // 3 algoritmos x 4 escenarios x 2 etapas
        for (String algoritmo : ALGORITMOS) {
            for (String escenario : ESCENARIOS) {
                assertEquals(1, contar(filas, algoritmo, escenario, MedicionBusqueda.PREPROCESAMIENTO));
                assertEquals(1, contar(filas, algoritmo, escenario, MedicionBusqueda.BUSQUEDA));
            }
        }
    }

    @Test
    void losTiemposSonCoherentes() {
        for (MedicionBusqueda.Fila fila : medicion.medir(TEXTO)) {
            ResultadoMedicion tiempo = fila.medicion();
            assertEquals(5, tiempo.repeticiones());
            assertTrue(tiempo.minimoMs() >= 0, "minimo negativo en " + fila);
            assertTrue(tiempo.minimoMs() <= tiempo.mediaMs(), "minimo mayor que la media en " + fila);
            assertTrue(tiempo.desviacionMs() >= 0, "desviacion negativa en " + fila);
        }
    }

    @Test
    void lasOcurrenciasSeReportanSoloEnLaBusquedaYLasColisionesSoloEnRabinKarp() {
        for (MedicionBusqueda.Fila fila : medicion.medir(TEXTO)) {
            if (fila.etapa().equals(MedicionBusqueda.PREPROCESAMIENTO)) {
                assertNull(fila.ocurrencias());
                assertNull(fila.colisiones());
            } else {
                assertNotNull(fila.ocurrencias());
                if (fila.algoritmo().equals("Rabin-Karp")) {
                    assertNotNull(fila.colisiones());
                } else {
                    assertNull(fila.colisiones());
                }
            }
        }
    }

    @Test
    void losPatronesAusentesNoTienenOcurrenciasYLosPresentesSi() {
        for (MedicionBusqueda.Fila fila : medicion.medir(TEXTO)) {
            if (!fila.etapa().equals(MedicionBusqueda.BUSQUEDA)) {
                continue;
            }
            if (fila.escenario().startsWith("ausente")) {
                assertEquals(0, fila.ocurrencias(), fila.toString());
            } else {
                assertTrue(fila.ocurrencias() > 0, fila.toString());
            }
        }
    }

    @Test
    void losTresAlgoritmosEncuentranLoMismoEnCadaEscenario() {
        List<MedicionBusqueda.Fila> filas = medicion.medir(TEXTO);
        for (String escenario : ESCENARIOS) {
            Integer referencia = ocurrencias(filas, "KMP", escenario);
            assertEquals(referencia, ocurrencias(filas, "Boyer-Moore", escenario));
            assertEquals(referencia, ocurrencias(filas, "Rabin-Karp", escenario));
        }
    }

    @Test
    void escribeElCsvConEncabezadoYUnaLineaPorFila() throws IOException {
        Path archivo = Files.createTempFile("busqueda", ".csv");
        try {
            MedicionBusqueda.escribirCsv(medicion.medir(TEXTO), archivo);
            List<String> lineas = Files.readAllLines(archivo);

            assertEquals(25, lineas.size()); // encabezado + 24 filas
            assertEquals("algoritmo,escenario,longitud_patron,longitud_texto,etapa,repeticiones,"
                    + "media_ms,desviacion_ms,minimo_ms,ocurrencias,colisiones", lineas.get(0));
            for (String linea : lineas) {
                assertEquals(11, linea.split(",", -1).length, linea);
            }
            // Los decimales usan punto, sin importar el idioma del equipo
            double media = Double.parseDouble(lineas.get(1).split(",", -1)[6]);
            assertTrue(media >= 0);
        } finally {
            Files.delete(archivo);
        }
    }

    // ---------- Apoyo ----------

    private long contar(List<MedicionBusqueda.Fila> filas, String algoritmo, String escenario, String etapa) {
        return filas.stream()
                .filter(f -> f.algoritmo().equals(algoritmo) && f.escenario().equals(escenario)
                        && f.etapa().equals(etapa))
                .count();
    }

    private Integer ocurrencias(List<MedicionBusqueda.Fila> filas, String algoritmo, String escenario) {
        return filas.stream()
                .filter(f -> f.algoritmo().equals(algoritmo) && f.escenario().equals(escenario)
                        && f.etapa().equals(MedicionBusqueda.BUSQUEDA))
                .findFirst().orElseThrow().ocurrencias();
    }
}