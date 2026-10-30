package co.edu.uniquindio.ranking.controller;

import co.edu.uniquindio.ranking.model.ResultadoBusqueda;
import co.edu.uniquindio.ranking.service.PatronInvalidoException;
import co.edu.uniquindio.ranking.service.ServicioBusqueda;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Busqueda de un patron en los abstracts con KMP, Boyer-Moore y Rabin-Karp.
 * Es un controlador aparte de ApiController para no tocar el que ya funciona.
 */
@RestController
@RequestMapping("/api")
public class BusquedaController {

    private final ServicioBusqueda servicioBusqueda;

    public BusquedaController(ServicioBusqueda servicioBusqueda) {
        this.servicioBusqueda = servicioBusqueda;
    }

    /**
     * GET /api/busqueda?patron=model -> ocurrencias por articulo con cada algoritmo,
     * si los tres coinciden y sus tiempos.
     *
     * Responde 400 si el patron esta vacio o tiene mas de 100 caracteres.
     * Si falta el parametro se trata como vacio, para responder con el mismo mensaje.
     */
    @GetMapping("/busqueda")
    public ResultadoBusqueda buscar(@RequestParam(defaultValue = "") String patron) {
        try {
            return servicioBusqueda.buscar(patron);
        } catch (PatronInvalidoException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
