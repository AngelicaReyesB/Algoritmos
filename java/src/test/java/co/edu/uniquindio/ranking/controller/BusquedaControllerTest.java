package co.edu.uniquindio.ranking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.ranking.model.ResultadoBusqueda;
import co.edu.uniquindio.ranking.service.ServicioBusquedaTestApoyo;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** El controlador es una clase normal: se prueba llamandolo directamente, sin levantar Spring. */
class BusquedaControllerTest {

    private final BusquedaController controlador =
            new BusquedaController(ServicioBusquedaTestApoyo.servicioCon("generative models and models"));

    @Test
    void unPatronVacioDevuelve400() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controlador.buscar(""));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("El patron no puede estar vacio", error.getReason());
    }

    @Test
    void unPatronDeMasDe100CaracteresDevuelve400() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.buscar("a".repeat(101)));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains("100"));
    }

    @Test
    void unPatronValidoDevuelveElResultado() {
        ResultadoBusqueda resultado = controlador.buscar("model");

        assertEquals(2, resultado.totalOcurrencias());
        assertTrue(resultado.coinciden());
    }

    @Test
    void unPatronDeExactamente100CaracteresNoDa400() {
        ResultadoBusqueda resultado = controlador.buscar("a".repeat(100));

        assertEquals(0, resultado.totalOcurrencias());
    }
}
