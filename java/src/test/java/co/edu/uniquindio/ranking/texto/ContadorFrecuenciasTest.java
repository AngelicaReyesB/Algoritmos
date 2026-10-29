package co.edu.uniquindio.ranking.texto;

import co.edu.uniquindio.ranking.model.FrecuenciaTermino;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContadorFrecuenciasTest {

    private final ContadorFrecuencias contador = new ContadorFrecuencias(new TokenizadorPropio());

    @Test
    void cuentaTerminosEnVariosTextos() {
        List<FrecuenciaTermino> resultado = contador.contar(List.of(
                "language models generate text",
                "large language models"));
        assertEquals(List.of(
                new FrecuenciaTermino("language", 2),
                new FrecuenciaTermino("models", 2),
                new FrecuenciaTermino("generate", 1),
                new FrecuenciaTermino("text", 1),
                new FrecuenciaTermino("large", 1)), resultado);
    }

    @Test
    void conservaElOrdenDePrimeraAparicion() {
        List<FrecuenciaTermino> resultado = contador.contar(List.of("zeta alfa zeta beta alfa"));
        assertEquals(List.of(
                new FrecuenciaTermino("zeta", 2),
                new FrecuenciaTermino("alfa", 2),
                new FrecuenciaTermino("beta", 1)), resultado);
    }

    @Test
    void modelYModelsSonTerminosDistintos() {
        // Sin lematizacion: singular y plural se cuentan por separado
        List<FrecuenciaTermino> resultado = contador.contar(List.of("model models model"));
        assertEquals(List.of(
                new FrecuenciaTermino("model", 2),
                new FrecuenciaTermino("models", 1)), resultado);
    }

    @Test
    void sinTextosDevuelveListaVacia() {
        assertTrue(contador.contar(List.of()).isEmpty());
    }
}