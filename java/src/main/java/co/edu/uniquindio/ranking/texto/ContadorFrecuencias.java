package co.edu.uniquindio.ranking.texto;

import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuenta cuantas veces aparece cada termino en todos los abstracts.
 *
 * Usa LinkedHashMap en lugar de HashMap porque conserva el orden de
 * insercion: los terminos quedan en el orden de su PRIMERA aparicion
 * en el corpus. Ese orden es el criterio de desempate de los tres
 * algoritmos de ordenamiento (que son estables), y garantiza que los
 * tres produzcan exactamente el mismo ranking.
 */
public class ContadorFrecuencias {

    private final Tokenizador tokenizador;

    public ContadorFrecuencias(Tokenizador tokenizador) {
        this.tokenizador = tokenizador;
    }

    public List<FrecuenciaTermino> contar(List<String> textos) {
        Map<String, Integer> frecuencias = new LinkedHashMap<>();
        for (String texto : textos) {
            for (String termino : tokenizador.tokenizar(texto)) {
                // Si el termino no existe empieza en 0; luego suma 1
                frecuencias.put(termino, frecuencias.getOrDefault(termino, 0) + 1);
            }
        }

        List<FrecuenciaTermino> resultado = new ArrayList<>(frecuencias.size());
        for (Map.Entry<String, Integer> entrada : frecuencias.entrySet()) {
            resultado.add(new FrecuenciaTermino(entrada.getKey(), entrada.getValue()));
        }
        return resultado;
    }
}