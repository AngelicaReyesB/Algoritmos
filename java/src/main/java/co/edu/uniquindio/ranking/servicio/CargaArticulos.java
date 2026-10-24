package co.edu.uniquindio.ranking.servicio;

import co.edu.uniquindio.ranking.modelo.Articulo;
import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import co.edu.uniquindio.ranking.repositorio.RepositorioArticulos;
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
 * Al iniciar la aplicacion, lee los PDF de data/pdfs, extrae su abstract
 * y lo guarda en la base de datos.
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
    private final Path carpetaPdfs;

    public CargaArticulos(LectorPdf lector,
                          ExtractorAbstract extractor,
                          RepositorioArticulos repositorio,
                          @Value("${ranking.carpeta-pdfs}") String carpetaPdfs) {
        this.lector = lector;
        this.extractor = extractor;
        this.repositorio = repositorio;
        this.carpetaPdfs = Path.of(carpetaPdfs);
    }

    @Override
    public void run(String... args) throws IOException {
        cargarPdfs();
    }

    public void cargarPdfs() throws IOException {
        if (!Files.isDirectory(carpetaPdfs)) {
            log.warn("No existe la carpeta de PDFs: {}", carpetaPdfs.toAbsolutePath());
            return;
        }

        // Lista de archivos .pdf de la carpeta, ordenados por nombre
        List<Path> pdfs;
        try (Stream<Path> archivos = Files.list(carpetaPdfs)) {
            pdfs = archivos
                    .filter(archivo -> archivo.toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .toList();
        }

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
}