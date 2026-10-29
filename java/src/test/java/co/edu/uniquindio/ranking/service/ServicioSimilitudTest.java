package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.modelo.Articulo;
import co.edu.uniquindio.ranking.modelo.ComparacionSimilitud;
import co.edu.uniquindio.ranking.modelo.ResultadoSimilitud;
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
 * Usa el archivo real de FastText del repositorio y el modelo real de MiniLM,
 * por eso la primera prueba tarda unos segundos.
 */
class ServicioSimilitudTest {

    private static final List<Articulo> ARTICULOS = List.of(
            new Articulo(1, "1. a.pdf", "A", null, "Artificial intelligence supports assessment in higher education.", false),
            new Articulo(2, "2. b.pdf", "B", null, "Generative AI changes how universities evaluate students.", false),
            new Articulo(3, "3. c.pdf", "C", null, "Teachers need training to use learning analytics tools.", false));

    /** Repositorio falso: devuelve los 3 articulos de arriba en lugar de consultar PostgreSQL. */
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

    private static final ServicioSimilitud SERVICIO =
            new ServicioSimilitud(REPOSITORIO, "../data/embeddings/fasttext-corpus.vec");

    @Test
    void devuelveUnaMatrizPorCadaUnoDeLosSeisAlgoritmos() {
        ComparacionSimilitud comparacion = SERVICIO.comparar(List.of(3, 1, 2));

        List<String> nombres = new ArrayList<>();
        for (ResultadoSimilitud resultado : comparacion.resultados()) {
            nombres.add(resultado.algoritmo());
        }
        assertEquals(List.of("Levenshtein", "Needleman-Wunsch", "Coseno TF-IDF", "Jaccard", "FastText", "MiniLM"),
                nombres);
    }

    @Test
    void losArticulosVienenEnElOrdenPedido() {
        ComparacionSimilitud comparacion = SERVICIO.comparar(List.of(3, 1));
        assertEquals(3, comparacion.articulos().get(0).id().intValue());
        assertEquals(1, comparacion.articulos().get(1).id().intValue());
    }

    @Test
    void cadaMatrizEsCuadradaSimetricaYConUnosEnLaDiagonal() {
        ComparacionSimilitud comparacion = SERVICIO.comparar(List.of(1, 2, 3));
        for (ResultadoSimilitud resultado : comparacion.resultados()) {
            double[][] matriz = resultado.matriz();
            assertEquals(3, matriz.length);
            for (int i = 0; i < 3; i++) {
                assertEquals(1.0, matriz[i][i], 1e-9);
                for (int j = 0; j < 3; j++) {
                    assertEquals(matriz[i][j], matriz[j][i], 1e-9);
                    assertTrue(matriz[i][j] >= 0 && matriz[i][j] <= 1,
                            resultado.algoritmo() + " fuera de rango: " + matriz[i][j]);
                }
            }
            assertTrue(resultado.tiempoMs() >= 0);
        }
    }

    @Test
    void conMenosDeDosArticulosResponde400() {
        assertThrows(ResponseStatusException.class, () -> SERVICIO.comparar(List.of(1)));
    }

    @Test
    void losIdsRepetidosCuentanUnaSolaVez() {
        // [1, 1] es un solo articulo: no alcanza para comparar
        assertThrows(ResponseStatusException.class, () -> SERVICIO.comparar(List.of(1, 1)));
    }

    @Test
    void unIdQueNoExisteResponde400() {
        assertThrows(ResponseStatusException.class, () -> SERVICIO.comparar(List.of(1, 99)));
    }

    @Test
    void sinListaDeIdsResponde400() {
        assertThrows(ResponseStatusException.class, () -> SERVICIO.comparar(null));
    }
}