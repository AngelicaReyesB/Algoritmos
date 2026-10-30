package co.edu.uniquindio.ranking.medicion;

import co.edu.uniquindio.ranking.busqueda.KnuthMorrisPratt;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Datos de prueba de la medicion de busqueda: el texto y los patrones.
 *
 * El texto es el de los 20 abstracts unidos con un espacio. Los patrones
 * (escenarios) se escogen para mostrar como cambia el tiempo segun el patron:
 *
 *  - corto:         una palabra de 5 letras que aparece en el corpus.
 *  - largo:         un fragmento de 40 caracteres copiado del propio corpus (aparece).
 *  - ausente_corto: 5 letras que no aparecen en el corpus.
 *  - ausente_largo: el fragmento largo con el ultimo caracter cambiado, asi que
 *                   casi coincide pero no aparece. Es el caso que mas favorece a
 *                   los desplazamientos largos de Boyer-Moore.
 *
 * Cada escenario se comprueba contra el texto antes de medir: los que deben
 * aparecer, aparecen; los ausentes, no.
 */
public final class CorpusBusqueda {

    public static final String CORTO = "corto";
    public static final String LARGO = "largo";
    public static final String AUSENTE_CORTO = "ausente_corto";
    public static final String AUSENTE_LARGO = "ausente_largo";

    private static final String PATRON_CORTO = "model";
    private static final String PATRON_AUSENTE_CORTO = "zqxjk";
    private static final int LONGITUD_PATRON_LARGO = 40;
    private static final char CARACTER_QUE_ROMPE = '#';

    private CorpusBusqueda() {
    }

    /**
     * Lee los abstracts de los JSON de la carpeta (campo "abstract") y los une con
     * un espacio. Es la misma lectura de BenchmarksJmh: no necesita Spring ni
     * PostgreSQL.
     */
    public static String cargarTexto(Path carpetaAbstracts) throws IOException {
        ObjectMapper lectorJson = new ObjectMapper();
        List<String> resumenes = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(carpetaAbstracts)) {
            for (Path archivo : archivos.filter(a -> a.toString().endsWith(".json")).sorted().toList()) {
                resumenes.add(lectorJson.readTree(archivo.toFile()).path("abstract").asText());
            }
        }
        return String.join(" ", resumenes);
    }

    /**
     * Crea los cuatro escenarios (nombre -> patron) a partir del texto, en este
     * orden, y comprueba que los presentes aparecen y los ausentes no.
     */
    public static Map<String, String> crearEscenarios(String texto) {
        if (texto.length() < 2 * LONGITUD_PATRON_LARGO) {
            throw new IllegalArgumentException("El texto es demasiado corto para crear los escenarios");
        }

        String largo = fragmentoDelMedio(texto);
        String ausenteLargo = largo.substring(0, largo.length() - 1) + CARACTER_QUE_ROMPE;

        Map<String, String> escenarios = new LinkedHashMap<>();
        escenarios.put(CORTO, PATRON_CORTO);
        escenarios.put(LARGO, largo);
        escenarios.put(AUSENTE_CORTO, PATRON_AUSENTE_CORTO);
        escenarios.put(AUSENTE_LARGO, ausenteLargo);

        comprobar(texto, CORTO, PATRON_CORTO, true);
        comprobar(texto, LARGO, largo, true);
        comprobar(texto, AUSENTE_CORTO, PATRON_AUSENTE_CORTO, false);
        comprobar(texto, AUSENTE_LARGO, ausenteLargo, false);
        return escenarios;
    }

    /** 40 caracteres desde la primera palabra completa que empieza despues de la mitad del texto. */
    private static String fragmentoDelMedio(String texto) {
        int inicio = texto.length() / 2;
        while (inicio < texto.length() && texto.charAt(inicio) != ' ') {
            inicio++;
        }
        inicio++; // la primera letra de la palabra siguiente
        if (inicio + LONGITUD_PATRON_LARGO > texto.length()) {
            inicio = texto.length() / 2; // texto sin espacios o muy corto: se usa la mitad tal cual
        }
        return texto.substring(inicio, inicio + LONGITUD_PATRON_LARGO);
    }

    private static void comprobar(String texto, String escenario, String patron, boolean debeAparecer) {
        boolean aparece = !new KnuthMorrisPratt().buscar(texto, patron).isEmpty();
        if (aparece != debeAparecer) {
            throw new IllegalStateException("El escenario '" + escenario + "' (patron '" + patron + "') "
                    + (debeAparecer ? "deberia aparecer en el corpus y no aparece" : "no deberia aparecer en el corpus y aparece")
                    + ". Cambia el patron en CorpusBusqueda.");
        }
    }
}
