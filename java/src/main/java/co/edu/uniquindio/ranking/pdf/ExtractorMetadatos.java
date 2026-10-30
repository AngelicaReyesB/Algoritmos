package co.edu.uniquindio.ranking.pdf;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Saca el titulo y los autores de un articulo a partir de su portada (pagina 1).
 *
 * TITULO
 *   1. Se usa el titulo que el PDF guarda en sus propiedades internas, pero
 *      solo si aparece tal cual en el texto de la pagina 1. Asi se descartan
 *      titulos internos con errores ("transform" en vez de "transforms") o que
 *      no son un titulo ("ET-04-2023-0117_proof 609..645").
 *   2. Si no sirve, se usa el texto escrito con la letra mas grande de la
 *      pagina 1, que en estos articulos es el titulo.
 *
 * AUTORES
 *   En todas las editoriales del corpus los autores estan en el renglon que
 *   sigue al titulo. Si ese renglon termina en coma, o el siguiente empieza
 *   con "and", la lista continua en el renglon siguiente.
 *   Luego se separan los nombres y se quitan las marcas de afiliacion:
 *   numeros (Xia3), letras sueltas (Chiu a,b), asteriscos y simbolos.
 *
 * No depende de PDFBox: recibe textos ya leidos por LectorPdf, por eso se
 * puede probar con textos de ejemplo.
 */
@Component
public class ExtractorMetadatos {

    /** Separador de autores, el mismo que usa data/metadatos.json. */
    public static final String SEPARADOR_AUTORES = "; ";

    /** Lo que se extrae de cada articulo. autores es null si no se encontraron. */
    public record Metadatos(String titulo, String autores) {
    }

    /** Extrae titulo y autores de la portada que leyo LectorPdf. */
    public Metadatos extraer(LectorPdf.Portada portada) {
        String titulo = extraerTitulo(portada.tituloInterno(), portada.texto(), portada.textoLetraMasGrande());
        return new Metadatos(titulo, extraerAutores(portada.texto(), titulo));
    }

    public String extraerTitulo(String tituloInterno, String textoPagina, String textoLetraMasGrande) {
        if (tituloInterno != null && !tituloInterno.isBlank()
                && normalizar(textoPagina).contains(normalizar(tituloInterno))) {
            return limpiarEspacios(tituloInterno);
        }
        return limpiarEspacios(textoLetraMasGrande);
    }

    /** Devuelve los autores separados por "; ", o null si no se encuentran. */
    public String extraerAutores(String textoPagina, String titulo) {
        // PDFBox 3 deja espacios especiales (\u00A0) alrededor de "and" en Springer
        String[] lineas = textoPagina.replace('\u00A0', ' ').split("\n");
        int finTitulo = lineaDondeTerminaElTitulo(lineas, titulo);
        if (finTitulo == -1 || finTitulo + 1 >= lineas.length) {
            return null;
        }

        // Renglon de autores, y los siguientes si la lista continua
        int i = finTitulo + 1;
        StringBuilder renglon = new StringBuilder(lineas[i]);
        while (i + 1 < lineas.length
                && (lineas[i].trim().endsWith(",") || lineas[i + 1].trim().startsWith("and "))) {
            i++;
            renglon.append(" ").append(lineas[i]);
        }

        List<String> autores = separarAutores(renglon.toString());
        return autores.isEmpty() ? null : String.join(SEPARADOR_AUTORES, autores);
    }

    /**
     * Busca el titulo en el texto (normalizado) y devuelve el numero del
     * renglon donde termina. Se toma la ULTIMA aparicion porque en MDPI el
     * titulo tambien sale antes, dentro del bloque "Citation:".
     * Devuelve -1 si no lo encuentra.
     */
    static int lineaDondeTerminaElTitulo(String[] lineas, String titulo) {
        String buscado = normalizar(titulo);
        if (buscado.isEmpty()) {
            return -1;
        }

        // fin[k] = largo del texto normalizado desde el renglon 0 hasta el k
        StringBuilder acumulado = new StringBuilder();
        int[] fin = new int[lineas.length];
        for (int k = 0; k < lineas.length; k++) {
            acumulado.append(normalizar(lineas[k]));
            fin[k] = acumulado.length();
        }

        int inicio = acumulado.lastIndexOf(buscado);
        if (inicio == -1) {
            return -1;
        }
        int ultimaLetra = inicio + buscado.length();
        for (int k = 0; k < lineas.length; k++) {
            if (fin[k] >= ultimaLetra) {
                return k;
            }
        }
        return -1;
    }

    /**
     * Separa un renglon de autores en nombres.
     * "Qi Xia3 , Xiaojing Weng2 and Thomas K.F. Chiu1,5,6*" -> [Qi Xia, Xiaojing Weng, Thomas K.F. Chiu]
     */
    static List<String> separarAutores(String renglon) {
        List<String> autores = new ArrayList<>();
        // Separadores entre autores: coma, "and", barra vertical y punto medio
        for (String parte : renglon.split(",|\\sand\\s|\\||·")) {
            String nombre = limpiarNombre(parte);
            if (pareceNombre(nombre)) {
                autores.add(nombre);
            }
        }
        return autores;
    }

    static String limpiarNombre(String parte) {
        String nombre = parte
                // Algunos PDF escriben la tilde aparte: "Gaševic´" -> "Gašević"
                .replace("\u00B4", "\u0301")
                .replace("\u00A8", "\u0308");
        nombre = Normalizer.normalize(nombre, Normalizer.Form.NFC);
        // Solo quedan letras, puntos, guiones y apostrofes (se van numeros, *, simbolos)
        nombre = nombre.replaceAll("[^\\p{L}.'\\- ]", " ");
        // Letras minusculas sueltas = marcas de afiliacion ("Chiu a")
        nombre = nombre.replaceAll("(?<=^| )\\p{Ll}(?= |$)", " ");
        return limpiarEspacios(nombre);
    }

    /** Un nombre tiene al menos dos letras y empieza con mayuscula. */
    static boolean pareceNombre(String nombre) {
        return nombre.length() >= 2 && Character.isUpperCase(nombre.charAt(0));
    }

    /**
     * Para comparar textos: deja solo letras y numeros en minuscula.
     * NFKC convierte ligaduras como "ﬁ" en "fi".
     * "Human-centred learning: A review" -> "humancentredlearningareview"
     */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String normal = Normalizer.normalize(texto, Normalizer.Form.NFKC).toLowerCase();
        return normal.replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** Cambia los espacios especiales y los saltos de linea por un solo espacio. */
    static String limpiarEspacios(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}