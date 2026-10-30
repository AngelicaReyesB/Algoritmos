package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.DendrogramaCriterio;
import co.edu.uniquindio.ranking.model.ResultadoAgrupamiento;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio con un repositorio falso (sin base de datos).
 */
class ServicioAgrupamientoTest {

    private static final List<Articulo> ARTICULOS = List.of(
            new Articulo(1, "1. a.pdf", "A", null, "Artificial intelligence supports assessment in higher education.", false),
            new Articulo(2, "2. b.pdf", "B", null, "Generative AI changes assessment in universities.", false),
            new Articulo(3, "3. c.pdf", "C", null, "Teachers need training to use learning analytics tools.", false),
            new Articulo(4, "4. d.pdf", "D", null, "Learning analytics tools help teachers follow students.", false));

    private static final RepositorioArticulos REPOSITORIO = new RepositorioArticulos(null) {
        @Override
        public List<Articulo> listar() {
            return ARTICULOS;
        }

        @Override
        public List<String> listarResumenes() {
            List<String> resumenes = new ArrayList<>();
            for (Articulo articulo : ARTICULOS) {
                resumenes.add(articulo.resumen());
            }
            return resumenes;
        }
    };

    private static final ServicioAgrupamiento SERVICIO = new ServicioAgrupamiento(REPOSITORIO,
            new ServicioSimilitud(REPOSITORIO, "../data/embeddings/fasttext-corpus.vec"));

    @Test
    void devuelveUnDendrogramaPorCriterio() {
        ResultadoAgrupamiento resultado = SERVICIO.agrupar("tfidf");
        List<String> criterios = new ArrayList<>();
        for (DendrogramaCriterio dendrograma : resultado.dendrogramas()) {
            criterios.add(dendrograma.criterio());
        }
        assertEquals(List.of("SINGLE", "COMPLETE", "AVERAGE"), criterios);
    }

    @Test
    void cadaDendrogramaTieneNMenosUnoUniones() {
        for (DendrogramaCriterio dendrograma : SERVICIO.agrupar("tfidf").dendrogramas()) {
            assertEquals(3, dendrograma.uniones().size());
        }
    }

    @Test
    void laCorrelacionQuedaEntreMenosUnoYUno() {
        for (DendrogramaCriterio dendrograma : SERVICIO.agrupar("tfidf").dendrogramas()) {
            double r = dendrograma.correlacionCofenetica();
            assertTrue(r >= -1 && r <= 1, dendrograma.criterio() + ": " + r);
        }
    }

    @Test
    void elMejorCriterioEsElDeMayorCorrelacion() {
        ResultadoAgrupamiento resultado = SERVICIO.agrupar("tfidf");
        double mayor = -2;
        String esperado = null;
        for (DendrogramaCriterio dendrograma : resultado.dendrogramas()) {
            if (dendrograma.correlacionCofenetica() > mayor) {
                mayor = dendrograma.correlacionCofenetica();
                esperado = dendrograma.criterio();
            }
        }
        assertEquals(esperado, resultado.mejorCriterio());
    }

    @Test
    void funcionaTambienConMiniLm() {
        assertEquals(3, SERVICIO.agrupar("minilm").dendrogramas().size());
    }

    @Test
    void unaSimilitudDesconocidaResponde400() {
        assertThrows(ResponseStatusException.class, () -> SERVICIO.agrupar("jaccard"));
    }
}