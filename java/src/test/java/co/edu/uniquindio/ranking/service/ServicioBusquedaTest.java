package co.edu.uniquindio.ranking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.ranking.medicion.MedicionManual;
import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.ArticuloEncontrado;
import co.edu.uniquindio.ranking.model.ResultadoBusqueda;
import co.edu.uniquindio.ranking.model.TiempoBusqueda;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ServicioBusquedaTest {

    private static final List<String> ALGORITMOS = List.of("KMP", "Boyer-Moore", "Rabin-Karp");

    /** Un repositorio falso: devuelve estos abstracts sin tocar la base de datos. */
    static RepositorioArticulos repositorioCon(String... resumenes) {
        List<Articulo> articulos = new ArrayList<>();
        for (int i = 0; i < resumenes.length; i++) {
            articulos.add(new Articulo(i + 1, (i + 1) + "_articulo.pdf", "Titulo " + (i + 1), null,
                    resumenes[i], false));
        }
        return new RepositorioArticulos(null) {
            @Override
            public List<Articulo> listar() {
                return articulos;
            }
        };
    }

    /** Pocas repeticiones: aqui se prueba la logica, no los tiempos. */
    static ServicioBusqueda servicioCon(String... resumenes) {
        return new ServicioBusqueda(repositorioCon(resumenes), new MedicionManual(), 1, 3);
    }

    // ---------- Validacion ----------

    @Test
    void rechazaUnPatronVacio() {
        PatronInvalidoException error = assertThrows(PatronInvalidoException.class,
                () -> servicioCon("texto").buscar(""));
        assertEquals("El patron no puede estar vacio", error.getMessage());
    }

    @Test
    void rechazaUnPatronNulo() {
        assertThrows(PatronInvalidoException.class, () -> servicioCon("texto").buscar(null));
    }

    @Test
    void rechazaUnPatronDeMasDe100Caracteres() {
        PatronInvalidoException error = assertThrows(PatronInvalidoException.class,
                () -> servicioCon("texto").buscar("a".repeat(101)));
        assertTrue(error.getMessage().contains("100"));
        assertTrue(error.getMessage().contains("101"));
    }

    @Test
    void aceptaUnPatronDeExactamente100Caracteres() {
        String patron = "ab".repeat(50);
        ResultadoBusqueda resultado = servicioCon(patron + " y mas texto").buscar(patron);

        assertEquals(100, resultado.longitudPatron());
        assertEquals(1, resultado.totalOcurrencias());
    }

    @Test
    void unEspacioEsUnPatronValido() {
        ResultadoBusqueda resultado = servicioCon("a b c").buscar(" ");

        assertEquals(2, resultado.totalOcurrencias());
        assertEquals(List.of(1, 3), resultado.articulos().get(0).posiciones());
    }

    // ---------- Resultados ----------

    @Test
    void encuentraLasPosicionesExactasYLasReportaUnaSolaVez() {
        // "model" empieza en 11 y en 22
        ResultadoBusqueda resultado = servicioCon("generative models and models", "sin coincidencias")
                .buscar("model");

        ArticuloEncontrado primero = resultado.articulos().get(0);
        assertEquals(List.of(11, 22), primero.posiciones());
        assertTrue(primero.coinciden());
        assertEquals(1, primero.id());
        assertEquals("1_articulo.pdf", primero.archivo());
        assertEquals("generative models and models", primero.resumen());

        assertEquals(2, resultado.totalOcurrencias());
        assertEquals(1, resultado.articulosConOcurrencias());
        assertEquals(2, resultado.totalArticulos());
        assertEquals("model", resultado.patron());
        assertEquals(5, resultado.longitudPatron());
    }

    @Test
    void cadaAlgoritmoReportaSusOcurrenciasEnElOrdenDelEnunciado() {
        ResultadoBusqueda resultado = servicioCon("model model model").buscar("model");

        var ocurrencias = resultado.articulos().get(0).ocurrencias();
        assertEquals(ALGORITMOS, List.copyOf(ocurrencias.keySet()));
        for (String algoritmo : ALGORITMOS) {
            assertEquals(3, ocurrencias.get(algoritmo), algoritmo);
        }
    }

    @Test
    void incluyeLasOcurrenciasSolapadas() {
        ResultadoBusqueda resultado = servicioCon("aaaa").buscar("aa");

        assertEquals(List.of(0, 1, 2), resultado.articulos().get(0).posiciones());
        assertEquals(3, resultado.articulos().get(0).ocurrencias().get("Boyer-Moore"));
    }

    @Test
    void unPatronAusenteNoTieneOcurrenciasPeroLosAlgoritmosCoinciden() {
        ResultadoBusqueda resultado = servicioCon("generative models", "language models").buscar("zqxjk");

        assertEquals(0, resultado.totalOcurrencias());
        assertEquals(0, resultado.articulosConOcurrencias());
        assertTrue(resultado.coinciden());
        for (ArticuloEncontrado articulo : resultado.articulos()) {
            assertTrue(articulo.posiciones().isEmpty());
            assertTrue(articulo.coinciden());
        }
    }

    @Test
    void distingueMayusculasDeMinusculas() {
        ResultadoBusqueda resultado = servicioCon("Model model MODEL").buscar("model");

        assertEquals(List.of(6), resultado.articulos().get(0).posiciones());
    }

    @Test
    void unPatronMasLargoQueElAbstractNoEncuentraNada() {
        ResultadoBusqueda resultado = servicioCon("corto").buscar("este patron es mas largo");

        assertEquals(0, resultado.totalOcurrencias());
        assertTrue(resultado.coinciden());
    }

    @Test
    void unAbstractSinTextoNoRompeLaBusqueda() {
        ResultadoBusqueda resultado = servicioCon(null, "", "model").buscar("model");

        assertEquals(3, resultado.totalArticulos());
        assertEquals(1, resultado.totalOcurrencias());
        assertEquals(List.of(0), resultado.articulos().get(2).posiciones());
        assertTrue(resultado.articulos().get(0).posiciones().isEmpty());
    }

    @Test
    void mantieneElOrdenDeLosArticulos() {
        ResultadoBusqueda resultado = servicioCon("uno", "dos", "tres").buscar("o");

        assertEquals(List.of(1, 2, 3), resultado.articulos().stream().map(ArticuloEncontrado::id).toList());
    }

    @Test
    void sinArticulosLaRespuestaEstaVaciaPeroEsValida() {
        ResultadoBusqueda resultado = servicioCon().buscar("model");

        assertEquals(0, resultado.totalArticulos());
        assertEquals(0, resultado.totalOcurrencias());
        assertTrue(resultado.coinciden());
        assertEquals(3, resultado.tiempos().size());
    }

    // ---------- Los tres algoritmos coinciden ----------

    @Test
    void losTresAlgoritmosCoincidenEnMuchosCasosAleatorios() {
        Random azar = new Random(42);
        String alfabeto = "ab c";
        for (int caso = 0; caso < 150; caso++) {
            String[] textos = new String[3];
            for (int t = 0; t < textos.length; t++) {
                StringBuilder texto = new StringBuilder();
                int largo = azar.nextInt(60);
                for (int i = 0; i < largo; i++) {
                    texto.append(alfabeto.charAt(azar.nextInt(alfabeto.length())));
                }
                textos[t] = texto.toString();
            }
            StringBuilder patron = new StringBuilder();
            int largoPatron = 1 + azar.nextInt(5);
            for (int i = 0; i < largoPatron; i++) {
                patron.append(alfabeto.charAt(azar.nextInt(alfabeto.length())));
            }

            ResultadoBusqueda resultado = servicioCon(textos).buscar(patron.toString());

            assertTrue(resultado.coinciden(), "patron '" + patron + "'");
            for (ArticuloEncontrado articulo : resultado.articulos()) {
                assertTrue(articulo.coinciden());
                int esperado = articulo.posiciones().size();
                for (String algoritmo : ALGORITMOS) {
                    assertEquals(esperado, articulo.ocurrencias().get(algoritmo));
                }
            }
        }
    }

    @Test
    void laComprobacionDetectaCuandoUnAlgoritmoDifiere() {
        List<Integer> igual = List.of(1, 5);

        assertTrue(ServicioBusqueda.mismasPosiciones(igual, List.of(1, 5), List.of(1, 5)));
        assertFalse(ServicioBusqueda.mismasPosiciones(igual, List.of(1), List.of(1, 5)));
        assertFalse(ServicioBusqueda.mismasPosiciones(igual, List.of(1, 5), List.of(1, 6)));
        assertFalse(ServicioBusqueda.mismasPosiciones(igual, List.of(5, 1), List.of(1, 5)));
    }

    // ---------- Tiempos ----------

    @Test
    void midePreprocesamientoYBusquedaDeLosTresAlgoritmos() {
        ResultadoBusqueda resultado = servicioCon("generative models", "language models").buscar("model");

        List<TiempoBusqueda> tiempos = resultado.tiempos();
        assertEquals(ALGORITMOS, tiempos.stream().map(TiempoBusqueda::algoritmo).toList());
        for (TiempoBusqueda tiempo : tiempos) {
            assertEquals("preprocesamiento", tiempo.preprocesamiento().etapa());
            assertEquals("busqueda", tiempo.busqueda().etapa());
            for (var medicion : List.of(tiempo.preprocesamiento(), tiempo.busqueda())) {
                assertEquals(3, medicion.repeticiones());
                assertTrue(medicion.minimoMs() >= 0, tiempo.algoritmo());
                assertTrue(medicion.minimoMs() <= medicion.mediaMs(), tiempo.algoritmo());
                assertTrue(medicion.desviacionMs() >= 0, tiempo.algoritmo());
            }
        }
    }

    @Test
    void soloRabinKarpReportaColisiones() {
        ResultadoBusqueda resultado = servicioCon("generative models").buscar("model");

        assertNull(resultado.tiempos().get(0).colisiones());
        assertNull(resultado.tiempos().get(1).colisiones());
        assertNotNull(resultado.tiempos().get(2).colisiones());
    }

    @Test
    void lasColisionesDeRabinKarpSonMenoresOIgualesQueLasVentanasRevisadas() {
        // Texto largo: con modulo 101 es esperable alguna colision, y nunca mas que ventanas
        String texto = "generative models learn patterns from data ".repeat(50);
        ResultadoBusqueda resultado = servicioCon(texto).buscar("language");

        int ventanas = texto.length() - "language".length() + 1;
        int colisiones = resultado.tiempos().get(2).colisiones();
        assertTrue(colisiones >= 0 && colisiones <= ventanas);
        assertEquals(0, resultado.totalOcurrencias()); // una colision nunca se cuenta como ocurrencia
    }
}
