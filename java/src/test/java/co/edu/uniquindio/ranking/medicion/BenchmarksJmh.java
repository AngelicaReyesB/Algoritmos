package co.edu.uniquindio.ranking.medicion;

import co.edu.uniquindio.ranking.algoritmos.BucketSort;
import co.edu.uniquindio.ranking.algoritmos.CountingSort;
import co.edu.uniquindio.ranking.algoritmos.RadixSort;
import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;
import co.edu.uniquindio.ranking.texto.ContadorFrecuencias;
import co.edu.uniquindio.ranking.texto.TokenizadorLucene;
import co.edu.uniquindio.ranking.texto.TokenizadorPropio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Benchmarks con JMH (version con librerias) de los tres algoritmos y de
 * los dos tokenizadores, sobre los mismos 20 abstracts del corpus.
 *
 * JMH resuelve los problemas de medir a mano con System.nanoTime():
 * - @Warmup: iteraciones de calentamiento para que el JIT optimice el codigo.
 * - @Fork: ejecuta cada benchmark en una maquina virtual NUEVA, para que
 *   un benchmark no afecte al siguiente (memoria, optimizaciones del JIT).
 * - Lo que devuelve un metodo @Benchmark se entrega a un "Blackhole",
 *   asi el JIT no puede eliminar el calculo por considerarlo inutil.
 * - Calcula el error de cada medicion (intervalo de confianza del 99,9 %).
 *
 * Se ejecuta con el metodo main (no es una prueba de JUnit).
 * Tarda unos 3 minutos. Resultados en results/java/jmh.csv
 */
@BenchmarkMode(Mode.AverageTime)          // tiempo promedio por ejecucion
@OutputTimeUnit(TimeUnit.MICROSECONDS)    // los algoritmos tardan decenas de microsegundos
@Warmup(iterations = 5, time = 1)         // 5 iteraciones de 1 s sin medir
@Measurement(iterations = 10, time = 1)   // 10 iteraciones de 1 s medidas
@Fork(2)                                  // todo se repite en 2 maquinas virtuales nuevas
@State(Scope.Benchmark)                   // los datos se preparan una vez y se comparten
public class BenchmarksJmh {

    /** Carpeta de los abstracts, relativa a java/ (donde se ejecuta). */
    private static final Path CARPETA_ABSTRACTS = Path.of("../data/abstracts");

    private List<String> textos;
    private List<FrecuenciaTermino> frecuencias;

    private final TokenizadorPropio tokenizadorPropio = new TokenizadorPropio();
    private final TokenizadorLucene tokenizadorLucene = new TokenizadorLucene();
    private final CountingSort countingSort = new CountingSort();
    private final RadixSort radixSort = new RadixSort();
    private final BucketSort bucketSort = new BucketSort();

    /**
     * Se ejecuta una vez antes de medir (no se incluye en el tiempo).
     * Lee los abstracts de los JSON: son los mismos que hay en la base de
     * datos, asi que no hace falta levantar Spring ni PostgreSQL.
     */
    @Setup
    public void preparar() throws IOException {
        textos = leerAbstracts();
        frecuencias = new ContadorFrecuencias(tokenizadorPropio).contar(textos);
    }

    private List<String> leerAbstracts() throws IOException {
        ObjectMapper lectorJson = new ObjectMapper();
        List<String> resumenes = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(CARPETA_ABSTRACTS)) {
            for (Path archivo : archivos.filter(a -> a.toString().endsWith(".json")).sorted().toList()) {
                resumenes.add(lectorJson.readTree(archivo.toFile()).path("abstract").asText());
            }
        }
        return resumenes;
    }

    // ---------- Preprocesamiento ----------

    @Benchmark
    public List<String> preprocesamientoPropio() {
        List<String> terminos = new ArrayList<>();
        for (String texto : textos) {
            terminos.addAll(tokenizadorPropio.tokenizar(texto));
        }
        return terminos;
    }

    @Benchmark
    public List<String> preprocesamientoLucene() {
        List<String> terminos = new ArrayList<>();
        for (String texto : textos) {
            terminos.addAll(tokenizadorLucene.tokenizar(texto));
        }
        return terminos;
    }

    // ---------- Algoritmos de ordenamiento ----------

    @Benchmark
    public List<FrecuenciaTermino> countingSort() {
        return countingSort.ordenar(frecuencias);
    }

    @Benchmark
    public List<FrecuenciaTermino> radixSort() {
        return radixSort.ordenar(frecuencias);
    }

    @Benchmark
    public List<FrecuenciaTermino> bucketSort() {
        return bucketSort.ordenar(frecuencias);
    }

    /** Ejecuta todos los benchmarks de esta clase y guarda el resultado en CSV. */
    public static void main(String[] args) throws RunnerException, IOException {
        Path resultado = Path.of("../results/java/jmh.csv");
        Files.createDirectories(resultado.getParent());

        Options opciones = new OptionsBuilder()
                .include(BenchmarksJmh.class.getSimpleName())
                .resultFormat(ResultFormatType.CSV)
                .result(resultado.toString())
                .build();
        new Runner(opciones).run();
    }
}