package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.algoritmos.AlgoritmoOrdenamiento;
import co.edu.uniquindio.ranking.algoritmos.BucketSort;
import co.edu.uniquindio.ranking.algoritmos.CountingSort;
import co.edu.uniquindio.ranking.algoritmos.RadixSort;
import co.edu.uniquindio.ranking.medicion.InformacionEquipo;
import co.edu.uniquindio.ranking.medicion.MedicionManual;
import co.edu.uniquindio.ranking.medicion.ReporteMedicion;
import co.edu.uniquindio.ranking.medicion.ResultadoMedicion;
import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;
import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import co.edu.uniquindio.ranking.texto.ContadorFrecuencias;
import co.edu.uniquindio.ranking.texto.TokenizadorLucene;
import co.edu.uniquindio.ranking.texto.TokenizadorPropio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mide el tiempo de cada etapa del proceso con System.nanoTime()
 * (version sin librerias) y valida el Top 15 de los tres algoritmos.
 *
 * Se ejecuta al iniciar SOLO si la propiedad ranking.medicion.ejecutar=true,
 * porque tarda varios segundos. @Order(2): corre despues de CargaArticulos,
 * cuando los abstracts ya estan en la base de datos.
 *
 * Resultados en la carpeta ranking.carpeta-resultados:
 *   tiempos.csv  equipo.txt  top15.csv
 */
@Component
@Order(2)
public class ServicioMedicion implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ServicioMedicion.class);

    // Calentamiento y repeticiones por etapa. Las etapas lentas (leer PDF)
    // se repiten pocas veces; las rapidas (ordenar) muchas, para reducir el ruido.
    private static final int CALENTAMIENTO_PDF = 1;
    private static final int REPETICIONES_PDF = 5;
    private static final int CALENTAMIENTO_BD = 5;
    private static final int REPETICIONES_BD = 50;
    private static final int CALENTAMIENTO_TEXTO = 20;
    private static final int REPETICIONES_TEXTO = 200;
    private static final int CALENTAMIENTO_ORDEN = 100;
    private static final int REPETICIONES_ORDEN = 1000;

    private static final int TOP = 15;

    private final MedicionManual medicion;
    private final LectorPdf lector;
    private final ExtractorAbstract extractor;
    private final RepositorioArticulos repositorio;
    private final Path carpetaPdfs;
    private final Path carpetaResultados;
    private final boolean ejecutar;

    public ServicioMedicion(MedicionManual medicion,
                            LectorPdf lector,
                            ExtractorAbstract extractor,
                            RepositorioArticulos repositorio,
                            @Value("${ranking.carpeta-pdfs}") String carpetaPdfs,
                            @Value("${ranking.carpeta-resultados}") String carpetaResultados,
                            @Value("${ranking.medicion.ejecutar:false}") boolean ejecutar) {
        this.medicion = medicion;
        this.lector = lector;
        this.extractor = extractor;
        this.repositorio = repositorio;
        this.carpetaPdfs = Path.of(carpetaPdfs);
        this.carpetaResultados = Path.of(carpetaResultados);
        this.ejecutar = ejecutar;
    }

    @Override
    public void run(String... args) throws IOException {
        if (!ejecutar) {
            log.info("Medicion desactivada (ranking.medicion.ejecutar=false)");
            return;
        }
        medirTodo();
    }

    /**
     * synchronized: si llegan dos peticiones a la vez, la segunda espera.
     * Dos mediciones simultaneas se estorbarian y los tiempos no servirian.
     */
    public synchronized ReporteMedicion medirTodo() throws IOException {
        List<ResultadoMedicion> resultados = new ArrayList<>();

        // Etapa 1: extraccion de abstracts desde los PDF (lectura PDFBox + extraccion)
        List<Path> pdfs = CargaArticulos.listarArchivos(carpetaPdfs, ".pdf");
        resultados.add(medicion.medir("Extraccion PDF (PDFBox)",
                CALENTAMIENTO_PDF, REPETICIONES_PDF, () -> extraerTodos(pdfs)));

        // Etapa 2: lectura de los abstracts desde la base de datos
        resultados.add(medicion.medir("Lectura de abstracts (BD)",
                CALENTAMIENTO_BD, REPETICIONES_BD, repositorio::listarResumenes));
        List<String> textos = repositorio.listarResumenes();

        // Etapa 3: preprocesamiento (tokenizar todos los abstracts)
        // 3a. Tokenizador propio (version sin librerias)
        ContadorFrecuencias contador = new ContadorFrecuencias(new TokenizadorPropio());
        resultados.add(medicion.medir("Preprocesamiento (propio)",
                CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO, () -> contador.tokenizarTodos(textos)));
        List<String> terminos = contador.tokenizarTodos(textos);

        // 3b. Tokenizador con Lucene (version con librerias), sobre los mismos textos
        ContadorFrecuencias contadorLucene = new ContadorFrecuencias(new TokenizadorLucene());
        resultados.add(medicion.medir("Preprocesamiento (Lucene)",
                CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO, () -> contadorLucene.tokenizarTodos(textos)));
        int terminosDistintosLucene = contadorLucene.contar(textos).size();

        // Etapa 4: conteo de frecuencias
        resultados.add(medicion.medir("Conteo de frecuencias",
                CALENTAMIENTO_TEXTO, REPETICIONES_TEXTO, () -> contador.contarTerminos(terminos)));
        List<FrecuenciaTermino> frecuencias = contador.contarTerminos(terminos);

        // Etapa 5: los tres algoritmos, sobre la MISMA lista de frecuencias
        List<AlgoritmoOrdenamiento> algoritmos = List.of(new CountingSort(), new RadixSort(), new BucketSort());
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            resultados.add(medicion.medir(algoritmo.getNombre(),
                    CALENTAMIENTO_ORDEN, REPETICIONES_ORDEN, () -> algoritmo.ordenar(frecuencias)));
        }

        // Validacion: el Top 15 de los tres algoritmos debe ser identico
        List<List<FrecuenciaTermino>> rankings = new ArrayList<>();
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            rankings.add(algoritmo.ordenar(frecuencias));
        }
        boolean coinciden = rankings.get(0).equals(rankings.get(1)) && rankings.get(0).equals(rankings.get(2));

        int frecuenciaMaxima = 0;
        for (FrecuenciaTermino f : frecuencias) {
            frecuenciaMaxima = Math.max(frecuenciaMaxima, f.frecuencia());
        }

        ReporteMedicion reporte = new ReporteMedicion(resultados, terminos.size(), frecuencias.size(),
                terminosDistintosLucene, frecuenciaMaxima, coinciden, InformacionEquipo.obtener());
        imprimir(reporte);
        guardar(reporte, algoritmos, rankings);
        return reporte;
    }

    /** Lee y extrae todos los PDF. Es la tarea que se mide en la etapa 1. */
    private List<String> extraerTodos(List<Path> pdfs) {
        List<String> resumenes = new ArrayList<>();
        for (Path pdf : pdfs) {
            try {
                resumenes.add(extractor.extraerAbstract(lector.leerPrimerasPaginas(pdf)));
            } catch (IOException e) {
                // Un Supplier no puede lanzar IOException; se envuelve en una excepcion no verificada
                throw new UncheckedIOException(e);
            }
        }
        return resumenes;
    }

    private void imprimir(ReporteMedicion reporte) {
        log.info("========== MEDICION DE TIEMPOS (ms) ==========");
        log.info(String.format(Locale.ROOT, "%-28s %6s %10s %10s %10s", "Etapa", "Rep.", "Media", "Desv.", "Minimo"));
        for (ResultadoMedicion r : reporte.resultados()) {
            log.info(String.format(Locale.ROOT, "%-28s %6d %10.4f %10.4f %10.4f",
                    r.etapa(), r.repeticiones(), r.mediaMs(), r.desviacionMs(), r.minimoMs()));
        }
        log.info("Terminos totales: {} | Terminos distintos (n): {} | Frecuencia maxima (k): {}",
                reporte.terminosTotales(), reporte.terminosDistintos(), reporte.frecuenciaMaxima());
        log.info("Terminos distintos con Lucene: {}", reporte.terminosDistintosLucene());
        log.info("Los 3 algoritmos producen el mismo ranking: {}", reporte.coinciden() ? "SI" : "NO <-- revisar");
        log.info("Equipo: {}", reporte.equipo());
        log.info("Resultados guardados en {}", carpetaResultados.toAbsolutePath().normalize());
    }

    private void guardar(ReporteMedicion reporte, List<AlgoritmoOrdenamiento> algoritmos,
                         List<List<FrecuenciaTermino>> rankings) throws IOException {
        Files.createDirectories(carpetaResultados);
        InformacionEquipo equipo = reporte.equipo();
        int n = reporte.terminosDistintos();

        // tiempos.csv
        List<String> lineas = new ArrayList<>();
        lineas.add("etapa,repeticiones,media_ms,desviacion_ms,minimo_ms");
        for (ResultadoMedicion r : reporte.resultados()) {
            lineas.add(String.format(Locale.ROOT, "%s,%d,%.4f,%.4f,%.4f",
                    r.etapa(), r.repeticiones(), r.mediaMs(), r.desviacionMs(), r.minimoMs()));
        }
        Files.write(carpetaResultados.resolve("tiempos.csv"), lineas);

        // top15.csv: una columna de termino y una de frecuencia por algoritmo
        List<String> top = new ArrayList<>();
        StringBuilder encabezado = new StringBuilder("posicion");
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            encabezado.append(',').append(algoritmo.getNombre()).append(" - termino")
                    .append(',').append(algoritmo.getNombre()).append(" - frecuencia");
        }
        top.add(encabezado.toString());
        for (int i = 0; i < Math.min(TOP, n); i++) {
            StringBuilder fila = new StringBuilder().append(i + 1);
            for (List<FrecuenciaTermino> ranking : rankings) {
                fila.append(',').append(ranking.get(i).termino())
                        .append(',').append(ranking.get(i).frecuencia());
            }
            top.add(fila.toString());
        }
        Files.write(carpetaResultados.resolve("top15.csv"), top);

        // equipo.txt
        Files.write(carpetaResultados.resolve("equipo.txt"), List.of(
                "Procesador: " + equipo.procesador(),
                "Nucleos logicos: " + equipo.nucleos(),
                "Memoria RAM total (MB): " + equipo.memoriaTotalMb(),
                "Sistema operativo: " + equipo.sistemaOperativo(),
                "Java: " + equipo.versionJava(),
                "Terminos totales: " + reporte.terminosTotales(),
                "Terminos distintos (n): " + n,
                "Terminos distintos con Lucene: " + reporte.terminosDistintosLucene(),
                "Frecuencia maxima (k): " + reporte.frecuenciaMaxima()));
    }
}