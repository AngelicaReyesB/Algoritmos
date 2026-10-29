package co.edu.uniquindio.ranking.controller;

import co.edu.uniquindio.ranking.model.ComparacionSimilitud;
import co.edu.uniquindio.ranking.model.SolicitudSimilitud;
import co.edu.uniquindio.ranking.service.ServicioSimilitud;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del Requerimiento 1: similitud textual.
 *
 * Es un controlador aparte de ControladorApi para no tocar los endpoints
 * del seguimiento (/api/articulos, /api/ranking, /api/tiempos).
 */
@RestController
@RequestMapping("/api/similitud")
public class SimilitudController {

    private final ServicioSimilitud servicio;

    public SimilitudController(ServicioSimilitud servicio) {
        this.servicio = servicio;
    }

    /**
     * POST /api/similitud
     * Cuerpo: { "ids": [3, 7, 12] }
     * Respuesta: los articulos y una matriz de similitud por cada algoritmo.
     * Si hay menos de 2 ids o alguno no existe: 400 con el mensaje del error.
     */
    @PostMapping
    public ComparacionSimilitud comparar(@RequestBody SolicitudSimilitud solicitud) {
        return servicio.comparar(solicitud.ids());
    }
}