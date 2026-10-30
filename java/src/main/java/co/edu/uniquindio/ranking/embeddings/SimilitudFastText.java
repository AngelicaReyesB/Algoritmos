package co.edu.uniquindio.ranking.embeddings;

import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;
import co.edu.uniquindio.ranking.texto.Tokenizador;

import java.util.ArrayList;
import java.util.List;

/**
 * Similitud con vectores de palabras de FastText.
 *
 * 1. Se tokeniza el abstract (tokenizador propio).
 * 2. Se busca el vector de cada palabra en el modelo; las que no estan se ignoran.
 * 3. El vector del abstract es el PROMEDIO de los vectores de sus palabras.
 * 4. La similitud es el coseno entre los vectores de los dos abstracts.
 *
 * Diferencia con TF-IDF y Jaccard: dos palabras distintas pero con significado
 * parecido ("teacher" y "educator") tienen vectores cercanos, asi que tambien
 * suman similitud.
 */
public class SimilitudFastText implements AlgoritmoSimilitud {

    private final Tokenizador tokenizador;
    private final ModeloFastText modelo;

    public SimilitudFastText(Tokenizador tokenizador, ModeloFastText modelo) {
        this.tokenizador = tokenizador;
        this.modelo = modelo;
    }

    @Override
    public String getNombre() {
        return "FastText";
    }

    /**
     * Por cada palabra se suma su vector de d = 300 numeros: (n + m) * d.
     * El coseno final cuesta d.
     */
    @Override
    public String getComplejidad() {
        return "O((n + m)·d)";
    }

    /**
     * Coseno entre los vectores promedio de los dos textos.
     *
     * Con embeddings el coseno puede ser negativo (vectores que apuntan en
     * sentidos opuestos). Para que quede entre 0 y 1, como los demas
     * algoritmos, un coseno negativo se toma como 0: no hay similitud.
     */
    @Override
    public double similitud(String textoA, String textoB) {
        double coseno = Vectores.coseno(vectorDelTexto(textoA), vectorDelTexto(textoB));
        return Math.max(0.0, coseno);
    }

    /**
     * Promedio de los vectores de las palabras del texto que estan en el modelo.
     * Si ninguna palabra esta en el modelo, devuelve un vector de ceros.
     */
    public double[] vectorDelTexto(String texto) {
        double[] suma = new double[modelo.getDimension()];
        int encontradas = 0;

        for (String palabra : tokenizador.tokenizar(texto)) {
            double[] vector = modelo.vector(palabra);
            if (vector == null) {
                continue; // la palabra no esta en el modelo
            }
            for (int i = 0; i < suma.length; i++) {
                suma[i] += vector[i];
            }
            encontradas++;
        }

        if (encontradas > 0) {
            for (int i = 0; i < suma.length; i++) {
                suma[i] /= encontradas;
            }
        }
        return suma;
    }

    /** Palabras del texto que no estan en el modelo. Sirve para reportarlas. */
    public List<String> palabrasNoEncontradas(String texto) {
        List<String> faltantes = new ArrayList<>();
        for (String palabra : tokenizador.tokenizar(texto)) {
            if (modelo.vector(palabra) == null) {
                faltantes.add(palabra);
            }
        }
        return faltantes;
    }
}