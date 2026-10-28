package co.edu.uniquindio.ranking.pdf;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aisla el abstract a partir del texto de un PDF.
 *
 * No depende de PDFBox: recibe un String. Asi se puede probar con textos
 * de ejemplo, sin necesidad de archivos PDF.
 *
 * Los 20 articulos del corpus vienen de cinco editoriales (Elsevier,
 * Springer, MDPI, Wiley y Emerald). Cada una arma la primera pagina de
 * forma distinta; las reglas de abajo cubren esos formatos.
 */
@Component
public class ExtractorAbstract {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    /**
     * Donde empieza el abstract.
     * "\\s?" entre letras acepta tambien la forma espaciada de Elsevier: "A B S T R A C T".
     */
    private static final Pattern INICIO = Pattern.compile(
            "\\ba\\s?b\\s?s\\s?t\\s?r\\s?a\\s?c\\s?t\\b|\\bresumen\\b", FLAGS);

    /** Donde termina el abstract: lo primero que aparezca de esta lista. */
    private static final Pattern FIN = Pattern.compile(
            "\\n\\s*k\\s?e\\s?y\\s?w\\s?o\\s?r\\s?d\\s?s?\\b"  // linea que empieza con Keywords, Key words o K E Y W O R D S (Wiley)
                    + "|\\n\\s*palabras clave\\b"
                    + "|\\b1\\s*\\.\\s*introduction\\b"             // "1. Introduction", tambien pegado: "texto.1. Introduction"
                    + "|\\n\\s*introduction\\b|\\n\\s*introducci[oó]n\\b"   // linea que empieza con Introduction
                    + "|(?<=\\.) {3,}\\n",                          // Elsevier: el abstract termina en ".   " (tres espacios)
            FLAGS);

    /** Una linea de palabras clave en la columna "Article info" de Elsevier es corta. */
    private static final int LARGO_LINEA_PALABRA_CLAVE = 45;

    /** Si no se encuentra el fin, se toman como maximo estos caracteres. */
    private static final int LONGITUD_MAXIMA = 2500;

    /**
     * Devuelve el abstract, o null si el texto no tiene la palabra "Abstract".
     */
    public String extraerAbstract(String texto) {
        String limpio = normalizar(texto);

        Matcher inicio = INICIO.matcher(limpio);
        if (!inicio.find()) {
            return null;
        }
        String resto = saltarPalabrasClave(limpio.substring(inicio.end()));

        Matcher fin = FIN.matcher(resto);
        String abstractTexto = fin.find()
                ? resto.substring(0, fin.start())
                : resto.substring(0, Math.min(LONGITUD_MAXIMA, resto.length()));

        abstractTexto = unirPalabrasCortadas(abstractTexto);
        abstractTexto = abstractTexto.replaceAll("\\s+", " "); // un solo espacio
        abstractTexto = recortarBordes(abstractTexto);
        return abstractTexto.isEmpty() ? null : abstractTexto;
    }

    /**
     * En algunos articulos de Elsevier la lista de palabras clave queda
     * justo despues del titulo "A B S T R A C T":
     *
     *   Keywords:
     *   Human-centered AI
     *   AI in education
     *   The rapid expansion of Learning Analytics ...   <- aqui empieza el abstract
     *
     * Se salta la linea "Keywords:" y las lineas cortas que la siguen.
     */
    private String saltarPalabrasClave(String resto) {
        String[] lineas = resto.strip().split("\n");
        if (!lineas[0].toLowerCase().startsWith("keywords")) {
            return resto;
        }
        int i = 1;
        while (i < lineas.length && lineas[i].strip().length() < LARGO_LINEA_PALABRA_CLAVE) {
            i++;
        }
        return String.join("\n", Arrays.copyOfRange(lineas, i, lineas.length));
    }

    /**
     * Une las palabras que el PDF corta al final de una linea.
     *
     * 1. Con guion visible: "gener-\native" -> "generative".
     * 2. Elsevier: PDFBox no lee ese guion, pero en esos PDF cada linea normal
     *    termina con un espacio. Si la mayoria de lineas termina en espacio,
     *    una linea que termina sin espacio es una palabra cortada:
     *    "the in\nclusion" -> "the inclusion".
     */
    private String unirPalabrasCortadas(String texto) {
        texto = texto.replaceAll("-\\n(?=[a-z])", "");

        String[] lineas = texto.split("\n");
        int conEspacioFinal = 0;
        for (String linea : lineas) {
            if (linea.endsWith(" ")) {
                conEspacioFinal++;
            }
        }
        if (conEspacioFinal > lineas.length / 2) {
            texto = texto.replaceAll("(?<=[a-z])\\n(?=[a-z])", "");
        }
        return texto;
    }

    /** Convierte ligaduras tipograficas en letras normales: "ﬁ" -> "fi". */
    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFKC);
    }

    /** Quita espacios y signos sueltos al inicio y al final (" : . - — ∗ *"). */
    private String recortarBordes(String texto) {
        String signos = " :.-—∗*";
        int inicio = 0;
        int fin = texto.length();
        while (inicio < fin && signos.indexOf(texto.charAt(inicio)) >= 0) {
            inicio++;
        }
        while (fin > inicio && signos.indexOf(texto.charAt(fin - 1)) >= 0) {
            fin--;
        }
        return texto.substring(inicio, fin);
    }
}