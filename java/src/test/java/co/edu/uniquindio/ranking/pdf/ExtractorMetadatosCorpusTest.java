package co.edu.uniquindio.ranking.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compara lo que se extrae de los 20 PDF reales con data/metadatos.json,
 * que tiene los titulos y autores verificados a mano.
 *
 * Los PDF no se suben al repositorio: si la carpeta no existe, la prueba
 * se salta en vez de fallar.
 */
class ExtractorMetadatosCorpusTest {

    private static final Path CARPETA_PDFS = Path.of("../data/pdfs");
    private static final Path ARCHIVO_METADATOS = Path.of("../data/metadatos.json");

    private final LectorPdf lector = new LectorPdf();
    private final ExtractorMetadatos extractor = new ExtractorMetadatos();

    @Test
    void extraeTitulosYAutoresIgualesALosVerificadosAMano() throws IOException {
        assumeTrue(Files.isDirectory(CARPETA_PDFS), "No existe la carpeta data/pdfs");

        // Se guardan todos los errores para verlos de una vez, no solo el primero
        List<String> errores = new ArrayList<>();
        for (JsonNode esperado : new ObjectMapper().readTree(ARCHIVO_METADATOS.toFile())) {
            int numero = esperado.path("numero").asInt();
            Path pdf = buscarPdf(numero);
            if (pdf == null) {
                errores.add(numero + ": no existe el PDF");
                continue;
            }

            ExtractorMetadatos.Metadatos extraido = extractor.extraer(lector.leerPortada(pdf));
            if (!esperado.path("titulo").asText().equals(extraido.titulo())) {
                errores.add(numero + " titulo: " + extraido.titulo());
            }
            if (!esperado.path("autores").asText().equals(extraido.autores())) {
                errores.add(numero + " autores: " + extraido.autores());
            }
        }
        assertEquals(List.of(), errores);
    }

    /** El PDF cuyo nombre empieza con "numero. ", por ejemplo "7. Human-centred....pdf". */
    private static Path buscarPdf(int numero) throws IOException {
        try (Stream<Path> archivos = Files.list(CARPETA_PDFS)) {
            return archivos
                    .filter(archivo -> archivo.getFileName().toString().startsWith(numero + ". "))
                    .findFirst()
                    .orElse(null);
        }
    }
}