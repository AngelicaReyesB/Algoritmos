/**
 * Vista "Articulos": lista de los articulos del corpus.
 *
 * Cada articulo es una tarjeta con su numero, titulo, autores y archivo.
 * El abstract esta oculto y se muestra con el boton "Ver abstract".
 */
import { obtenerArticulos } from "./api.js";
import { escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-articulos");
const resumen = document.getElementById("resumen-articulos");
const nota = document.getElementById("nota-articulos");

/** Flecha del boton: apunta a la derecha y gira hacia abajo al abrir (por CSS). */
const ICONO_FLECHA = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M9 6l6 6-6 6"></path></svg>';

export async function cargarArticulos() {
    mostrarCargando(contenedor, "Cargando artículos…");
    try {
        const articulos = await obtenerArticulos();
        mostrarResumen(articulos);
        contenedor.innerHTML = `<ol class="lista-articulos">${articulos.map(crearArticulo).join("")}</ol>`;
    } catch (error) {
        resumen.textContent = "";
        mostrarError(contenedor, error);
    }
}

/** Cantidad de palabras de un texto: se separa por espacios. */
function contarPalabras(texto) {
    return texto.trim().split(/\s+/).length;
}

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Texto debajo del titulo de la vista y nota de la derecha. */
function mostrarResumen(articulos) {
    const totalPalabras = articulos.reduce((suma, a) => suma + contarPalabras(a.resumen), 0);
    const manuales = articulos.filter((a) => a.manual).length;
    resumen.textContent =
        `${articulos.length} artículos · ${articulos.length - manuales} abstracts extraídos automáticamente · ` +
        `${totalPalabras.toLocaleString("es-CO")} palabras`;
    nota.textContent = manuales === 1 ? "1 abstract corregido a mano" : `${manuales} abstracts corregidos a mano`;
}

function crearArticulo(articulo) {
    const idAbstract = `abstract-${articulo.id}`;
    const insignia = articulo.manual
        ? `<span class="insignia insignia--aviso">Corregido a mano</span>`
        : "";
    // Los autores llegan separados por "; " y se muestran separados por " · "
    const autores = (articulo.autores || "(sin autores)").split("; ").join(" · ");

    return `
        <li class="tarjeta-articulo">
            <div class="tarjeta-articulo__fila">
                <span class="tarjeta-articulo__numero">${numeroDelArticulo(articulo)}</span>
                <div class="tarjeta-articulo__datos">
                    <h3 class="tarjeta-articulo__titulo">${escaparHtml(articulo.titulo || "(sin título)")}</h3>
                    <p class="tarjeta-articulo__autores">${escaparHtml(autores)}</p>
                    <div class="tarjeta-articulo__meta">
                        <span class="mono">${escaparHtml(articulo.archivo)}</span>
                        ${insignia}
                    </div>
                </div>
                <button class="boton-abstract" type="button" aria-expanded="false" aria-controls="${idAbstract}">
                    ${ICONO_FLECHA}<span>Ver abstract</span>
                </button>
            </div>
            <div id="${idAbstract}" class="tarjeta-articulo__abstract" hidden>
                <p class="etiqueta">Abstract</p>
                <p class="tarjeta-articulo__texto">${escaparHtml(articulo.resumen)}</p>
                <p class="tarjeta-articulo__palabras">${contarPalabras(articulo.resumen)} palabras</p>
            </div>
        </li>`;
}

/**
 * Un solo evento para todos los botones (delegacion): al hacer clic en
 * "Ver abstract" se muestra u oculta el abstract de ese articulo.
 */
contenedor.addEventListener("click", (evento) => {
    const boton = evento.target.closest(".boton-abstract");
    if (!boton) {
        return;
    }
    const abierto = boton.getAttribute("aria-expanded") === "true";
    boton.setAttribute("aria-expanded", !abierto);
    boton.querySelector("span").textContent = abierto ? "Ver abstract" : "Ocultar abstract";
    document.getElementById(boton.getAttribute("aria-controls")).hidden = abierto;
});