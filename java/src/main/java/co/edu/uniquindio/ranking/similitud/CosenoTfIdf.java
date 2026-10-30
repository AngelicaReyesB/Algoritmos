package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Similitud del coseno con pesos TF-IDF.
 *
 * Cada abstract se convierte en un vector: una posicion por termino y, en
 * cada posicion, el peso TF-IDF del termino:
 *
 *   TF(t)     = cuantas veces aparece t en el abstract
 *   IDF(t)    = ln(N / df(t))   N = numero de abstracts del corpus,
 *                               df(t) = en cuantos abstracts aparece t
 *   peso(t)   = TF(t) * IDF(t)
 *
 * IDF le baja el peso a los terminos que aparecen en casi todos los abstracts
 * (dicen poco de cada uno) y se lo sube a los que aparecen en pocos.
 *
 * La similitud es el coseno del angulo entre los dos vectores:
 *
 *   coseno = (A . B) / (|A| * |B|)
 *
 * Como los pesos nunca son negativos, el resultado queda entre 0 y 1.
 */
public class CosenoTfIdf implements AlgoritmoSimilitud {

    private final Tokenizador tokenizador;

    /** IDF de cada termino del corpus. Se calcula una sola vez, en el constructor. */
    private final Map<String, Double> idf = new HashMap<>();

    /**
     * @param corpus los abstracts sobre los que se calcula el IDF (los 20 del corpus).
     */
    public CosenoTfIdf(Tokenizador tokenizador, List<String> corpus) {
        this.tokenizador = tokenizador;
        calcularIdf(corpus);
    }

    @Override
    public String getNombre() {
        return "Coseno TF-IDF";
    }

    /**
     * Arma el vector TF-IDF de cada texto recorriendo sus palabras una vez (n + m)
     * y el coseno recorre esos vectores. El IDF se calcula una sola vez al crear
     * el algoritmo, no en cada comparacion.
     */
    @Override
    public String getComplejidad() {
        return "O(n + m)";
    }

    @Override
    public double similitud(String textoA, String textoB) {
        return coseno(vectorTfIdf(textoA), vectorTfIdf(textoB));
    }

    /** IDF(t) = ln(N / df(t)). */
    private void calcularIdf(List<String> corpus) {
        // df: en cuantos abstracts aparece cada termino (se cuenta una vez por abstract)
        Map<String, Integer> df = new HashMap<>();
        for (String documento : corpus) {
            Set<String> terminosDelDocumento = new HashSet<>(tokenizador.tokenizar(documento));
            for (String termino : terminosDelDocumento) {
                df.put(termino, df.getOrDefault(termino, 0) + 1);
            }
        }

        int n = corpus.size();
        for (Map.Entry<String, Integer> entrada : df.entrySet()) {
            idf.put(entrada.getKey(), Math.log((double) n / entrada.getValue()));
        }
    }

    /**
     * IDF de un termino. Un termino que no aparece en el corpus tiene IDF 0:
     * no hay datos para saber que tan informativo es.
     */
    public double idf(String termino) {
        return idf.getOrDefault(termino, 0.0);
    }

    /** TF: cuantas veces aparece cada termino en el texto. */
    public Map<String, Integer> tf(String texto) {
        Map<String, Integer> tf = new HashMap<>();
        for (String termino : tokenizador.tokenizar(texto)) {
            tf.put(termino, tf.getOrDefault(termino, 0) + 1);
        }
        return tf;
    }

    /** Vector TF-IDF del texto: termino -> TF * IDF. Es publico para el caso de estudio. */
    public Map<String, Double> vectorTfIdf(String texto) {
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entrada : tf(texto).entrySet()) {
            String termino = entrada.getKey();
            vector.put(termino, entrada.getValue() * idf(termino));
        }
        return vector;
    }

    /**
     * Coseno entre dos vectores: (A . B) / (|A| * |B|).
     *
     * Si uno de los vectores tiene norma 0 (ningun termino con peso), no hay
     * angulo que medir y se devuelve 0.
     */
    public static double coseno(Map<String, Double> a, Map<String, Double> b) {
        // Producto punto: solo suman los terminos que estan en los dos vectores
        double productoPunto = 0;
        for (Map.Entry<String, Double> entrada : a.entrySet()) {
            Double pesoEnB = b.get(entrada.getKey());
            if (pesoEnB != null) {
                productoPunto += entrada.getValue() * pesoEnB;
            }
        }

        double normaA = norma(a);
        double normaB = norma(b);
        if (normaA == 0 || normaB == 0) {
            return 0.0;
        }
        return productoPunto / (normaA * normaB);
    }

    /** Norma (largo) del vector: raiz de la suma de los pesos al cuadrado. */
    private static double norma(Map<String, Double> vector) {
        double suma = 0;
        for (double peso : vector.values()) {
            suma += peso * peso;
        }
        return Math.sqrt(suma);
    }
}