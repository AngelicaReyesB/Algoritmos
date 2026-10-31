package co.edu.uniquindio.ranking.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.ranking.service.ServicioBusquedaTestApoyo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * La web (vistaBusqueda.js) lee estos nombres exactos del JSON. Si alguien renombra un
 * campo del record, esta prueba falla antes de que la pestana deje de funcionar.
 */
class ResultadoBusquedaJsonTest {

    @Test
    void elJsonTieneLosCamposQueUsaLaWeb() throws Exception {
        ResultadoBusqueda resultado = ServicioBusquedaTestApoyo
                .servicioCon("generative models and models", "otro texto").buscar("model");

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(resultado));

        assertEquals("model", json.get("patron").asText());
        assertEquals(5, json.get("longitudPatron").asInt());
        assertTrue(json.get("coinciden").asBoolean());
        assertEquals(2, json.get("totalOcurrencias").asInt());
        assertEquals(1, json.get("articulosConOcurrencias").asInt());
        assertEquals(2, json.get("totalArticulos").asInt());
        assertEquals(38, json.get("longitudTexto").asInt()); // 28 + 10 caracteres
        assertTrue(json.get("tamanoAlfabeto").asInt() > 0);

        JsonNode primero = json.get("articulos").get(0);
        assertEquals(1, primero.get("id").asInt());
        assertEquals("1_articulo.pdf", primero.get("archivo").asText());
        assertEquals("Titulo 1", primero.get("titulo").asText());
        assertEquals("generative models and models", primero.get("resumen").asText());
        assertEquals(11, primero.get("posiciones").get(0).asInt());
        assertEquals(22, primero.get("posiciones").get(1).asInt());
        assertTrue(primero.get("coinciden").asBoolean());
        assertEquals(2, primero.get("ocurrencias").get("Boyer-Moore").asInt());

        JsonNode rabinKarp = json.get("tiempos").get(2);
        assertEquals("Rabin-Karp", rabinKarp.get("algoritmo").asText());
        assertTrue(rabinKarp.get("colisiones").isInt());
        assertTrue(rabinKarp.get("preprocesamiento").get("mediaMs").isNumber());
        assertTrue(rabinKarp.get("busqueda").get("desviacionMs").isNumber());
        assertTrue(rabinKarp.get("busqueda").get("minimoMs").isNumber());
        assertEquals(3, rabinKarp.get("busqueda").get("repeticiones").asInt());
        assertTrue(json.get("tiempos").get(0).get("colisiones").isNull());
        assertEquals("O(m^3)", json.get("tiempos").get(1).get("complejidadPreprocesamiento").asText());
        assertEquals("O(n)", json.get("tiempos").get(0).get("complejidadBusqueda").asText());
    }
}
