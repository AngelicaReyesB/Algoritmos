package co.edu.uniquindio.ranking.texto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tokenizador implementado por el equipo (version sin librerias externas).
 *
 * Reglas, en este orden:
 * 1. Normaliza ligaduras tipograficas de los PDF ("ﬁ" -> "fi").
 * 2. Pasa todo a minusculas.
 * 3. Elimina URLs (https://..., www..., dominio.com).
 * 4. Toma solo secuencias de letras: numeros, signos y guiones separan palabras.
 *    Por eso "state-of-the-art" produce "state", "of", "the", "art".
 * 5. Descarta palabras de menos de 2 letras (se conservan "ai" y "llm").
 * 6. Descarta palabras vacias (stopwords).
 *
 * No aplica lematizacion ni stemming: "model" y "models" son terminos distintos.
 */
public class TokenizadorPropio implements Tokenizador {

    private static final int LONGITUD_MINIMA = 2;

    /** Tambien la usa TokenizadorLucene, para que ambos limpien el texto igual. */
    static final Pattern URL = Pattern.compile(
            "https?://\\S+|www\\.\\S+|\\b[\\w.-]+\\.(com|org|net|io|edu|google)\\b\\S*");

    @Override
    public List<String> tokenizar(String texto) {
        // Pasos 1, 2 y 3
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFKC);
        limpio = limpio.toLowerCase(Locale.ROOT);
        limpio = URL.matcher(limpio).replaceAll(" ");

        // Paso 4: recorrer caracter por caracter armando palabras
        List<String> terminos = new ArrayList<>();
        StringBuilder palabra = new StringBuilder();
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            if (Character.isLetter(c)) {
                palabra.append(c);            // la palabra continua
            } else {
                agregarSiEsValida(palabra.toString(), terminos); // la palabra termino
                palabra.setLength(0);
            }
        }
        agregarSiEsValida(palabra.toString(), terminos); // ultima palabra del texto

        return terminos;
    }

    /** Pasos 5 y 6: longitud minima y palabras vacias. */
    private void agregarSiEsValida(String palabra, List<String> terminos) {
        if (palabra.length() >= LONGITUD_MINIMA && !PalabrasVacias.esPalabraVacia(palabra)) {
            terminos.add(palabra);
        }
    }
}