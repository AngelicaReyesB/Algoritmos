package co.edu.uniquindio.ranking.medicion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CorpusBusquedaTest {

    /** Texto de prueba: contiene "model" y no contiene "zqxjk" ni '#'. */
    private static final String TEXTO =
            "generative models learn patterns from data and large language models generate text ".repeat(40);

    @Test
    void creaLosCuatroEscenariosEnOrden() {
        Map<String, String> escenarios = CorpusBusqueda.crearEscenarios(TEXTO);

        assertEquals(List.of("corto", "largo", "ausente_corto", "ausente_largo"),
                List.copyOf(escenarios.keySet()));
    }

    @Test
    void elPatronLargoEsUnFragmentoDe40CaracteresDelTexto() {
        String largo = CorpusBusqueda.crearEscenarios(TEXTO).get(CorpusBusqueda.LARGO);

        assertEquals(40, largo.length());
    }

    @Test
    void elAusenteLargoSoloCambiaElUltimoCaracter() {
        Map<String, String> escenarios = CorpusBusqueda.crearEscenarios(TEXTO);
        String largo = escenarios.get(CorpusBusqueda.LARGO);
        String ausenteLargo = escenarios.get(CorpusBusqueda.AUSENTE_LARGO);

        assertEquals(largo.length(), ausenteLargo.length());
        assertEquals(largo.substring(0, 39), ausenteLargo.substring(0, 39));
        assertEquals('#', ausenteLargo.charAt(39));
    }

    @Test
    void rechazaUnTextoDondeNoApareceElPatronCorto() {
        // Sin la palabra "model" el escenario "corto" no seria un patron presente
        assertThrows(IllegalStateException.class, () -> CorpusBusqueda.crearEscenarios("x".repeat(200)));
    }

    @Test
    void rechazaUnTextoMuyCorto() {
        assertThrows(IllegalArgumentException.class, () -> CorpusBusqueda.crearEscenarios("model"));
    }

    @Test
    void cargarTextoUneLosAbstractsEnOrdenDeArchivoEIgnoraLoDemas() throws IOException {
        Path carpeta = Files.createTempDirectory("abstracts");
        try {
            Files.writeString(carpeta.resolve("02_b.json"), "{\"archivo\":\"b.pdf\",\"abstract\":\"segundo texto\"}");
            Files.writeString(carpeta.resolve("01_a.json"), "{\"archivo\":\"a.pdf\",\"abstract\":\"primer texto\"}");
            Files.writeString(carpeta.resolve("nota.txt"), "esto no es un abstract");

            assertEquals("primer texto segundo texto", CorpusBusqueda.cargarTexto(carpeta));
        } finally {
            try (var archivos = Files.list(carpeta)) {
                for (Path archivo : archivos.toList()) {
                    Files.delete(archivo);
                }
            }
            Files.delete(carpeta);
        }
    }
}
