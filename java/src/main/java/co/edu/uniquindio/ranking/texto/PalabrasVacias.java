package co.edu.uniquindio.ranking.texto;

import java.util.HashSet;
import java.util.Set;
/**
 * Palabras vacias (stopwords): palabras muy frecuentes que no aportan
 * informacion sobre el tema del texto y se eliminan antes de contar.
 *
 * Hay dos listas:
 * - BASE: articulos, preposiciones, pronombres y conjunciones en ingles y espanol.
 * - DOMINIO: palabras que aparecen en casi cualquier abstract cientifico,
 *   sin importar el tema ("paper", "results", "approach"...).
 *
 * Las listas las definio el equipo y se justifican en el informe.
 */
public final class PalabrasVacias {

    private static final Set<String> BASE = Set.of(
            // Ingles
            "a", "about", "above", "after", "again", "all", "also", "am", "an", "and", "any",
            "are", "as", "at", "be", "because", "been", "before", "being", "between", "both",
            "but", "by", "can", "could", "did", "do", "does", "doing", "during", "each", "few",
            "for", "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers",
            "him", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
            "may", "me", "more", "most", "my", "no", "nor", "not", "of", "off", "on", "once",
            "only", "or", "other", "our", "out", "over", "own", "same", "she", "should", "so",
            "some", "such", "than", "that", "the", "their", "them", "then", "there", "these",
            "they", "this", "those", "through", "to", "too", "under", "until", "up", "us", "very",
            "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why",
            "will", "with", "would", "you", "your", "among", "within", "without", "via",
            // Espanol
            "de", "la", "que", "el", "en", "y", "los", "se", "del", "las", "un", "por", "con",
            "una", "su", "para", "es", "al", "lo", "como", "mas", "pero", "sus", "le", "ya",
            "este", "si", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "tambien",
            "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno",
            "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "esto", "antes", "algunos"
    );

    private static final Set<String> DOMINIO = Set.of(
            "paper", "study", "studies", "result", "results", "approach", "approaches",
            "show", "shows", "shown", "use", "used", "uses", "using", "based",
            "propose", "proposed", "present", "presents", "work", "however"
    );

    private PalabrasVacias() {
        // Clase de utilidad: no se crean objetos de ella
    }

    /** Todas las palabras vacias (base + dominio). La usa TokenizadorLucene. */
    public static Set<String> todas() {
        Set<String> todas = new HashSet<>(BASE);
        todas.addAll(DOMINIO);
        return todas;
    }

    /** Devuelve true si la palabra (en minusculas) debe descartarse. */
    public static boolean esPalabraVacia(String palabra) {
        return BASE.contains(palabra) || DOMINIO.contains(palabra);
    }
}