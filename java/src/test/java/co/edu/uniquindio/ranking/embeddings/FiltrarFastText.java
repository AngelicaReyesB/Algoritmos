package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import co.edu.uniquindio.ranking.texto.TokenizadorPropio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Herramienta que se ejecuta UNA vez (no es parte de la aplicacion).
 *
 * El archivo de FastText pesa 2,2 GB y tiene 1 millon de palabras. Esta clase
 * guarda solo las palabras que aparecen en los 20 abstracts del corpus, en un
 * archivo pequeno que si se sube al repositorio:
 *
 *   data/embeddings/fasttext-corpus.vec
 *
 * Uso: ejecutar el main desde IntelliJ (carpeta de trabajo: java/).
 * Si el .vec original esta en otra ruta, cambiar ARCHIVO_ORIGINAL.
 */
public class FiltrarFastText {

    private static final Path ARCHIVO_ORIGINAL =
            Path.of("C:/Users/felip/Desarrollo/Algoritmos/fasttext/wiki-news-300d-1M.vec");
    private static final Path CARPETA_PDFS = Path.of("../data/pdfs");
    private static final Path ARCHIVO_FILTRADO = Path.of("../data/embeddings/fasttext-corpus.vec");

    public static void main(String[] args) throws Exception {
        Set<String> vocabulario = vocabularioDelCorpus();
        System.out.println("Palabras distintas en el corpus: " + vocabulario.size());

        // 1. Recorrer el archivo grande y guardar solo las lineas de palabras del corpus
        List<String> lineas = new ArrayList<>();
        int dimension = 0;
        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_ORIGINAL, StandardCharsets.UTF_8)) {
            dimension = Integer.parseInt(lector.readLine().split(" ")[1]); // encabezado
            String linea;
            while ((linea = lector.readLine()) != null) {
                String palabra = linea.substring(0, linea.indexOf(' '));
                if (vocabulario.contains(palabra)) {
                    lineas.add(linea.strip());
                }
            }
        }

        // 2. Escribir el archivo pequeno con el mismo formato (encabezado + lineas)
        Files.createDirectories(ARCHIVO_FILTRADO.getParent());
        try (BufferedWriter escritor = Files.newBufferedWriter(ARCHIVO_FILTRADO, StandardCharsets.UTF_8)) {
            escritor.write(lineas.size() + " " + dimension);
            escritor.newLine();
            for (String linea : lineas) {
                escritor.write(linea);
                escritor.newLine();
            }
        }

        System.out.println("Palabras encontradas en FastText: " + lineas.size());
        System.out.println("Palabras que no estan en FastText: " + (vocabulario.size() - lineas.size()));
        System.out.println("Archivo generado: " + ARCHIVO_FILTRADO.toAbsolutePath().normalize());
    }

    /** Todas las palabras distintas de los 20 abstracts, con el tokenizador propio. */
    private static Set<String> vocabularioDelCorpus() throws Exception {
        LectorPdf lector = new LectorPdf();
        ExtractorAbstract extractor = new ExtractorAbstract();
        TokenizadorPropio tokenizador = new TokenizadorPropio();

        Set<String> vocabulario = new HashSet<>();
        try (Stream<Path> archivos = Files.list(CARPETA_PDFS)) {
            for (Path pdf : archivos.filter(a -> a.toString().endsWith(".pdf")).toList()) {
                String resumen = extractor.extraerAbstract(lector.leerPrimerasPaginas(pdf));
                if (resumen != null) {
                    vocabulario.addAll(tokenizador.tokenizar(resumen));
                }
            }
        }
        return vocabulario;
    }
}