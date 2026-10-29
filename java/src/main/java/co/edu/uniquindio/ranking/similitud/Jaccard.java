package co.edu.uniquindio.ranking.similitud;

import co.edu.uniquindio.ranking.texto.Tokenizador;

import java.util.HashSet;
import java.util.Set;

/**
 * Coeficiente de Jaccard sobre los CONJUNTOS de terminos de cada texto.
 *
 *   Jaccard = |A interseccion B| / |A union B|
 *
 * = terminos que estan en los dos / terminos distintos entre los dos.
 *
 * Solo mira si un termino esta o no esta: no importa cuantas veces se
 * repite ni en que orden aparece.
 */
public class Jaccard implements AlgoritmoSimilitud {

    private final Tokenizador tokenizador;

    public Jaccard(Tokenizador tokenizador) {
        this.tokenizador = tokenizador;
    }

    @Override
    public String getNombre() {
        return "Jaccard";
    }

    @Override
    public double similitud(String textoA, String textoB) {
        return jaccard(conjunto(textoA), conjunto(textoB));
    }

    /** Terminos del texto sin repetir. Es publico para el caso de estudio. */
    public Set<String> conjunto(String texto) {
        return new HashSet<>(tokenizador.tokenizar(texto));
    }

    /** Cuantos terminos estan en los dos conjuntos. */
    public static int interseccion(Set<String> a, Set<String> b) {
        int enComun = 0;
        for (String termino : a) {
            if (b.contains(termino)) {
                enComun++;
            }
        }
        return enComun;
    }

    /**
     * Cuantos terminos distintos hay entre los dos conjuntos.
     * Los que estan en los dos se cuentan una sola vez: |A| + |B| - |A interseccion B|.
     */
    public static int union(Set<String> a, Set<String> b) {
        return a.size() + b.size() - interseccion(a, b);
    }

    /** |A interseccion B| / |A union B|. Dos conjuntos vacios se consideran iguales. */
    public static double jaccard(Set<String> a, Set<String> b) {
        int union = union(a, b);
        if (union == 0) {
            return 1.0;
        }
        return (double) interseccion(a, b) / union;
    }
}