package co.edu.uniquindio.ranking.embeddings;

/**
 * Operaciones con vectores, escritas a mano.
 * Las usan los dos modelos de embeddings (FastText y MiniLM).
 */
public final class Vectores {

    private Vectores() {
    }

    /**
     * Coseno entre dos vectores del mismo tamano: (A . B) / (|A| * |B|).
     *
     * Si uno de los vectores es todo ceros no hay angulo que medir y se devuelve 0.
     */
    public static double coseno(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamano");
        }
        double productoPunto = 0;
        double sumaCuadradosA = 0;
        double sumaCuadradosB = 0;
        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            sumaCuadradosA += a[i] * a[i];
            sumaCuadradosB += b[i] * b[i];
        }
        if (sumaCuadradosA == 0 || sumaCuadradosB == 0) {
            return 0.0;
        }
        return productoPunto / (Math.sqrt(sumaCuadradosA) * Math.sqrt(sumaCuadradosB));
    }
}