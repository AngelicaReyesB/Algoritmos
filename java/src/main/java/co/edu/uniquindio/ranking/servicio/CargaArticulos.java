package co.edu.uniquindio.ranking.servicio;

import co.edu.uniquindio.ranking.modelo.Articulo;
import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import co.edu.uniquindio.ranking.repositorio.RepositorioArticulos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Al iniciar la aplicacion carga el corpus en la base de datos, en dos pasos:
 *
 * 1. Extrae el abstract de cada PDF de data/pdfs con PDFBox.
 * 2. Carga las correcciones manuales de data/abstracts (JSON con "manual": true).
 *
 * El orden importa: las correcciones van despues, y el repositorio no deja
 * que una extraccion automatica sobrescriba una correccion manual.
 *
 * Implementa CommandLineRunner: Spring ejecuta el metodo run() una vez,
 * justo despues de arrancar.
 */
@Component
public class CargaArticulos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaArticulos.class);

    private final LectorPdf lector;
    private final ExtractorAbstract extractor;
    private final RepositorioArticulos repositorio;
    private final ObjectMapper lectorJson;
    private final Path carpetaPdfs;
    private final Path carpetaAbstracts;

    public CargaArticulos(LectorPdf lector,
                          ExtractorAbstract extractor,
                          RepositorioArticulos repositorio,
                          ObjectMapper lectorJson,
                          @Value("${ranking.carpeta-pdfs}") String carpetaPdfs,
                          @Value("${ranking.carpeta-abstracts}") String carpetaAbstracts) {
        this.lector = lector;
        this.extractor = extractor;
        this.repositorio = repositorio;
        this.lectorJson = lectorJson;
        this.carpetaPdfs = Path.of(carpetaPdfs);
        this.carpetaAbstracts = Path.of(carpetaAbstracts);
    }

    @Override
    public void run(String... args) throws IOException {
        cargarPdfs();
        cargarCorreccionesManuales();
    }

    /** Paso 1: extraccion automatica desde los PDF. */
    public void cargarPdfs() throws IOException {
        List<Path> pdfs = listarArchivos(carpetaPdfs, ".pdf");

        int extraidos = 0;
        for (Path pdf : pdfs) {
            String nombre = pdf.getFileName().toString();
            String texto = lector.leerPrimerasPaginas(pdf);
            String resumen = extractor.extraerAbstract(texto);
            if (resumen == null) {
                log.warn("Sin abstract detectado: {} (revisar manualmente)", nombre);
                continue;
            }
            String titulo = extractor.extraerTitulo(texto);
            repositorio.guardar(new Articulo(null, nombre, titulo, null, resumen, false));
            extraidos++;
        }
        log.info("Abstracts extraidos: {}/{}", extraidos, pdfs.size());
    }

    /**
     * Paso 2: correcciones manuales.
     *
     * Cada JSON tiene la forma:
     *   { "file": "20_x.pdf", "title": "...", "manual": true, "abstract": "..." }
     * Solo se cargan los que tienen "manual": true. Los demas son la
     * extraccion automatica de la version Python y no hacen falta aqui.
     */
    public void cargarCorreccionesManuales() throws IOException {
        int cargados = 0;
        for (Path archivo : listarArchivos(carpetaAbstracts, ".json")) {
            JsonNode json = lectorJson.readTree(archivo.toFile());
            if (!json.path("manual").asBoolean(false)) {
                continue;
            }
            String nombrePdf = json.path("file").asText();
            String titulo = json.path("title").asText();
            String resumen = json.path("abstract").asText();
            repositorio.guardar(new Articulo(null, nombrePdf, titulo, null, resumen, true));
            log.info("Correccion manual cargada: {}", nombrePdf);
            cargados++;
        }
        log.info("Correcciones manuales cargadas: {}", cargados);
    }

    /** Archivos de una carpeta con cierta extension, ordenados por nombre. */
    private List<Path> listarArchivos(Path carpeta, String extension) throws IOException {
        if (!Files.isDirectory(carpeta)) {
            log.warn("No existe la carpeta: {}", carpeta.toAbsolutePath());
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .filter(archivo -> archivo.toString().toLowerCase().endsWith(extension))
                    .sorted()
                    .toList();
        }
    }
}