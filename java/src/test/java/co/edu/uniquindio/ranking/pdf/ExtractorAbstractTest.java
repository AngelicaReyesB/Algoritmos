package co.edu.uniquindio.ranking.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas con textos de ejemplo que imitan lo que PDFBox devuelve
 * en los casos problematicos encontrados en TEST-03.
 */
class ExtractorAbstractTest {

    private final ExtractorAbstract extractor = new ExtractorAbstract();

    @Test
    void extraeElTextoEntreAbstractEIntroduccion() {
        String texto = "Titulo del articulo\nAutores\nAbstract\nWe propose a new model.\n1 Introduction\nTexto";
        assertEquals("We propose a new model", extractor.extraerAbstract(texto));
    }

    @Test
    void terminaEnPalabrasClave() {
        String texto = "Abstract: Diffusion models work well.\nKeywords: diffusion, images";
        assertEquals("Diffusion models work well", extractor.extraerAbstract(texto));
    }

    @Test
    void sinLaPalabraAbstractDevuelveNull() {
        assertNull(extractor.extraerAbstract("Titulo\nAutores\n1 Introduction\nEl texto no tiene esa seccion"));
    }

    @Test
    void eliminaElSelloDeArxiv() {
        String texto = "Abstract\nLanguage models scale.\narXiv:2001.08361v1 [cs.LG] 23 Jan 2020\n"
                + "1 Introduction";
        assertEquals("Language models scale", extractor.extraerAbstract(texto));
    }

    @Test
    void terminaEnNotaAlPieDeContribucion() {
        String texto = "Abstract\nThe dominant models are recurrent.\n∗Equal contribution. Listing order is random.";
        assertEquals("The dominant models are recurrent", extractor.extraerAbstract(texto));
    }

    @Test
    void terminaEnEjemplosDeFigura() {
        String texto = "Abstract\nChain of thought improves reasoning.\nA: The cafeteria had 23 apples.\nQ: Roger";
        assertEquals("Chain of thought improves reasoning", extractor.extraerAbstract(texto));
    }

    @Test
    void eliminaElAvisoDeCopyrightDeAcm() {
        String texto = "ABSTRACT\nartists paint, while\nPermission to make digital or hard copies of part\n"
                + "of this work. https://doi.org/10.1145/3586183.3606763\nauthors write.\nCCS CONCEPTS\nx";
        assertEquals("artists paint, while authors write", extractor.extraerAbstract(texto));
    }

    @Test
    void normalizaLigadurasYUnePalabrasCortadas() {
        String texto = "Abstract\nAn eﬃcient gener-\native model.\nKeywords: x";
        assertEquals("An efficient generative model", extractor.extraerAbstract(texto));
    }

    @Test
    void tituloEsLaPrimeraLineaNoVacia() {
        assertEquals("Generative Adversarial Nets",
                extractor.extraerTitulo("\n\n  Generative Adversarial Nets \nIan Goodfellow"));
    }
}