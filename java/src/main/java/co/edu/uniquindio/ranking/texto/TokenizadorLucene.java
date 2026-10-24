package co.edu.uniquindio.ranking.texto;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tokenizador con Apache Lucene (version con librerias).
 *
 * Usa StandardAnalyzer, que:
 * - separa el texto en palabras segun el estandar Unicode (UAX #29),
 * - pasa a minusculas,
 * - elimina las palabras vacias que se le indiquen.
 *
 * Para que la comparacion con TokenizadorPropio sea justa, se aplican las
 * MISMAS reglas: misma limpieza previa (ligaduras y URLs), las mismas
 * palabras vacias, longitud minima 2 y solo palabras formadas por letras.
 * Asi, cualquier diferencia en los resultados se debe a como Lucene separa
 * las palabras, no a reglas distintas.
 *
 * No se usa EnglishAnalyzer porque aplica stemming ("models" -> "model"),
 * y el equipo decidio no aplicar stemming ni lematizacion.
 */
public class TokenizadorLucene implements Tokenizador {

    private static final int LONGITUD_MINIMA = 2;

    private final Analyzer analizador =
            new StandardAnalyzer(new CharArraySet(PalabrasVacias.todas(), true));

    @Override
    public List<String> tokenizar(String texto) {
        // Misma limpieza previa que TokenizadorPropio
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFKC);
        limpio = limpio.toLowerCase(Locale.ROOT);
        limpio = TokenizadorPropio.URL.matcher(limpio).replaceAll(" ");

        List<String> terminos = new ArrayList<>();
        // Lucene entrega las palabras una por una a traves de un "TokenStream"
        try (TokenStream flujo = analizador.tokenStream("texto", limpio)) {
            CharTermAttribute termino = flujo.addAttribute(CharTermAttribute.class);
            flujo.reset();
            while (flujo.incrementToken()) {
                String palabra = termino.toString();
                if (palabra.length() >= LONGITUD_MINIMA && esSoloLetras(palabra)) {
                    terminos.add(palabra);
                }
            }
            flujo.end();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return terminos;
    }

    /**
     * Lucene conserva numeros ("2020") y palabras con apostrofe o punto
     * ("agent's", "e.g"). Se descartan para aplicar la misma regla que
     * el tokenizador propio: solo letras.
     */
    private boolean esSoloLetras(String palabra) {
        for (int i = 0; i < palabra.length(); i++) {
            if (!Character.isLetter(palabra.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}