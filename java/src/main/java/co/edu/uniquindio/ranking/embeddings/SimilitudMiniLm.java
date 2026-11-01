package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Similitud con el modelo de frases all-MiniLM-L6-v2 (sentence-transformers),
 * ejecutado en Java con LangChain4j.
 *
 * 1. El modelo recibe el abstract COMPLETO y devuelve un vector de 384 numeros.
 *    A diferencia de FastText, el vector de cada palabra depende de las
 *    palabras que la rodean (el modelo tiene en cuenta el contexto).
 * 2. La similitud es el coseno entre los vectores de los dos abstracts,
 *    calculado a mano en Vectores.coseno().
 *
 * Lo unico que hace la libreria es convertir el texto en vector. El modelo
 * corre en este mismo programa (ONNX): no usa internet ni una clave de API.
 *
 * Calcular un vector toma tiempo y el modelo ocupa mucha memoria (unos 100 MB
 * dentro del heap mas la libreria nativa de ONNX). Por eso cada vector se calcula
 * UNA sola vez y se guarda en un archivo (JSON). En las siguientes ejecuciones,
 * y en el contenedor Docker, el vector se lee del archivo y el modelo ni siquiera
 * se carga: solo se crea si llega un texto que no esta en el archivo.
 * Los vectores son exactamente los mismos que calcularia el modelo.
 */
public class SimilitudMiniLm implements AlgoritmoSimilitud {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** El modelo se crea solo cuando hace falta (ver obtenerModelo). */
    private EmbeddingModel modelo;

    /** Archivo donde se guardan los vectores; null = no usar archivo. */
    private final Path archivoVectores;

    /** Vectores leidos del archivo: huella SHA-256 del texto -> vector. */
    private final Map<String, double[]> vectoresEnArchivo;

    /** Vectores ya usados en esta ejecucion: texto -> vector. */
    private final Map<String, double[]> vectoresCalculados = new ConcurrentHashMap<>();

    /** Sin archivo: siempre calcula con el modelo (como antes). */
    public SimilitudMiniLm() {
        this(null);
    }

    /** Con archivo: lee los vectores guardados y agrega al archivo los que calcule. */
    public SimilitudMiniLm(Path archivoVectores) {
        this.archivoVectores = archivoVectores;
        this.vectoresEnArchivo = leerArchivo(archivoVectores);
    }

    @Override
    public String getNombre() {
        return "MiniLM";
    }

    /**
     * Comparar dos vectores ya calculados cuesta d = 384 (el coseno).
     * El vector de cada texto lo calcula el modelo una sola vez y queda guardado.
     */
    @Override
    public String getComplejidad() {
        return "O(d) + modelo (1 vez por texto)";
    }

    /**
     * Coseno entre los vectores de los dos textos.
     *
     * Igual que en FastText, un coseno negativo se toma como 0
     * para que el resultado quede entre 0 y 1.
     */
    @Override
    public double similitud(String textoA, String textoB) {
        double coseno = Vectores.coseno(vectorDelTexto(textoA), vectorDelTexto(textoB));
        return Math.max(0.0, coseno);
    }

    /**
     * Vector de 384 numeros del texto.
     * Orden de busqueda: memoria -> archivo -> modelo (y se guarda en el archivo).
     */
    public double[] vectorDelTexto(String texto) {
        return vectoresCalculados.computeIfAbsent(texto, this::buscarOCalcular);
    }

    private double[] buscarOCalcular(String texto) {
        String clave = huella(texto);
        double[] guardado;
        synchronized (vectoresEnArchivo) {
            guardado = vectoresEnArchivo.get(clave);
        }
        if (guardado != null) {
            return guardado;
        }
        double[] vector = calcularConModelo(texto);
        synchronized (vectoresEnArchivo) {
            vectoresEnArchivo.put(clave, vector);
            guardarArchivo();
        }
        return vector;
    }

    /** Le pide el vector al modelo y lo convierte de float[] a double[]. */
    private double[] calcularConModelo(String texto) {
        float[] vector = obtenerModelo().embed(texto).content().vector();
        double[] resultado = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            resultado[i] = vector[i];
        }
        return resultado;
    }

    /** Carga el modelo la primera vez que se necesita. */
    private synchronized EmbeddingModel obtenerModelo() {
        if (modelo == null) {
            modelo = new AllMiniLmL6V2EmbeddingModel();
        }
        return modelo;
    }

    /** Huella del texto: el archivo no guarda los abstracts, solo su SHA-256. */
    static String huella(String texto) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static Map<String, double[]> leerArchivo(Path archivo) {
        if (archivo != null && Files.isRegularFile(archivo)) {
            try {
                return new HashMap<>(JSON.readValue(archivo.toFile(),
                        new TypeReference<Map<String, double[]>>() { }));
            } catch (IOException e) {
                // archivo danado: se ignora y se calcula con el modelo
            }
        }
        return new HashMap<>();
    }

    /**
     * Escribe todos los vectores en el archivo. Si no se puede (por ejemplo, la carpeta
     * esta montada solo de lectura en Docker), se sigue sin archivo: no es un error.
     */
    private void guardarArchivo() {
        if (archivoVectores == null) {
            return;
        }
        try {
            if (archivoVectores.getParent() != null) {
                Files.createDirectories(archivoVectores.getParent());
            }
            JSON.writeValue(archivoVectores.toFile(), vectoresEnArchivo);
        } catch (IOException e) {
            // solo lectura o sin permisos: los vectores quedan en memoria
        }
    }
}
