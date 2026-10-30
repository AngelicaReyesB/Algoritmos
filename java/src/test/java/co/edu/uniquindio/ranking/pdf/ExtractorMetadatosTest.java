package co.edu.uniquindio.ranking.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas con textos de ejemplo que imitan la pagina 1 de cada editorial.
 */
class ExtractorMetadatosTest {

    private final ExtractorMetadatos extractor = new ExtractorMetadatos();

    // ---------- Titulo ----------

    @Test
    void usaElTituloInternoSiApareceEnLaPagina() {
        String texto = "Computers and Education\nAI technologies for education: Recent research \n& future directions \nKe Zhang a,*";
        assertEquals("AI technologies for education: Recent research & future directions",
                extractor.extraerTitulo(" AI technologies for education: Recent research & future directions",
                        texto, "Computers and Education"));
    }

    @Test
    void tituloInternoConErrorSeCambiaPorLaLetraMasGrande() {
        // El PDF 11 guarda "transform" en vez de "transforms"
        String texto = "A scoping review on how generative artificial \nintelligence transforms assessment\nQi Xia3";
        assertEquals("A scoping review on how generative artificial intelligence transforms assessment",
                extractor.extraerTitulo("A scoping review on how generative artificial intelligence transform assessment",
                        texto, "A scoping review on\u00A0how generative artificial  intelligence transforms assessment "));
    }

    @Test
    void sinTituloInternoUsaLaLetraMasGrande() {
        assertEquals("Artificial intelligence for education",
                extractor.extraerTitulo(null, "Artificial intelligence\nfor education\nJyoti Gavhane",
                        "Artificial intelligence for education "));
    }

    @Test
    void lasLigadurasDelTextoNoImpidenEncontrarElTitulo() {
        // En el texto, "fi" viene como un solo caracter (ligadura)
        String texto = "Application and theory gaps during the rise of Arti\uFB01cial Intelligence\nin Education";
        assertEquals("Application and theory gaps during the rise of Artificial Intelligence in Education",
                extractor.extraerTitulo("Application and theory gaps during the rise of Artificial Intelligence in Education",
                        texto, "Computers and Education"));
    }

    // ---------- Autores ----------

    @Test
    void elsevierQuitaLasLetrasDeAfiliacion() {
        String texto = "Systematic literature review\non education \nThomas K.F. Chiu a,*, Qi Xia b, Miaoting Cheng c \n"
                + "a Department of Curriculum";
        assertEquals("Thomas K.F. Chiu; Qi Xia; Miaoting Cheng",
                extractor.extraerAutores(texto, "Systematic literature review on education"));
    }

    @Test
    void siElRenglonTerminaEnComaLosAutoresSiguenAbajo() {
        String texto = "Opportunities of AI \nSrecko Joksimovic a,b,*, Dirk Ifenthaler c,d, Maarten De Laat a,b, \n"
                + "George Siemens a,b \na Education Futures";
        assertEquals("Srecko Joksimovic; Dirk Ifenthaler; Maarten De Laat; George Siemens",
                extractor.extraerAutores(texto, "Opportunities of AI"));
    }

    @Test
    void siElSiguienteRenglonEmpiezaConAndLosAutoresSiguen() {
        String texto = "A Systematic Review\nBayode Ogunleye 1,* , Oluwaseun Ajao 2, Olakunle Olayinka 3\n"
                + "and Hemlata Sharma 4\n1 School of Architecture";
        assertEquals("Bayode Ogunleye; Oluwaseun Ajao; Olakunle Olayinka; Hemlata Sharma",
                extractor.extraerAutores(texto, "A Systematic Review"));
    }

    @Test
    void springerQuitaLosNumerosYAsteriscos() {
        String texto = "A meta systematic review\nMelissa Bond1,2,3*  , Hassan Khosravi4, Sin Wang Chong3,8 and George Siemens5 \n"
                + "Abstract";
        assertEquals("Melissa Bond; Hassan Khosravi; Sin Wang Chong; George Siemens",
                extractor.extraerAutores(texto, "A meta systematic review"));
    }

    @Test
    void separaElAndConEspaciosEspeciales() {
        // En Springer, PDFBox 3 entrega "and" rodeado de \u00A0 en vez de espacios normales
        String texto = "Titulo\nSin Wang Chong3,8\u00A0and\u00A0George Siemens5\nAbstract";
        assertEquals("Sin Wang Chong; George Siemens", extractor.extraerAutores(texto, "Titulo"));
    }

    @Test
    void separaConBarraYConPuntoMedio() {
        assertEquals("Tamar Mikeladze; Paulien C. Meijer; Roald P. Verhoeff",
                extractor.extraerAutores("Titulo\nTamar Mikeladze1,2 \u2003|   Paulien C. Meijer1 |   Roald P. Verhoeff1\nx",
                        "Titulo"));
        assertEquals("Fan Ouyang; Tuan Anh Dinh; Weiqi Xu",
                extractor.extraerAutores("Titulo\nFan\u00A0Ouyang1 \u00B7 Tuan\u00A0Anh\u00A0Dinh1 \u00B7 Weiqi\u00A0Xu1\nx",
                        "Titulo"));
    }

    @Test
    void uneLaTildeEscritaAparte() {
        // "Gaševic´": la tilde viene como un caracter separado despues de la c
        assertEquals("Dragan Gašević; Roberto Martinez-Maldonado",
                extractor.extraerAutores("Titulo\nDragan Gaševic\u00B4 a, Roberto Martinez-Maldonado a\nx", "Titulo"));
    }

    @Test
    void enMdpiUsaLaUltimaAparicionDelTitulo() {
        // El titulo sale primero dentro del bloque "Citation:"
        String texto = "Citation: Lo, C.K. What Is the Impact\nof ChatGPT? Educ. Sci.\nReview\n"
                + "What Is the Impact of ChatGPT?\nChung Kwan Lo\nDepartment of Mathematics";
        assertEquals("Chung Kwan Lo", extractor.extraerAutores(texto, "What Is the Impact of ChatGPT?"));
    }

    @Test
    void siElTituloNoEstaEnElTextoDevuelveNull() {
        assertNull(extractor.extraerAutores("Otro texto\nJuan Perez", "Titulo que no aparece"));
    }
}