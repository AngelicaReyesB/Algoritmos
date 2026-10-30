package co.edu.uniquindio.ranking.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el texto de un PDF usando Apache PDFBox.
 *
 * Es la unica clase del proyecto que depende de PDFBox: todo el
 * procesamiento del texto se hace despues, en ExtractorAbstract y
 * ExtractorMetadatos.
 */
@Component
public class LectorPdf {

    /** Solo se leen las primeras paginas: ahi estan el titulo y el abstract. */
    private static final int PAGINAS_A_LEER = 2;

    /**
     * Lo que se necesita de la pagina 1 para sacar titulo y autores.
     *
     * @param tituloInterno       titulo guardado en las propiedades del PDF (puede ser null)
     * @param texto               texto de la pagina 1
     * @param textoLetraMasGrande texto escrito con la letra mas grande de la pagina 1
     */
    public record Portada(String tituloInterno, String texto, String textoLetraMasGrande) {
    }

    public String leerPrimerasPaginas(Path archivo) throws IOException {
        try (PDDocument documento = Loader.loadPDF(archivo.toFile())) {
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setStartPage(1);
            extractor.setEndPage(PAGINAS_A_LEER);
            // Por defecto PDFBox usa el salto de linea del sistema operativo
            // ("\r\n" en Windows). Se fija "\n" para que las expresiones
            // regulares de ExtractorAbstract funcionen igual en cualquier sistema.
            extractor.setLineSeparator("\n");
            return extractor.getText(documento);
        }
    }

    public Portada leerPortada(Path archivo) throws IOException {
        try (PDDocument documento = Loader.loadPDF(archivo.toFile())) {
            String tituloInterno = documento.getDocumentInformation().getTitle();

            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setStartPage(1);
            extractor.setEndPage(1);
            extractor.setLineSeparator("\n");
            String texto = extractor.getText(documento);

            return new Portada(tituloInterno, texto, leerLetraMasGrande(documento));
        }
    }

    /**
     * PDFBox entrega el texto por trozos y cada letra trae su tamano.
     * Se guarda cada trozo con el tamano de su letra mas grande y al final
     * se unen los trozos que tienen el tamano maximo de la pagina.
     */
    private String leerLetraMasGrande(PDDocument documento) throws IOException {
        List<String> trozos = new ArrayList<>();
        List<Float> tamanos = new ArrayList<>();

        PDFTextStripper extractor = new PDFTextStripper() {
            @Override
            protected void writeString(String texto, List<TextPosition> letras) {
                float mayor = 0;
                for (TextPosition letra : letras) {
                    mayor = Math.max(mayor, letra.getFontSizeInPt());
                }
                trozos.add(texto);
                tamanos.add(mayor);
            }
        };
        extractor.setStartPage(1);
        extractor.setEndPage(1);
        extractor.getText(documento); // recorre la pagina y llama a writeString

        float maximo = 0;
        for (float tamano : tamanos) {
            maximo = Math.max(maximo, tamano);
        }

        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < trozos.size(); i++) {
            if (tamanos.get(i) == maximo) {
                resultado.append(trozos.get(i)).append(" ");
            }
        }
        return resultado.toString();
    }
}