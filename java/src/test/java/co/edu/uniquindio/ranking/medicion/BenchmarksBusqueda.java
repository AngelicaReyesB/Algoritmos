package co.edu.uniquindio.ranking.medicion;

import co.edu.uniquindio.ranking.busqueda.BoyerMoore;
import co.edu.uniquindio.ranking.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.ranking.busqueda.RabinKarp;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks con JMH (version con librerias) de los tres algoritmos de busqueda,
 * con la misma configuracion de BenchmarksJmh, sobre el texto de los 20 abstracts.
 *
 * Para cada algoritmo se miden por separado dos etapas:
 *  - Preprocesamiento: tablas (KMP, Boyer-Moore) o hash inicial (Rabin-Karp).
 *  - Busqueda: el recorrido del texto con el preprocesamiento ya hecho.
 *
 * El parametro "escenario" repite cada benchmark con los cuatro patrones de
 * CorpusBusqueda: corto, largo, ausente_corto y ausente_largo.
 *
 * Se ejecuta con el metodo main (no es una prueba de JUnit), desde java/.
 * Son 6 benchmarks x 4 escenarios = 24, de 30 s cada uno: tarda unos 12 minutos.
 * Resultados en results/java/jmh_busqueda.csv (en nanosegundos).
 */
@BenchmarkMode(Mode.AverageTime)          // tiempo promedio por ejecucion
@OutputTimeUnit(TimeUnit.NANOSECONDS)     // el preprocesamiento de un patron corto tarda nanosegundos
@Warmup(iterations = 5, time = 1)         // 5 iteraciones de 1 s sin medir
@Measurement(iterations = 10, time = 1)   // 10 iteraciones de 1 s medidas
@Fork(2)                                  // todo se repite en 2 maquinas virtuales nuevas
@State(Scope.Benchmark)                   // los datos se preparan una vez y se comparten
public class BenchmarksBusqueda {

    /** Carpeta de los abstracts, relativa a java/ (donde se ejecuta). */
    private static final Path CARPETA_ABSTRACTS = Path.of("../data/abstracts");

    /** Los mismos nombres de CorpusBusqueda (en una anotacion deben ser literales). */
    @Param({"corto", "largo", "ausente_corto", "ausente_largo"})
    public String escenario;

    private String texto;
    private String patron;

    private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();
    private final BoyerMoore boyerMoore = new BoyerMoore();
    private final RabinKarp rabinKarp = new RabinKarp();

    // Preprocesamiento ya calculado, para medir la busqueda sin el
    private int[] tablaPrefijos;
    private Map<Character, Integer> caracterMalo;
    private int[] sufijoBueno;
    private RabinKarp.Preprocesamiento preprocesamientoRabinKarp;

    /** Se ejecuta antes de medir (no se incluye en el tiempo). */
    @Setup
    public void preparar() throws IOException {
        texto = CorpusBusqueda.cargarTexto(CARPETA_ABSTRACTS);
        patron = CorpusBusqueda.crearEscenarios(texto).get(escenario);
        if (patron == null) {
            throw new IllegalStateException("Escenario desconocido: " + escenario);
        }

        tablaPrefijos = kmp.construirTablaPrefijos(patron);
        caracterMalo = boyerMoore.construirTablaCaracterMalo(patron);
        sufijoBueno = boyerMoore.construirTablaSufijoBueno(patron);
        preprocesamientoRabinKarp = rabinKarp.preprocesar(texto, patron);
    }

    // ---------- KMP ----------

    @Benchmark
    public int[] kmpPreprocesamiento() {
        return kmp.construirTablaPrefijos(patron);
    }

    @Benchmark
    public List<Integer> kmpBusqueda() {
        return kmp.buscar(texto, patron, tablaPrefijos);
    }

    // ---------- Boyer-Moore ----------

    @Benchmark
    public void boyerMoorePreprocesamiento(Blackhole sumidero) {
        // Las dos tablas se entregan al Blackhole para que ninguna se descarte
        sumidero.consume(boyerMoore.construirTablaCaracterMalo(patron));
        sumidero.consume(boyerMoore.construirTablaSufijoBueno(patron));
    }

    @Benchmark
    public List<Integer> boyerMooreBusqueda() {
        return boyerMoore.buscar(texto, patron, caracterMalo, sufijoBueno);
    }

    // ---------- Rabin-Karp ----------

    @Benchmark
    public RabinKarp.Preprocesamiento rabinKarpPreprocesamiento() {
        return rabinKarp.preprocesar(texto, patron);
    }

    @Benchmark
    public RabinKarp.Resultado rabinKarpBusqueda() {
        return rabinKarp.buscarContandoColisiones(texto, patron, preprocesamientoRabinKarp);
    }

    /** Ejecuta los benchmarks de esta clase y guarda el resultado en CSV. */
    public static void main(String[] args) throws RunnerException, IOException {
        Path resultado = Path.of("../results/java/jmh_busqueda.csv");
        Files.createDirectories(resultado.getParent());

        Options opciones = new OptionsBuilder()
                .include(BenchmarksBusqueda.class.getSimpleName())
                .resultFormat(ResultFormatType.CSV)
                .result(resultado.toString())
                .build();
        new Runner(opciones).run();
    }
}
