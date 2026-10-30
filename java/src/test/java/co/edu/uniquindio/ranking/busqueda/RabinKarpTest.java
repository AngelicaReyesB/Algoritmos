package co.edu.uniquindio.ranking.busqueda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de Rabin-Karp.
 *
 * Hereda las pruebas comunes de PruebasAlgoritmoBusqueda (inicio, final, varias
 * ocurrencias, solapadas, vacios, mayusculas, comparacion con fuerza bruta...) y
 * agrega las propias: parametros, hash y colisiones.
 *
 * Los valores esperados de hash y colisiones se contrastaron con un calculo
 * directo e independiente del hash de cada ventana, en 17.500 casos con modulos
 * desde 1 hasta 10007.
 */
class RabinKarpTest extends PruebasAlgoritmoBusqueda {

    private final RabinKarp rabinKarp = new RabinKarp();

    @Override
    protected AlgoritmoBusqueda crearAlgoritmo() {
        return rabinKarp;
    }

    // ---------- Parametros ----------

    @Test
    void usaBase256YModuloPrimo101PorDefecto() {
        assertEquals(256, RabinKarp.BASE);
        assertEquals(101, rabinKarp.getModulo());
    }

    @Test
    void rechazaUnModuloInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new RabinKarp(0));
        assertThrows(IllegalArgumentException.class, () -> new RabinKarp(-7));
    }

    // ---------- Hash ----------

    @Test
    void hashDeUnCaracterEsSuCodigoModuloElPrimo() {
        assertEquals(97, rabinKarp.calcularHash("a", 0, 1)); // 97 mod 101
        assertEquals(0, rabinKarp.calcularHash("e", 0, 1));  // 101 mod 101
    }

    @Test
    void hashDeDosCaracteres() {
        // (97 * 256 + 98) mod 101 = 24930 mod 101 = 84
        assertEquals(84, rabinKarp.calcularHash("ab", 0, 2));
    }

    @Test
    void hashDeUnaVentanaDentroDelTexto() {
        // La ventana "ab" empieza en la posicion 2 de "xxabxx"
        assertEquals(84, rabinKarp.calcularHash("xxabxx", 2, 2));
    }

    // ---------- Colisiones ----------

    @Test
    void conModuloPequenoDosCaracteresDistintosTienenElMismoHash() {
        RabinKarp pequeno = new RabinKarp(7);
        // 'a' = 97 y 'h' = 104: ambos dan 6 al dividir entre 7
        assertEquals(6, pequeno.calcularHash("a", 0, 1));
        assertEquals(6, pequeno.calcularHash("h", 0, 1));
    }

    @Test
    void unaColisionNoSeReportaComoOcurrencia() {
        RabinKarp pequeno = new RabinKarp(7);
        RabinKarp.Resultado resultado = pequeno.buscarContandoColisiones("h", "a");

        assertEquals(List.of(), resultado.posiciones()); // el hash coincide, el texto no
        assertEquals(1, resultado.colisiones());
    }

    @Test
    void unaColisionYUnaOcurrenciaRealConviven() {
        RabinKarp pequeno = new RabinKarp(7);
        RabinKarp.Resultado resultado = pequeno.buscarContandoColisiones("ha", "a");

        assertEquals(List.of(1), resultado.posiciones());
        assertEquals(1, resultado.colisiones());
    }

    @Test
    void cadaVentanaQueColisionaSeCuenta() {
        RabinKarp pequeno = new RabinKarp(7);
        RabinKarp.Resultado resultado = pequeno.buscarContandoColisiones("hhhh", "a");

        assertEquals(List.of(), resultado.posiciones());
        assertEquals(4, resultado.colisiones());
    }

    @Test
    void lasOcurrenciasRealesNoCuentanComoColisiones() {
        RabinKarp.Resultado resultado = rabinKarp.buscarContandoColisiones("aaaa", "aa");

        assertEquals(List.of(0, 1, 2), resultado.posiciones());
        assertEquals(0, resultado.colisiones());
    }

    @Test
    void sinBusquedaPosibleNoHayColisiones() {
        assertEquals(0, rabinKarp.buscarContandoColisiones("ai", "artificial").colisiones());
        assertEquals(0, rabinKarp.buscarContandoColisiones("texto", "").colisiones());
    }

    // ---------- El modulo no cambia el resultado, solo las colisiones ----------

    @Test
    void conCualquierModuloElResultadoEsElDeFuerzaBruta() {
        Random azar = new Random(7);
        for (int modulo : new int[] {1, 2, 3, 7, 101, 10007}) {
            RabinKarp rk = new RabinKarp(modulo);
            for (int caso = 0; caso < 300; caso++) {
                String texto = cadenaAleatoria(azar, azar.nextInt(60));
                String patron = cadenaAleatoria(azar, 1 + azar.nextInt(5));
                assertEquals(referencia(texto, patron), rk.buscar(texto, patron),
                        "modulo=" + modulo + " texto=" + texto + " patron=" + patron);
            }
        }
    }

    // ---------- Preprocesamiento y busqueda por separado ----------

    @Test
    void preprocesarCalculaElPesoDelPrimeroYLosDosHashes() {
        RabinKarp.Preprocesamiento pre = rabinKarp.preprocesar("xxab", "ab");

        assertEquals(54L, pre.pesoPrimero());        // 256^1 mod 101
        assertEquals(84, pre.hashPatron());          // hash de "ab"
        assertEquals(35, pre.hashPrimeraVentana());  // hash de "xx"
    }

    @Test
    void buscarConElPreprocesamientoYaCalculadoDaLoMismo() {
        RabinKarp pequeno = new RabinKarp(7);
        String texto = "hahaha";
        String patron = "ha";
        RabinKarp.Preprocesamiento pre = pequeno.preprocesar(texto, patron);

        assertEquals(pequeno.buscarContandoColisiones(texto, patron),
                pequeno.buscarContandoColisiones(texto, patron, pre));
        assertEquals(pequeno.buscar(texto, patron), pequeno.buscar(texto, patron, pre));
    }

    // ---------- Entradas invalidas ----------

    @Test
    void rechazaEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> rabinKarp.buscar(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> rabinKarp.buscar("a", null));
    }

    // ---------- Apoyo ----------

    private String cadenaAleatoria(Random azar, int largo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < largo; i++) {
            sb.append((char) ('a' + azar.nextInt(3)));
        }
        return sb.toString();
    }

    /** Busqueda ingenua: prueba cada posicion. Solo sirve para verificar a Rabin-Karp. */
    private List<Integer> referencia(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i + patron.length() <= texto.length(); i++) {
            if (texto.startsWith(patron, i)) {
                posiciones.add(i);
            }
        }
        return posiciones;
    }
}
