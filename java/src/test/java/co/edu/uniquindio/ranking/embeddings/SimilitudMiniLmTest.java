package co.edu.uniquindio.ranking.embeddings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprueba que, con los vectores ya guardados en el archivo, SimilitudMiniLm
 * NO necesita cargar el modelo (si lo intentara, esta prueba tardaria varios
 * segundos y gastaria cientos de MB).
 */
class SimilitudMiniLmArchivoTest {

    @TempDir
    Path carpeta;

    @Test
    void usaLosVectoresDelArchivoSinCargarElModelo() throws Exception {
        Path archivo = carpeta.resolve("minilm-vectores.json");
        Map<String, double[]> vectores = Map.of(
                SimilitudMiniLm.huella("texto uno"), new double[]{1, 0},
                SimilitudMiniLm.huella("texto dos"), new double[]{1, 0},
                SimilitudMiniLm.huella("texto tres"), new double[]{0, 1});
        new ObjectMapper().writeValue(archivo.toFile(), vectores);

        SimilitudMiniLm similitud = new SimilitudMiniLm(archivo);

        assertEquals(1.0, similitud.similitud("texto uno", "texto dos"), 1e-12);
        assertEquals(0.0, similitud.similitud("texto uno", "texto tres"), 1e-12);
    }

    @Test
    void laHuellaEsEstableYDependeDelTexto() {
        assertEquals(SimilitudMiniLm.huella("abc"), SimilitudMiniLm.huella("abc"));
        assertEquals(64, SimilitudMiniLm.huella("abc").length());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                SimilitudMiniLm.huella("abc"), SimilitudMiniLm.huella("abd"));
    }
}
