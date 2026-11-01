/**
 * Vista "Top 15": terminos mas frecuentes del corpus.
 *
 * - A la izquierda, el grafico de frecuencias. Se puede ver de dos formas
 *   para compararlas: sin librerias (cada barra es un <div>) o con Chart.js.
 * - A la derecha, la tabla con el ranking de los 3 algoritmos de
 *   ordenamiento, para comprobar que producen el mismo orden.
 */
import { obtenerRanking } from "./api.js";
import { crearContenedorChartJs, dibujarBarrasChartJs } from "./graficosChartJs.js";
import { crearGraficoBarras, escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-ranking");
const subtitulo = document.getElementById("resumen-ranking");
const selectorTop = document.getElementById("selector-top");

const ICONO_OK = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" aria-hidden="true"><path d="M5 12l5 5L20 7"></path></svg>';
const ICONO_ERROR = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"></path></svg>';

/** Cantidad de terminos elegida (5, 10, 15 o 20). */
let top = 15;

/** Elementos del grafico, guardados para redibujar Chart.js al cambiar de version. */
let elementosGrafico = [];

export async function cargarRanking() {
    mostrarCargando(contenedor, "Cargando ranking…");
    try {
        const datos = await obtenerRanking(top);
        const primerRanking = Object.values(datos.rankings)[0];
        elementosGrafico = primerRanking.map((t) => ({ etiqueta: t.termino, valor: t.frecuencia }));

        subtitulo.textContent =
            `Top ${top} de ${datos.terminosDistintos} términos distintos · frecuencia máxima ${datos.frecuenciaMaxima}`;
        contenedor.innerHTML = `
            <div class="rejilla-top">
                ${crearPanelGrafico(elementosGrafico)}
                <div class="columna">
                    ${crearAvisoCoinciden(datos.rankings)}
                    ${crearTabla(datos.rankings)}
                    ${crearCriterio()}
                </div>
            </div>`;
    } catch (error) {
        subtitulo.textContent = "";
        mostrarError(contenedor, error);
    }
}

/** Tarjeta del grafico, con el selector "HTML y CSS" / "Chart.js". */
function crearPanelGrafico(elementos) {
    return `
        <section class="panel">
            <div class="panel__encabezado">
                <h3>Frecuencia por término</h3>
                <div class="segmentado segmentado--pequeno" role="radiogroup" aria-label="Versión del gráfico">
                    <button type="button" role="radio" aria-checked="true" data-grafico="html">HTML y CSS</button>
                    <button type="button" role="radio" aria-checked="false" data-grafico="chartjs">Chart.js</button>
                </div>
            </div>
            <div class="panel__cuerpo">
                <div id="grafico-html">${crearGraficoBarras(elementos)}</div>
                <div id="grafico-chartjs" hidden>${crearContenedorChartJs("grafico-ranking-chartjs", elementos.length)}</div>
                <p class="nota nota--pie">Ocurrencias de cada término en los 20 abstracts.
                    La versión HTML y CSS no usa librerías; la otra usa Chart.js.</p>
            </div>
        </section>`;
}

/** Aviso verde si los 3 algoritmos coinciden en todas las posiciones; rojo si no. */
function crearAvisoCoinciden(rankings) {
    const nombres = Object.keys(rankings);
    const referencia = rankings[nombres[0]];
    const iguales = referencia.filter((_, i) => coincidenEn(rankings, i)).length;

    if (iguales === referencia.length) {
        return `
            <div class="aviso aviso--ok">
                ${ICONO_OK}
                <p><strong>Los tres algoritmos producen el mismo orden.</strong>
                ${iguales} de ${referencia.length} posiciones coinciden.</p>
            </div>`;
    }
    return `
        <div class="aviso aviso--error">
            ${ICONO_ERROR}
            <p><strong>Los rankings no coinciden.</strong>
            Solo ${iguales} de ${referencia.length} posiciones son iguales.</p>
        </div>`;
}

/** true si en la posicion i los 3 algoritmos tienen el mismo termino y la misma frecuencia. */
function coincidenEn(rankings, i) {
    const nombres = Object.keys(rankings);
    const referencia = rankings[nombres[0]][i];
    return nombres.every((nombre) => rankings[nombre][i].termino === referencia.termino
        && rankings[nombre][i].frecuencia === referencia.frecuencia);
}

/** Tabla: una fila por posicion y una columna por algoritmo. */
function crearTabla(rankings) {
    const nombres = Object.keys(rankings);
    const encabezados = nombres.map((nombre) => `<th>${escaparHtml(nombre)}</th>`).join("");

    const filas = rankings[nombres[0]].map((_, i) => {
        const celdas = nombres.map((nombre) => {
            const termino = rankings[nombre][i];
            return `<td><span class="mono">${escaparHtml(termino.termino)}</span>
                        <span class="dato-suave">${termino.frecuencia}</span></td>`;
        }).join("");
        const coincide = coincidenEn(rankings, i)
            ? `<span class="texto-ok">Sí</span>`
            : `<span class="texto-error">No</span>`;
        return `<tr><td class="numero dato-suave">${i + 1}</td>${celdas}<td>${coincide}</td></tr>`;
    }).join("");

    return `
        <div class="tabla-desplazable tabla-desplazable--tarjeta">
            <table class="tabla">
                <caption>Comparación de ordenamientos</caption>
                <thead><tr><th class="numero">#</th>${encabezados}<th>Coinciden</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

function crearCriterio() {
    return `
        <aside class="recuadro">
            <p class="recuadro__titulo">Criterio de orden</p>
            <p>Frecuencia de mayor a menor. Los empates conservan el orden de la primera aparición
            del término en el corpus, porque los tres algoritmos son estables.
            Complejidad: Counting <span class="mono">O(n + k)</span> ·
            Radix <span class="mono">O(d·(n + b))</span> ·
            Bucket <span class="mono">O(n)</span> promedio.</p>
        </aside>`;
}

/** Botones 5 / 10 / 15 / 20: cambian la cantidad de terminos y recargan. */
selectorTop.addEventListener("click", (evento) => {
    const boton = evento.target.closest("button");
    if (!boton) {
        return;
    }
    top = Number(boton.dataset.top);
    selectorTop.querySelectorAll("button").forEach((b) => b.setAttribute("aria-checked", b === boton));
    cargarRanking();
});

/** Selector "HTML y CSS" / "Chart.js" del grafico. */
contenedor.addEventListener("click", (evento) => {
    const boton = evento.target.closest("[data-grafico]");
    if (!boton) {
        return;
    }
    const usarChartJs = boton.dataset.grafico === "chartjs";
    boton.parentElement.querySelectorAll("button").forEach((b) => b.setAttribute("aria-checked", b === boton));
    document.getElementById("grafico-html").hidden = usarChartJs;
    document.getElementById("grafico-chartjs").hidden = !usarChartJs;
    // Chart.js se dibuja cuando su contenedor ya es visible, para que tome bien el tamano
    if (usarChartJs) {
        dibujarBarrasChartJs("grafico-ranking-chartjs", elementosGrafico);
    }
});