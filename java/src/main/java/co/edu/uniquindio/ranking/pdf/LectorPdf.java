package co.edu.uniquindio.ranking.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Lee el texto de un PDF usando Apache PDFBox.
 *
 * Es la unica clase del proyecto que depende de PDFBox: todo el
 * procesamiento del texto se hace despues, en ExtractorAbstract.
 */
@Component
public class LectorPdf {

    /** Solo se leen las primeras paginas: ahi estan el titulo y el abstract. */
    private static final int PAGINAS_A_LEER = 2;

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
}