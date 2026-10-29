package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.modelo.Articulo;
import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Al iniciar la aplicacion carga el corpus en la base de datos:
 *
 * 1. Borra lo que habia, para que no queden articulos de un corpus anterior.
 * 2. Extrae el abstract de cada PDF de data/pdfs con PDFBox.
 *    El titulo y los autores salen de data/metadatos.json.
 * 3. Carga las correcciones manuales de data/abstracts (JSON con "manual": true).
 *
 * Implementa CommandLineRunner: Spring ejecuta el metodo run() una vez,
 * justo despues de arrancar.
 */
@Component
@Order(1)
public class CargaArticulos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaArticulos.class);

    private final LectorPdf lector;
    private final ExtractorAbstract extractor;
    private final RepositorioArticulos repositorio;
    private final ObjectMapper lectorJson;
    private final Path carpetaPdfs;
    private final Path carpetaAbstracts;
    private final Path archivoMetadatos;

    public CargaArticulos(LectorPdf lector,
                          ExtractorAbstract extractor,
                          RepositorioArticulos repositorio,
                          ObjectMapper lectorJson,
                          @Value("${ranking.carpeta-pdfs}") String carpetaPdfs,
                          @Value("${ranking.carpeta-abstracts}") String carpetaAbstracts,
                          @Value("${ranking.archivo-metadatos}") String archivoMetadatos) {
        this.lector = lector;
        this.extractor = extractor;
        this.repositorio = repositorio;
        this.lectorJson = lectorJson;
        this.carpetaPdfs = Path.of(carpetaPdfs);
        this.carpetaAbstracts = Path.of(carpetaAbstracts);
        this.archivoMetadatos = Path.of(archivoMetadatos);
    }

    @Override
    public void run(String... args) throws IOException {
        repositorio.borrarTodos();
        cargarPdfs();
        cargarCorreccionesManuales();
    }

    /** Paso 2: extraccion automatica desde los PDF. */
    public void cargarPdfs() throws IOException {
        Map<Integer, JsonNode> metadatos = leerMetadatos();
        List<Path> pdfs = listarArchivos(carpetaPdfs, ".pdf");

        int extraidos = 0;
        for (Path pdf : pdfs) {
            String nombre = pdf.getFileName().toString();
            String resumen = extractor.extraerAbstract(lector.leerPrimerasPaginas(pdf));
            if (resumen == null) {
                log.warn("Sin abstract detectado: {} (revisar manualmente)", nombre);
                continue;
            }

            JsonNode datos = metadatos.get(numeroDelArchivo(nombre));
            String titulo = nombre;
            String autores = null;
            if (datos == null) {
                log.warn("Sin metadatos en {}: {}", archivoMetadatos.getFileName(), nombre);
            } else {
                titulo = datos.path("titulo").asText();
                autores = datos.path("autores").asText();
            }
            repositorio.guardar(new Articulo(null, nombre, titulo, autores, resumen, false));
            extraidos++;
        }
        log.info("Abstracts extraidos: {}/{}", extraidos, pdfs.size());
    }

    /**
     * Lee data/metadatos.json: una lista de { "numero", "titulo", "autores" }.
     * Devuelve un mapa numero del articulo -> sus datos.
     */
    private Map<Integer, JsonNode> leerMetadatos() throws IOException {
        Map<Integer, JsonNode> metadatos = new HashMap<>();
        if (!Files.exists(archivoMetadatos)) {
            log.warn("No existe el archivo de metadatos: {}", archivoMetadatos.toAbsolutePath());
            return metadatos;
        }
        for (JsonNode articulo : lectorJson.readTree(archivoMetadatos.toFile())) {
            metadatos.put(articulo.path("numero").asInt(), articulo);
        }
        return metadatos;
    }

    /**
     * Paso 3: correcciones manuales.
     *
     * Cada JSON tiene la forma:
     *   { "file": "7. x.pdf", "title": "...", "manual": true, "abstract": "..." }
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

    /**
     * Numero al inicio del nombre del archivo: "12. A meta systematic review.pdf" -> 12.
     * Si el nombre no empieza con un numero devuelve -1.
     */
    static int numeroDelArchivo(String nombre) {
        int i = 0;
        while (i < nombre.length() && Character.isDigit(nombre.charAt(i))) {
            i++;
        }
        return i == 0 ? -1 : Integer.parseInt(nombre.substring(0, i));
    }

    /**
     * Archivos de una carpeta con cierta extension, ordenados por el numero
     * del inicio del nombre (1, 2, 3, ..., 10, 11) y luego por nombre.
     * Sin esto el orden alfabetico seria 1, 10, 11, ..., 2, 20, 3.
     */
    static List<Path> listarArchivos(Path carpeta, String extension) throws IOException {
        if (!Files.isDirectory(carpeta)) {
            log.warn("No existe la carpeta: {}", carpeta.toAbsolutePath());
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .filter(archivo -> archivo.toString().toLowerCase().endsWith(extension))
                    .sorted(Comparator
                            .comparingInt((Path archivo) -> numeroDelArchivo(archivo.getFileName().toString()))
                            .thenComparing(archivo -> archivo.getFileName().toString()))
                    .toList();
        }
    }
}