package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.embeddings.ModeloFastText;
import co.edu.uniquindio.ranking.embeddings.SimilitudFastText;
import co.edu.uniquindio.ranking.embeddings.SimilitudMiniLm;
import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.ComparacionSimilitud;
import co.edu.uniquindio.ranking.model.ResultadoSimilitud;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;
import co.edu.uniquindio.ranking.similitud.CosenoTfIdf;
import co.edu.uniquindio.ranking.similitud.Jaccard;
import co.edu.uniquindio.ranking.similitud.Levenshtein;
import co.edu.uniquindio.ranking.similitud.NeedlemanWunsch;
import co.edu.uniquindio.ranking.texto.TokenizadorPropio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Compara los articulos elegidos con los 6 algoritmos de similitud
 * (Requerimiento 1) y devuelve una matriz por algoritmo.
 */
@Service
public class ServicioSimilitud {

    private final RepositorioArticulos repositorio;
    private final Path archivoFastText;

    /**
     * Los 6 algoritmos. Se crean la primera vez que se usan (no al arrancar):
     * - TF-IDF necesita los abstracts, que se cargan en la BD despues de arrancar.
     * - FastText y MiniLM tardan unos segundos en cargar su modelo.
     */
    private List<AlgoritmoSimilitud> algoritmos;

    public ServicioSimilitud(RepositorioArticulos repositorio,
                             @Value("${ranking.archivo-fasttext}") String archivoFastText) {
        this.repositorio = repositorio;
        this.archivoFastText = Path.of(archivoFastText);
    }

    /**
     * Calcula la matriz de similitud de cada algoritmo para los articulos elegidos.
     *
     * @param ids ids de los articulos (minimo 2; los repetidos se ignoran)
     */
    public ComparacionSimilitud comparar(List<Integer> ids) {
        List<Articulo> articulos = buscarArticulos(ids);

        List<ResultadoSimilitud> resultados = new ArrayList<>();
        for (AlgoritmoSimilitud algoritmo : obtenerAlgoritmos()) {
            long inicio = System.nanoTime();
            double[][] matriz = calcularMatriz(algoritmo, articulos);
            double tiempoMs = (System.nanoTime() - inicio) / 1_000_000.0;
            resultados.add(new ResultadoSimilitud(algoritmo.getNombre(), algoritmo.getComplejidad(),
                    matriz, tiempoMs));
        }
        return new ComparacionSimilitud(articulos, resultados);
    }

    /**
     * Matriz de similitud: matriz[i][j] = similitud entre el articulo i y el j.
     *
     * - La diagonal es 1 (cada articulo es identico a si mismo).
     * - La matriz es simetrica: solo se calcula la parte de arriba (j > i)
     *   y se copia abajo. Asi cada par se compara una sola vez.
     */
    static double[][] calcularMatriz(AlgoritmoSimilitud algoritmo, List<Articulo> articulos) {
        int n = articulos.size();
        double[][] matriz = new double[n][n];
        for (int i = 0; i < n; i++) {
            matriz[i][i] = 1.0;
            for (int j = i + 1; j < n; j++) {
                double similitud = algoritmo.similitud(articulos.get(i).resumen(), articulos.get(j).resumen());
                matriz[i][j] = similitud;
                matriz[j][i] = similitud;
            }
        }
        return matriz;
    }

    /** Busca los articulos en la BD, en el orden en que se pidieron. Valida los ids. */
    private List<Articulo> buscarArticulos(List<Integer> ids) {
        if (ids == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar la lista de ids");
        }
        // LinkedHashSet quita los repetidos y conserva el orden
        List<Integer> sinRepetir = new ArrayList<>(new LinkedHashSet<>(ids));
        if (sinRepetir.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione al menos 2 articulos distintos");
        }

        Map<Integer, Articulo> porId = new HashMap<>();
        for (Articulo articulo : repositorio.listar()) {
            porId.put(articulo.id(), articulo);
        }

        List<Articulo> articulos = new ArrayList<>();
        for (Integer id : sinRepetir) {
            Articulo articulo = porId.get(id);
            if (articulo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el articulo con id " + id);
            }
            articulos.add(articulo);
        }
        return articulos;
    }

    /**
     * Crea los 6 algoritmos la primera vez y despues reutiliza los mismos.
     * synchronized: si llegan dos peticiones al tiempo, solo una los crea.
     */
    private synchronized List<AlgoritmoSimilitud> obtenerAlgoritmos() {
        if (algoritmos == null) {
            TokenizadorPropio tokenizador = new TokenizadorPropio();
            try {
                algoritmos = List.of(
                        new Levenshtein(tokenizador),
                        new NeedlemanWunsch(tokenizador),
                        new CosenoTfIdf(tokenizador, repositorio.listarResumenes()),
                        new Jaccard(tokenizador),
                        new SimilitudFastText(tokenizador, new ModeloFastText(archivoFastText)),
                        new SimilitudMiniLm());
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer el modelo de FastText: " + archivoFastText, e);
            }
        }
        return algoritmos;
    }

    /**
     * Devuelve uno de los 6 algoritmos por su nombre (por ejemplo "Coseno TF-IDF" o "MiniLM").
     * Lo usa el agrupamiento (R2) para no crear los algoritmos dos veces.
     */
    public AlgoritmoSimilitud buscarAlgoritmo(String nombre) {
        for (AlgoritmoSimilitud algoritmo : obtenerAlgoritmos()) {
            if (algoritmo.getNombre().equals(nombre)) {
                return algoritmo;
            }
        }
        throw new IllegalArgumentException("No existe el algoritmo de similitud: " + nombre);
    }
}