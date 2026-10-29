package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.algoritmos.AlgoritmoOrdenamiento;
import co.edu.uniquindio.ranking.algoritmos.BucketSort;
import co.edu.uniquindio.ranking.algoritmos.CountingSort;
import co.edu.uniquindio.ranking.algoritmos.RadixSort;
import co.edu.uniquindio.ranking.model.ComparacionRanking;
import co.edu.uniquindio.ranking.model.FrecuenciaTermino;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import co.edu.uniquindio.ranking.texto.ContadorFrecuencias;
import co.edu.uniquindio.ranking.texto.TokenizadorPropio;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une todo el proceso: abstracts de la BD -> frecuencias -> los tres algoritmos.
 */
@Service
public class ServicioRanking {

    private final RepositorioArticulos repositorio;
    private final List<AlgoritmoOrdenamiento> algoritmos =
            List.of(new CountingSort(), new RadixSort(), new BucketSort());

    public ServicioRanking(RepositorioArticulos repositorio) {
        this.repositorio = repositorio;
    }

    /** Ordena las frecuencias con los tres algoritmos y devuelve el Top N de cada uno. */
    public ComparacionRanking compararAlgoritmos(int top) {
        // Paso 1: frecuencias de todos los abstracts guardados en la BD
        ContadorFrecuencias contador = new ContadorFrecuencias(new TokenizadorPropio());
        List<FrecuenciaTermino> frecuencias = contador.contar(repositorio.listarResumenes());

        // Paso 2: ordenar con cada algoritmo y quedarse con los primeros "top"
        // LinkedHashMap: el JSON muestra los algoritmos en este mismo orden
        Map<String, List<FrecuenciaTermino>> rankings = new LinkedHashMap<>();
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            List<FrecuenciaTermino> ordenado = algoritmo.ordenar(frecuencias);
            int limite = Math.min(top, ordenado.size());
            rankings.put(algoritmo.getNombre(), ordenado.subList(0, limite));
        }

        // Paso 3: validar que los tres Top N sean identicos
        List<FrecuenciaTermino> referencia = rankings.get(algoritmos.get(0).getNombre());
        boolean coinciden = true;
        for (List<FrecuenciaTermino> ranking : rankings.values()) {
            if (!ranking.equals(referencia)) {
                coinciden = false;
            }
        }

        int frecuenciaMaxima = 0;
        for (FrecuenciaTermino f : frecuencias) {
            frecuenciaMaxima = Math.max(frecuenciaMaxima, f.frecuencia());
        }

        return new ComparacionRanking(frecuencias.size(), frecuenciaMaxima, coinciden, rankings);
    }
}