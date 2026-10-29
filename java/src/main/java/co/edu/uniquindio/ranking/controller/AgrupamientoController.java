package co.edu.uniquindio.ranking.controller;

import co.edu.uniquindio.ranking.model.ResultadoAgrupamiento;
import co.edu.uniquindio.ranking.service.ServicioAgrupamiento;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del Requerimiento 2: agrupamiento jerarquico.
 */
@RestController
@RequestMapping("/api/agrupamiento")
public class AgrupamientoController {

    private final ServicioAgrupamiento servicio;

    public AgrupamientoController(ServicioAgrupamiento servicio) {
        this.servicio = servicio;
    }

    /**
     * GET /api/agrupamiento?similitud=tfidf   (o similitud=minilm)
     * Respuesta: los articulos y un dendrograma por criterio, con su correlacion cofenetica.
     * Si la similitud no es tfidf ni minilm: 400.
     */
    @GetMapping
    public ResultadoAgrupamiento agrupar(@RequestParam(defaultValue = "tfidf") String similitud) {
        return servicio.agrupar(similitud);
    }
}