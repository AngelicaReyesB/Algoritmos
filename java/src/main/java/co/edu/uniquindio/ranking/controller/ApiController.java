package co.edu.uniquindio.ranking.controller;

import co.edu.uniquindio.ranking.medicion.ReporteMedicion;
import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.ComparacionRanking;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import co.edu.uniquindio.ranking.service.ServicioMedicion;
import co.edu.uniquindio.ranking.service.ServicioRanking;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * API REST que consume el frontend. Todas las rutas empiezan por /api.
 * Spring convierte automaticamente los objetos que se devuelven a JSON.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final int TOP_MAXIMO = 100;

    private final RepositorioArticulos repositorio;
    private final ServicioRanking servicioRanking;
    private final ServicioMedicion servicioMedicion;

    public ApiController(RepositorioArticulos repositorio,
                          ServicioRanking servicioRanking,
                          ServicioMedicion servicioMedicion) {
        this.repositorio = repositorio;
        this.servicioRanking = servicioRanking;
        this.servicioMedicion = servicioMedicion;
    }

    /** GET /api/articulos -> los 20 articulos con su titulo y abstract. */
    @GetMapping("/articulos")
    public List<Articulo> listarArticulos() {
        return repositorio.listar();
    }

    /** GET /api/ranking?top=15 -> Top N de los tres algoritmos y si coinciden. */
    @GetMapping("/ranking")
    public ComparacionRanking ranking(@RequestParam(defaultValue = "15") int top) {
        if (top < 1 || top > TOP_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El parametro top debe estar entre 1 y " + TOP_MAXIMO);
        }
        return servicioRanking.compararAlgoritmos(top);
    }

    /**
     * GET /api/tiempos -> ejecuta la medicion completa y devuelve los resultados.
     * Tarda unos segundos (la extraccion de los PDF se repite 5 veces).
     */
    @GetMapping("/tiempos")
    public ReporteMedicion tiempos() throws IOException {
        return servicioMedicion.medirTodo();
    }
}