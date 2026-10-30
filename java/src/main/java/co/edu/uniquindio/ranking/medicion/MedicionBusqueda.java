package co.edu.uniquindio.ranking.medicion;

import co.edu.uniquindio.ranking.busqueda.BoyerMoore;
import co.edu.uniquindio.ranking.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.ranking.busqueda.RabinKarp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Medicion manual (version sin librerias) de los tres algoritmos de busqueda,
 * con el mismo procedimiento del seguimiento: MedicionManual (calentamiento,
 * repeticiones, media, desviacion estandar y minimo, en milisegundos).
 *
 * Para cada algoritmo y cada escenario de CorpusBusqueda se mide POR SEPARADO:
 *  - preprocesamiento: lo que se calcula antes de recorrer el texto
 *      KMP: tabla de prefijos.
 *      Boyer-Moore: tabla del caracter malo y tabla del sufijo bueno.
 *      Rabin-Karp: h, hash del patron y hash de la primera ventana.
 *  - busqueda: el recorrido del texto, con el preprocesamiento ya hecho.
 *
 * Antes de medir se comprueba que los tres algoritmos encuentren exactamente las
 * mismas posiciones; si no, no tiene sentido comparar sus tiempos.
 *
 * Se ejecuta con el metodo main (no es una prueba de JUnit).
 * Resultados en results/java/busqueda.csv
 */
public class MedicionBusqueda {

    public static final int CALENTAMIENTO = 200;
    public static final int REPETICIONES = 1000;

    public static final String PREPROCESAMIENTO = "preprocesamiento";
    public static final String BUSQUEDA = "busqueda";

    /** Una fila del CSV. ocurrencias y colisiones solo aplican a la etapa de busqueda (colisiones, solo a Rabin-Karp). */
    public record Fila(String algoritmo, String escenario, int longitudPatron, int longitudTexto,
                       String etapa, ResultadoMedicion medicion, Integer ocurrencias, Integer colisiones) {
    }

    /** Las dos etapas de un algoritmo para un patron concreto, listas para medir. */
    private record Etapas(String algoritmo, Supplier<?> preprocesamiento, Supplier<?> busqueda,
                          List<Integer> posiciones, Integer colisiones) {
    }

    private final MedicionManual medicionManual;
    private final int calentamiento;
    private final int repeticiones;

    public MedicionBusqueda() {
        this(new MedicionManual(), CALENTAMIENTO, REPETICIONES);
    }

    public MedicionBusqueda(MedicionManual medicionManual, int calentamiento, int repeticiones) {
        this.medicionManual = medicionManual;
        this.calentamiento = calentamiento;
        this.repeticiones = repeticiones;
    }

    /** Mide los tres algoritmos en todos los escenarios, por etapa. */
    public List<Fila> medir(String texto) {
        List<Fila> filas = new ArrayList<>();
        for (Map.Entry<String, String> escenario : CorpusBusqueda.crearEscenarios(texto).entrySet()) {
            String patron = escenario.getValue();
            List<Etapas> algoritmos = List.of(
                    etapasKmp(texto, patron),
                    etapasBoyerMoore(texto, patron),
                    etapasRabinKarp(texto, patron));
            comprobarQueCoinciden(escenario.getKey(), algoritmos);

            for (Etapas etapas : algoritmos) {
                ResultadoMedicion preprocesamiento =
                        medicionManual.medir(PREPROCESAMIENTO, calentamiento, repeticiones, etapas.preprocesamiento());
                ResultadoMedicion busqueda =
                        medicionManual.medir(BUSQUEDA, calentamiento, repeticiones, etapas.busqueda());

                filas.add(new Fila(etapas.algoritmo(), escenario.getKey(), patron.length(), texto.length(),
                        PREPROCESAMIENTO, preprocesamiento, null, null));
                filas.add(new Fila(etapas.algoritmo(), escenario.getKey(), patron.length(), texto.length(),
                        BUSQUEDA, busqueda, etapas.posiciones().size(), etapas.colisiones()));
            }
        }
        return filas;
    }

    private Etapas etapasKmp(String texto, String patron) {
        KnuthMorrisPratt kmp = new KnuthMorrisPratt();
        int[] tabla = kmp.construirTablaPrefijos(patron);
        return new Etapas(kmp.getNombre(),
                () -> kmp.construirTablaPrefijos(patron),
                () -> kmp.buscar(texto, patron, tabla),
                kmp.buscar(texto, patron, tabla),
                null);
    }

    private Etapas etapasBoyerMoore(String texto, String patron) {
        BoyerMoore boyerMoore = new BoyerMoore();
        Map<Character, Integer> caracterMalo = boyerMoore.construirTablaCaracterMalo(patron);
        int[] sufijoBueno = boyerMoore.construirTablaSufijoBueno(patron);
        return new Etapas(boyerMoore.getNombre(),
                // Se devuelven las dos tablas para que el compilador no pueda descartar ninguna
                () -> new Object[] {boyerMoore.construirTablaCaracterMalo(patron),
                        boyerMoore.construirTablaSufijoBueno(patron)},
                () -> boyerMoore.buscar(texto, patron, caracterMalo, sufijoBueno),
                boyerMoore.buscar(texto, patron, caracterMalo, sufijoBueno),
                null);
    }

    private Etapas etapasRabinKarp(String texto, String patron) {
        RabinKarp rabinKarp = new RabinKarp();
        RabinKarp.Preprocesamiento pre = rabinKarp.preprocesar(texto, patron);
        RabinKarp.Resultado resultado = rabinKarp.buscarContandoColisiones(texto, patron, pre);
        return new Etapas(rabinKarp.getNombre(),
                () -> rabinKarp.preprocesar(texto, patron),
                () -> rabinKarp.buscarContandoColisiones(texto, patron, pre),
                resultado.posiciones(),
                resultado.colisiones());
    }

    private void comprobarQueCoinciden(String escenario, List<Etapas> algoritmos) {
        List<Integer> referencia = algoritmos.get(0).posiciones();
        for (Etapas etapas : algoritmos) {
            if (!etapas.posiciones().equals(referencia)) {
                throw new IllegalStateException("En el escenario '" + escenario + "' " + etapas.algoritmo()
                        + " no encuentra las mismas posiciones que " + algoritmos.get(0).algoritmo());
            }
        }
    }

    /** Escribe las filas en un CSV. Los decimales usan punto para que cualquier programa lo lea igual. */
    public static void escribirCsv(List<Fila> filas, Path destino) throws IOException {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        List<String> lineas = new ArrayList<>();
        lineas.add("algoritmo,escenario,longitud_patron,longitud_texto,etapa,repeticiones,"
                + "media_ms,desviacion_ms,minimo_ms,ocurrencias,colisiones");
        for (Fila fila : filas) {
            ResultadoMedicion medicion = fila.medicion();
            lineas.add(String.join(",",
                    fila.algoritmo(),
                    fila.escenario(),
                    String.valueOf(fila.longitudPatron()),
                    String.valueOf(fila.longitudTexto()),
                    fila.etapa(),
                    String.valueOf(medicion.repeticiones()),
                    decimales(medicion.mediaMs()),
                    decimales(medicion.desviacionMs()),
                    decimales(medicion.minimoMs()),
                    fila.ocurrencias() == null ? "" : String.valueOf(fila.ocurrencias()),
                    fila.colisiones() == null ? "" : String.valueOf(fila.colisiones())));
        }
        Files.write(destino, lineas, StandardCharsets.UTF_8);
    }

    private static String decimales(double valor) {
        return String.format(Locale.ROOT, "%.6f", valor);
    }

    /** Ejecuta la medicion completa y guarda el resultado en CSV. Se ejecuta desde java/. */
    public static void main(String[] args) throws IOException {
        String texto = CorpusBusqueda.cargarTexto(Path.of("../data/abstracts"));
        System.out.println("Texto: " + texto.length() + " caracteres. Midiendo...");

        List<Fila> filas = new MedicionBusqueda().medir(texto);

        Path destino = Path.of("../results/java/busqueda.csv");
        escribirCsv(filas, destino);

        System.out.printf("%-12s %-14s %-17s %10s %12s%n", "Algoritmo", "Escenario", "Etapa", "Media (ms)", "Minimo (ms)");
        for (Fila fila : filas) {
            System.out.printf(Locale.ROOT, "%-12s %-14s %-17s %10.6f %12.6f%n", fila.algoritmo(), fila.escenario(),
                    fila.etapa(), fila.medicion().mediaMs(), fila.medicion().minimoMs());
        }
        System.out.println("Guardado en " + destino.toAbsolutePath().normalize());
    }
}