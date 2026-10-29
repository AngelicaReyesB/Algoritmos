package co.edu.uniquindio.ranking.embeddings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Vectores de palabras preentrenados de FastText (Meta, wiki-news-300d-1M).
 *
 * El archivo .vec es texto plano:
 *
 *   999994 300                         <- encabezado: cantidad de palabras y dimensiones
 *   education 0.0123 -0.0456 ...       <- una palabra y sus 300 numeros por linea
 *
 * Esta clase solo lee esa tabla y la guarda en un Map: palabra -> vector.
 * No entrena nada: los vectores ya vienen calculados por Meta.
 */
public class ModeloFastText {

    private final Map<String, double[]> vectores;
    private final int dimension;

    /** Lee el archivo .vec (el filtrado con las palabras del corpus). */
    public ModeloFastText(Path archivoVec) throws IOException {
        this(leerArchivo(archivoVec));
    }

    /** Recibe la tabla ya armada. Se usa en las pruebas con vectores pequenos. */
    public ModeloFastText(Map<String, double[]> vectores) {
        if (vectores.isEmpty()) {
            throw new IllegalArgumentException("El modelo no tiene palabras");
        }
        this.vectores = vectores;
        this.dimension = vectores.values().iterator().next().length;
    }

    /** Vector de la palabra, o null si la palabra no esta en el modelo. */
    public double[] vector(String palabra) {
        return vectores.get(palabra);
    }

    /** Cuantos numeros tiene cada vector (300 en FastText wiki-news). */
    public int getDimension() {
        return dimension;
    }

    /** Cuantas palabras tiene el modelo cargado. */
    public int getCantidadPalabras() {
        return vectores.size();
    }

    /**
     * Lee el .vec linea por linea. La primera linea es el encabezado y se salta.
     * Cada linea siguiente: la palabra, un espacio y los numeros separados por espacios.
     */
    private static Map<String, double[]> leerArchivo(Path archivoVec) throws IOException {
        Map<String, double[]> vectores = new HashMap<>();
        try (BufferedReader lector = Files.newBufferedReader(archivoVec, StandardCharsets.UTF_8)) {
            lector.readLine(); // encabezado: "cantidad dimensiones"
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.strip().split(" ");
                double[] vector = new double[partes.length - 1];
                for (int i = 1; i < partes.length; i++) {
                    vector[i - 1] = Double.parseDouble(partes[i]);
                }
                vectores.put(partes[0], vector);
            }
        }
        return vectores;
    }
}