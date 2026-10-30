package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

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
 * Calcular un vector toma tiempo, asi que cada texto se calcula una sola vez
 * y el resultado se guarda en memoria.
 */
public class SimilitudMiniLm implements AlgoritmoSimilitud {

    private final EmbeddingModel modelo = new AllMiniLmL6V2EmbeddingModel();

    /** Vectores ya calculados: texto -> vector. */
    private final Map<String, double[]> vectoresCalculados = new ConcurrentHashMap<>();

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
     * Si ya se calculo antes, se devuelve el guardado; si no, se calcula y se guarda.
     */
    public double[] vectorDelTexto(String texto) {
        return vectoresCalculados.computeIfAbsent(texto, this::calcularVector);
    }

    /** Le pide el vector al modelo y lo convierte de float[] a double[]. */
    private double[] calcularVector(String texto) {
        float[] vector = modelo.embed(texto).content().vector();
        double[] resultado = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            resultado[i] = vector[i];
        }
        return resultado;
    }
}