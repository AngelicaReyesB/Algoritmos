package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.pdf.ExtractorAbstract;
import co.edu.uniquindio.ranking.pdf.LectorPdf;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Caso de estudio de MiniLM para el documento (articulos 5 y 6).
 *
 * Extrae los dos abstracts igual que la aplicacion, le pide el vector
 * de 384 numeros a MiniLM y muestra los datos del coseno paso a paso:
 * producto punto, normas y coseno.
 *
 * Se ejecuta con el metodo main desde la carpeta java/.
 */
public class CasoMiniLm {

    public static void main(String[] args) throws IOException {
        String abstractA = leerAbstract("5.");
        String abstractB = leerAbstract("6.");

        EmbeddingModel modelo = new AllMiniLmL6V2EmbeddingModel();
        float[] vectorA = modelo.embed(abstractA).content().vector();
        float[] vectorB = modelo.embed(abstractB).content().vector();

        double productoPunto = 0;
        double sumaCuadradosA = 0;
        double sumaCuadradosB = 0;
        for (int i = 0; i < vectorA.length; i++) {
            productoPunto += vectorA[i] * vectorB[i];
            sumaCuadradosA += vectorA[i] * vectorA[i];
            sumaCuadradosB += vectorB[i] * vectorB[i];
        }
        double normaA = Math.sqrt(sumaCuadradosA);
        double normaB = Math.sqrt(sumaCuadradosB);

        System.out.println("Dimension: " + vectorA.length);
        System.out.printf(Locale.ROOT, "Primeros 5 valores de A: %.4f %.4f %.4f %.4f %.4f%n",
                vectorA[0], vectorA[1], vectorA[2], vectorA[3], vectorA[4]);
        System.out.printf(Locale.ROOT, "Primeros 5 valores de B: %.4f %.4f %.4f %.4f %.4f%n",
                vectorB[0], vectorB[1], vectorB[2], vectorB[3], vectorB[4]);
        System.out.printf(Locale.ROOT, "Producto punto: %.4f%n", productoPunto);
        System.out.printf(Locale.ROOT, "Norma de A: %.4f%n", normaA);
        System.out.printf(Locale.ROOT, "Norma de B: %.4f%n", normaB);
        System.out.printf(Locale.ROOT, "Coseno: %.4f%n", productoPunto / (normaA * normaB));
    }

    /** Busca en data/pdfs el PDF cuyo nombre empieza con el prefijo y extrae su abstract. */
    private static String leerAbstract(String prefijo) throws IOException {
        try (Stream<Path> archivos = Files.list(Path.of("../data/pdfs"))) {
            Path pdf = archivos
                    .filter(a -> a.getFileName().toString().startsWith(prefijo))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No se encontro el PDF " + prefijo));
            String texto = new LectorPdf().leerPrimerasPaginas(pdf);
            return new ExtractorAbstract().extraerAbstract(texto);
        }
    }
}