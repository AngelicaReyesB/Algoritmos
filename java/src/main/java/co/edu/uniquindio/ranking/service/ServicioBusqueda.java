package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.busqueda.BoyerMoore;
import co.edu.uniquindio.ranking.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.ranking.busqueda.RabinKarp;
import co.edu.uniquindio.ranking.medicion.MedicionBusqueda;
import co.edu.uniquindio.ranking.medicion.MedicionManual;
import co.edu.uniquindio.ranking.medicion.ResultadoMedicion;
import co.edu.uniquindio.ranking.model.Articulo;
import co.edu.uniquindio.ranking.model.ArticuloEncontrado;
import co.edu.uniquindio.ranking.model.ResultadoBusqueda;
import co.edu.uniquindio.ranking.model.TiempoBusqueda;
import co.edu.uniquindio.ranking.repository.RepositorioArticulos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca un patron en los abstracts guardados con KMP, Boyer-Moore y Rabin-Karp.
 *
 * Para cada busqueda:
 *  1. Ejecuta los tres algoritmos sobre cada abstract.
 *  2. Comprueba que los tres encuentren exactamente las mismas posiciones
 *     (si no, lo informa con coinciden = false en vez de ocultarlo).
 *  3. Mide el tiempo de cada algoritmo, separando preprocesamiento y busqueda,
 *     con la misma MedicionManual del resto del proyecto.
 *
 * La busqueda distingue mayusculas de minusculas (es una comparacion exacta).
 */
@Service
public class ServicioBusqueda {

    /** Longitud maxima del patron. Boyer-Moore construye su tabla en O(m^3): sin limite, un patron enorme bloquearia el servidor. */
    public static final int PATRON_MAXIMO = 100;

    static final int CALENTAMIENTO = 20;
    static final int REPETICIONES = 200;

    private final RepositorioArticulos repositorio;
    private final MedicionManual medicionManual;
    private final int calentamiento;
    private final int repeticiones;

    private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();
    private final BoyerMoore boyerMoore = new BoyerMoore();
    private final RabinKarp rabinKarp = new RabinKarp();

    @Autowired
    public ServicioBusqueda(RepositorioArticulos repositorio, MedicionManual medicionManual) {
        this(repositorio, medicionManual, CALENTAMIENTO, REPETICIONES);
    }

    /** Para las pruebas: pocas repeticiones, porque ahi se prueba la logica y no los tiempos. */
    ServicioBusqueda(RepositorioArticulos repositorio, MedicionManual medicionManual,
                     int calentamiento, int repeticiones) {
        this.repositorio = repositorio;
        this.medicionManual = medicionManual;
        this.calentamiento = calentamiento;
        this.repeticiones = repeticiones;
    }

    /**
     * @throws PatronInvalidoException si el patron esta vacio o tiene mas de PATRON_MAXIMO caracteres
     */
    public ResultadoBusqueda buscar(String patron) {
        validarPatron(patron);

        List<Articulo> articulos = repositorio.listar();
        List<String> textos = new ArrayList<>();
        for (Articulo articulo : articulos) {
            textos.add(articulo.resumen() == null ? "" : articulo.resumen());
        }

        // Preprocesamiento de cada algoritmo
        int[] tablaPrefijos = kmp.construirTablaPrefijos(patron);
        Map<Character, Integer> caracterMalo = boyerMoore.construirTablaCaracterMalo(patron);
        int[] sufijoBueno = boyerMoore.construirTablaSufijoBueno(patron);
        List<RabinKarp.Preprocesamiento> hashes = preprocesarRabinKarp(textos, patron);

        // Busqueda con los tres algoritmos
        List<List<Integer>> porKmp = buscarConKmp(textos, patron, tablaPrefijos);
        List<List<Integer>> porBoyerMoore = buscarConBoyerMoore(textos, patron, caracterMalo, sufijoBueno);
        List<RabinKarp.Resultado> porRabinKarp = buscarConRabinKarp(textos, patron, hashes);

        // Un elemento por articulo + la comprobacion de que los tres coinciden
        boolean todosCoinciden = true;
        int totalOcurrencias = 0;
        int conOcurrencias = 0;
        int colisiones = 0;
        List<ArticuloEncontrado> encontrados = new ArrayList<>();
        for (int i = 0; i < articulos.size(); i++) {
            Articulo articulo = articulos.get(i);
            List<Integer> posicionesKmp = porKmp.get(i);
            List<Integer> posicionesBoyerMoore = porBoyerMoore.get(i);
            List<Integer> posicionesRabinKarp = porRabinKarp.get(i).posiciones();
            colisiones += porRabinKarp.get(i).colisiones();

            boolean coinciden = mismasPosiciones(posicionesKmp, posicionesBoyerMoore, posicionesRabinKarp);
            todosCoinciden = todosCoinciden && coinciden;

            Map<String, Integer> ocurrencias = new LinkedHashMap<>(); // el JSON respeta este orden
            ocurrencias.put(kmp.getNombre(), posicionesKmp.size());
            ocurrencias.put(boyerMoore.getNombre(), posicionesBoyerMoore.size());
            ocurrencias.put(rabinKarp.getNombre(), posicionesRabinKarp.size());

            totalOcurrencias += posicionesKmp.size();
            if (!posicionesKmp.isEmpty()) {
                conOcurrencias++;
            }
            encontrados.add(new ArticuloEncontrado(articulo.id(), articulo.archivo(), articulo.titulo(),
                    articulo.resumen(), posicionesKmp, ocurrencias, coinciden));
        }

        List<TiempoBusqueda> tiempos = medirTiempos(textos, patron, tablaPrefijos, caracterMalo,
                sufijoBueno, hashes, colisiones);

        return new ResultadoBusqueda(patron, patron.length(), todosCoinciden, totalOcurrencias,
                conOcurrencias, articulos.size(), encontrados, tiempos);
    }

    /** El patron debe tener entre 1 y PATRON_MAXIMO caracteres. Un espacio cuenta como caracter. */
    static void validarPatron(String patron) {
        if (patron == null || patron.isEmpty()) {
            throw new PatronInvalidoException("El patron no puede estar vacio");
        }
        if (patron.length() > PATRON_MAXIMO) {
            throw new PatronInvalidoException("El patron no puede tener mas de " + PATRON_MAXIMO
                    + " caracteres (tiene " + patron.length() + ")");
        }
    }

    /** true si las tres listas de posiciones son exactamente iguales. */
    static boolean mismasPosiciones(List<Integer> kmp, List<Integer> boyerMoore, List<Integer> rabinKarp) {
        return kmp.equals(boyerMoore) && kmp.equals(rabinKarp);
    }

    // ---------- Las tres busquedas (las mismas que se miden) ----------

    private List<List<Integer>> buscarConKmp(List<String> textos, String patron, int[] tablaPrefijos) {
        List<List<Integer>> resultados = new ArrayList<>();
        for (String texto : textos) {
            resultados.add(kmp.buscar(texto, patron, tablaPrefijos));
        }
        return resultados;
    }

    private List<List<Integer>> buscarConBoyerMoore(List<String> textos, String patron,
                                                    Map<Character, Integer> caracterMalo, int[] sufijoBueno) {
        List<List<Integer>> resultados = new ArrayList<>();
        for (String texto : textos) {
            resultados.add(boyerMoore.buscar(texto, patron, caracterMalo, sufijoBueno));
        }
        return resultados;
    }

    /** Rabin-Karp necesita el hash de la primera ventana de CADA texto: su preprocesamiento es por abstract. */
    private List<RabinKarp.Preprocesamiento> preprocesarRabinKarp(List<String> textos, String patron) {
        List<RabinKarp.Preprocesamiento> hashes = new ArrayList<>();
        for (String texto : textos) {
            hashes.add(rabinKarp.preprocesar(texto, patron));
        }
        return hashes;
    }

    private List<RabinKarp.Resultado> buscarConRabinKarp(List<String> textos, String patron,
                                                         List<RabinKarp.Preprocesamiento> hashes) {
        List<RabinKarp.Resultado> resultados = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) {
            resultados.add(rabinKarp.buscarContandoColisiones(textos.get(i), patron, hashes.get(i)));
        }
        return resultados;
    }

    // ---------- Tiempos ----------

    private List<TiempoBusqueda> medirTiempos(List<String> textos, String patron, int[] tablaPrefijos,
                                              Map<Character, Integer> caracterMalo, int[] sufijoBueno,
                                              List<RabinKarp.Preprocesamiento> hashes, int colisiones) {
        ResultadoMedicion kmpPre = medir(MedicionBusqueda.PREPROCESAMIENTO,
                () -> kmp.construirTablaPrefijos(patron));
        ResultadoMedicion kmpBusqueda = medir(MedicionBusqueda.BUSQUEDA,
                () -> buscarConKmp(textos, patron, tablaPrefijos));

        ResultadoMedicion bmPre = medir(MedicionBusqueda.PREPROCESAMIENTO,
                // Se devuelven las dos tablas para que el compilador no pueda descartar ninguna
                () -> new Object[] {boyerMoore.construirTablaCaracterMalo(patron),
                        boyerMoore.construirTablaSufijoBueno(patron)});
        ResultadoMedicion bmBusqueda = medir(MedicionBusqueda.BUSQUEDA,
                () -> buscarConBoyerMoore(textos, patron, caracterMalo, sufijoBueno));

        ResultadoMedicion rkPre = medir(MedicionBusqueda.PREPROCESAMIENTO,
                () -> preprocesarRabinKarp(textos, patron));
        ResultadoMedicion rkBusqueda = medir(MedicionBusqueda.BUSQUEDA,
                () -> buscarConRabinKarp(textos, patron, hashes));

        return List.of(
                new TiempoBusqueda(kmp.getNombre(), kmpPre, kmpBusqueda, null),
                new TiempoBusqueda(boyerMoore.getNombre(), bmPre, bmBusqueda, null),
                new TiempoBusqueda(rabinKarp.getNombre(), rkPre, rkBusqueda, colisiones));
    }

    private ResultadoMedicion medir(String etapa, java.util.function.Supplier<?> tarea) {
        return medicionManual.medir(etapa, calentamiento, repeticiones, tarea);
    }
}
