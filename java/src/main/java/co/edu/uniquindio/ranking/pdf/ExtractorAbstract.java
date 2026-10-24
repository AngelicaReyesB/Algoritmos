package co.edu.uniquindio.ranking.pdf;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aisla el abstract (y un titulo aproximado) a partir del texto de un PDF.
 *
 * No depende de PDFBox: recibe un String. Asi se puede probar con textos
 * de ejemplo, sin necesidad de archivos PDF.
 *
 * Es la misma logica de la version Python (python/src/extract.py).
 */
@Component
public class ExtractorAbstract {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    /** Donde empieza el abstract. */
    private static final Pattern INICIO = Pattern.compile("\\babstract\\b|\\bresumen\\b", FLAGS);

    /** Donde termina el abstract: lo primero que aparezca de esta lista. */
    private static final Pattern FIN = Pattern.compile(
            "\\bkey\\s?words?\\b|\\bindex terms\\b|\\bpalabras clave\\b"
                    + "|\\n\\s*(?:1|I)\\s*[.\\-]?\\s*introduction\\b|\\bintroduction\\b|\\bintroducci[oó]n\\b"
                    + "|\\bccs\\s+concepts\\b"                    // formato ACM
                    + "|\\bequal contribution\\b"                 // nota al pie de autores
                    + "|\\b\\d+(?:st|nd|rd|th)\\s+conference on\\b" // nota al pie de la conferencia
                    + "|\\bpreprint\\b"                           // "Preprint. Under review."
                    + "|\\n\\s*contents\\s*\\n"                   // indice de contenidos
                    + "|\\bfig(?:ure|\\.)\\s*1\\s*[:.]"           // leyenda de la Figura 1
                    + "|\\n\\s*[QA]:\\s",                         // ejemplos de una figura (Q: / A:)
            FLAGS);

    /** Sello lateral que arXiv agrega en la primera pagina. */
    private static final Pattern SELLO_ARXIV = Pattern.compile(
            "arXiv:\\d{4}\\.\\d{4,5}(?:v\\d+)?\\s*\\[[^\\]]+\\]\\s*\\d{1,2}\\s+\\w{3}\\s+\\d{4}");

    /** Aviso de copyright de ACM que interrumpe el abstract. */
    private static final Pattern PERMISO_ACM = Pattern.compile(
            "Permission to make digital or hard copies.*?https?://doi\\.org/\\S+",
            FLAGS | Pattern.DOTALL);

    /** Si no se encuentra el fin, se toman como maximo estos caracteres. */
    private static final int LONGITUD_MAXIMA = 2500;

    /**
     * Devuelve el abstract, o null si el texto no tiene la palabra "Abstract".
     */
    public String extraerAbstract(String texto) {
        String limpio = normalizar(texto);
        limpio = SELLO_ARXIV.matcher(limpio).replaceAll(" ");
        limpio = PERMISO_ACM.matcher(limpio).replaceAll(" ");

        Matcher inicio = INICIO.matcher(limpio);
        if (!inicio.find()) {
            return null;
        }
        String resto = limpio.substring(inicio.end());

        Matcher fin = FIN.matcher(resto);
        String abstractTexto = fin.find()
                ? resto.substring(0, fin.start())
                : resto.substring(0, Math.min(LONGITUD_MAXIMA, resto.length()));

        abstractTexto = abstractTexto.replaceAll("-\\n(?=[a-z])", ""); // une palabras cortadas
        abstractTexto = abstractTexto.replaceAll("\\s+", " ");          // un solo espacio
        abstractTexto = recortarBordes(abstractTexto);
        return abstractTexto.isEmpty() ? null : abstractTexto;
    }

    /**
     * Titulo aproximado: la primera linea no vacia.
     * Limitacion conocida: en algunos PDF la primera linea no es el titulo.
     */
    public String extraerTitulo(String texto) {
        for (String linea : normalizar(texto).split("\\n")) {
            if (!linea.isBlank()) {
                return linea.strip();
            }
        }
        return "";
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