package co.edu.uniquindio.ranking.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas con textos de ejemplo que imitan lo que PDFBox devuelve
 * para cada formato de editorial del corpus.
 */
class ExtractorAbstractTest {

    private final ExtractorAbstract extractor = new ExtractorAbstract();

    @Test
    void sinLaPalabraAbstractDevuelveNull() {
        assertNull(extractor.extraerAbstract("Titulo\nAutores\n1. Introduction\nEl texto no tiene esa seccion"));
    }

    @Test
    void springerTerminaEnPalabrasClave() {
        String texto = "REVIEW ARTICLE\nTitulo\nAbstract \nGenerative AI provides opportunities.\n"
                + "Keywords:  Generative artificial intelligence, Scoping review";
        assertEquals("Generative AI provides opportunities", extractor.extraerAbstract(texto));
    }

    @Test
    void mdpiConDosPuntosDespuesDeAbstract() {
        String texto = "Review\nTitulo\nAbstract: The use of GenAI is debated.\nKeywords: artificial intelligence";
        assertEquals("The use of GenAI is debated", extractor.extraerAbstract(texto));
    }

    @Test
    void laPalabraKeywordsDentroDelAbstractNoLoCorta() {
        String texto = "Abstract: Trends in citations, keywords, and co-authorship.\nKeywords: x";
        assertEquals("Trends in citations, keywords, and co-authorship", extractor.extraerAbstract(texto));
    }

    @Test
    void elsevierConAbstractEspaciado() {
        String texto = "A R T I C L E  I N F O   \nKeywords: \nAI literacy \nA B S T R A C T   \n"
                + "Since AI is finding its way into everyday life. \nMore text.   \n1. Introduction \nx";
        assertEquals("Since AI is finding its way into everyday life. More text",
                extractor.extraerAbstract(texto));
    }

    @Test
    void elsevierTerminaEnTresEspaciosSinIntroduccion() {
        String texto = "A B S T R A C T   \nThis article reports a review. \nOf 40 studies.   \n"
                + "Since Alan Turing first articulated";
        assertEquals("This article reports a review. Of 40 studies", extractor.extraerAbstract(texto));
    }

    @Test
    void elsevierSaltaLasPalabrasClaveDespuesDelTitulo() {
        String texto = "A R T I C L E I N F O A B S T R A C T\nKeywords:\nHuman-centered AI\nAI in education\n"
                + "The rapid expansion of Learning Analytics offers new systems for teachers and students.\n"
                + "1. Introduction";
        assertEquals("The rapid expansion of Learning Analytics offers new systems for teachers and students",
                extractor.extraerAbstract(texto));
    }

    @Test
    void introduccionPegadaAlFinalDelAbstract() {
        String texto = "A B S T R A C T\nWe highlight the new journal Computers & Education.1. Introduction\nx";
        assertEquals("We highlight the new journal Computers & Education", extractor.extraerAbstract(texto));
    }

    @Test
    void wileyTerminaEnPalabrasClaveEspaciadas() {
        String texto = "Abstract\nRecent literature underscores the need.\nK E Y W O R D S\nAI competence";
        assertEquals("Recent literature underscores the need", extractor.extraerAbstract(texto));
    }

    @Test
    void unePalabrasCortadasConGuion() {
        String texto = "Abstract\nAn eﬃcient gener-\native model.\nKeywords: x";
        assertEquals("An efficient generative model", extractor.extraerAbstract(texto));
    }

    @Test
    void elsevierUnePalabrasCortadasSinGuion() {
        String texto = "A B S T R A C T   \nFifty-nine studies met the in\nclusion criteria. \nWe coded them. \n"
                + "Then we wrote.   \n1. Introduction";
        assertEquals("Fifty-nine studies met the inclusion criteria. We coded them. Then we wrote",
                extractor.extraerAbstract(texto));
    }
}