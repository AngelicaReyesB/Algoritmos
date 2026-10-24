/**
 * Vista "Articulos": lista de los articulos del corpus con su abstract.
 *
 * Cada abstract va dentro de un <details>: el navegador lo muestra
 * plegado y se despliega al hacer clic, sin necesidad de JavaScript.
 */
import { obtenerArticulos } from "./api.js";
import { escaparHtml, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-articulos");

export async function cargarArticulos() {
    contenedor.innerHTML = "<p>Cargando artículos…</p>";
    try {
        const articulos = await obtenerArticulos();
        contenedor.innerHTML = crearResumen(articulos) + articulos.map(crearArticulo).join("");
    } catch (error) {
        mostrarError(contenedor, error);
    }
}

/** Cantidad de palabras de un texto: se separa por espacios. */
function contarPalabras(texto) {
    return texto.trim().split(/\s+/).length;
}

function crearResumen(articulos) {
    const totalPalabras = articulos.reduce((suma, a) => suma + contarPalabras(a.resumen), 0);
    const manuales = articulos.filter((a) => a.manual).length;
    return `
        <div class="resumen">
            <p>Artículos: <strong>${articulos.length}</strong></p>
            <p>Palabras en los abstracts: <strong>${totalPalabras.toLocaleString("es-CO")}</strong></p>
            <p>Corregidos a mano: <strong>${manuales}</strong></p>
        </div>`;
}

function crearArticulo(articulo) {
    const insignia = articulo.manual
        ? `<span class="insignia insignia--aviso">Corregido a mano</span>`
        : "";
    return `
        <article class="articulo">
            <div class="articulo__encabezado">
                <h3 class="articulo__archivo">${escaparHtml(articulo.archivo)}</h3>
                ${insignia}
            </div>
            <p class="articulo__titulo">Título extraído: ${escaparHtml(articulo.titulo || "(sin título)")}</p>
            <details>
                <summary>Ver abstract (${contarPalabras(articulo.resumen)} palabras)</summary>
                <p class="articulo__resumen">${escaparHtml(articulo.resumen)}</p>
            </details>
        </article>`;
}