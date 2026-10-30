package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.agrupamiento.AgrupamientoJerarquico;
import co.edu.uniquindio.ranking.agrupamiento.CorrelacionCofenetica;
import co.edu.uniquindio.ranking.agrupamiento.Criterio;
import co.edu.uniquindio.ranking.agrupamiento.MatrizDistancias;
import co.edu.uniquindio.ranking.agrupamiento.Union;
import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.DendrogramaCriterio;
import co.edu.uniquindio.ranking.model.ResultadoAgrupamiento;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import co.edu.uniquindio.ranking.similitud.AlgoritmoSimilitud;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupamiento jerarquico de los 20 abstracts (Requerimiento 2).
 *
 * 1. Matriz de distancias: 1 - similitud (TF-IDF o MiniLM).
 * 2. Agrupamiento con los 3 criterios: Single, Complete y Average.
 * 3. Correlacion cofenetica de cada uno, para elegir el mas coherente.
 */
@Service
public class ServicioAgrupamiento {

    private final RepositorioArticulos repositorio;
    private final ServicioSimilitud servicioSimilitud;

    public ServicioAgrupamiento(RepositorioArticulos repositorio, ServicioSimilitud servicioSimilitud) {
        this.repositorio = repositorio;
        this.servicioSimilitud = servicioSimilitud;
    }

    /**
     * @param similitud "tfidf" (coseno con TF-IDF) o "minilm"
     */
    public ResultadoAgrupamiento agrupar(String similitud) {
        AlgoritmoSimilitud algoritmo = elegirSimilitud(similitud);

        List<Articulo> articulos = repositorio.listar();
        List<String> resumenes = new ArrayList<>();
        for (Articulo articulo : articulos) {
            resumenes.add(articulo.resumen());
        }

        // Paso 1: una sola matriz de distancias para los tres criterios
        long inicioMatriz = System.nanoTime();
        double[][] distancias = MatrizDistancias.calcular(algoritmo, resumenes);
        double tiempoMatrizMs = (System.nanoTime() - inicioMatriz) / 1_000_000.0;

        // Pasos 2 y 3: agrupar con cada criterio y medir su correlacion cofenetica
        List<DendrogramaCriterio> dendrogramas = new ArrayList<>();
        DendrogramaCriterio mejor = null;
        for (Criterio criterio : Criterio.values()) {
            long inicio = System.nanoTime();
            List<Union> uniones = AgrupamientoJerarquico.agrupar(distancias, criterio);
            double tiempoMs = (System.nanoTime() - inicio) / 1_000_000.0;

            long inicioCorrelacion = System.nanoTime();
            double correlacion = CorrelacionCofenetica.calcular(distancias, uniones);
            double tiempoCorrelacionMs = (System.nanoTime() - inicioCorrelacion) / 1_000_000.0;

            DendrogramaCriterio dendrograma = new DendrogramaCriterio(criterio.name(), criterio.getComplejidad(),
                    uniones, correlacion, tiempoMs, tiempoCorrelacionMs);
            dendrogramas.add(dendrograma);

            if (mejor == null || correlacion > mejor.correlacionCofenetica()) {
                mejor = dendrograma;
            }
        }

        return new ResultadoAgrupamiento(similitud, articulos,
                MatrizDistancias.complejidad(algoritmo), tiempoMatrizMs,
                CorrelacionCofenetica.COMPLEJIDAD, dendrogramas, mejor.criterio());
    }

    /** Traduce el parametro de la URL al algoritmo del Requerimiento 1. */
    private AlgoritmoSimilitud elegirSimilitud(String similitud) {
        return switch (similitud) {
            case "tfidf" -> servicioSimilitud.buscarAlgoritmo("Coseno TF-IDF");
            case "minilm" -> servicioSimilitud.buscarAlgoritmo("MiniLM");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La similitud debe ser 'tfidf' o 'minilm'");
        };
    }
}