package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.modelo.ComparacionRanking;
import co.edu.uniquindio.ranking.modelo.FrecuenciaTermino;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioRankingTest {

    /**
     * Repositorio falso: en lugar de consultar PostgreSQL devuelve textos fijos.
     * Asi se prueba el servicio sin base de datos.
     */
    private static RepositorioArticulos repositorioCon(List<String> resumenes) {
        return new RepositorioArticulos(null) {
            @Override
            public List<String> listarResumenes() {
                return resumenes;
            }
        };
    }

    private final ServicioRanking servicio = new ServicioRanking(repositorioCon(List.of(
            "language models generate text",
            "large language models",
            "models")));

    @Test
    void devuelveElTopDeLosTresAlgoritmos() {
        ComparacionRanking comparacion = servicio.compararAlgoritmos(2);

        assertEquals(3, comparacion.rankings().size());
        List<FrecuenciaTermino> esperado = List.of(
                new FrecuenciaTermino("models", 3),
                new FrecuenciaTermino("language", 2));
        assertEquals(esperado, comparacion.rankings().get("Counting Sort"));
        assertEquals(esperado, comparacion.rankings().get("Radix Sort"));
        assertEquals(esperado, comparacion.rankings().get("Bucket Sort"));
        assertTrue(comparacion.coinciden());
    }

    @Test
    void reportaTerminosDistintosYFrecuenciaMaxima() {
        ComparacionRanking comparacion = servicio.compararAlgoritmos(15);
        assertEquals(5, comparacion.terminosDistintos()); // language, models, generate, text, large
        assertEquals(3, comparacion.frecuenciaMaxima());  // models
    }

    @Test
    void siTopEsMayorQueLosTerminosDevuelveTodos() {
        ComparacionRanking comparacion = servicio.compararAlgoritmos(15);
        assertEquals(5, comparacion.rankings().get("Counting Sort").size());
    }
}