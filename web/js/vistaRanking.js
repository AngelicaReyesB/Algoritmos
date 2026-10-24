/**
 * Vista "Top 15": tabla con el ranking de cada algoritmo y grafico de barras.
 *
 * El grafico se dibuja de dos formas para compararlas:
 * - Sin librerias: cada barra es un <div> cuyo ancho es proporcional a la frecuencia.
 * - Con librerias: Chart.js.
 */
import { obtenerRanking } from "./api.js";
import { crearContenedorChartJs, dibujarBarrasChartJs } from "./graficosChartJs.js";
import { crearGraficoBarras, escaparHtml, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-ranking");
const selectorTop = document.getElementById("selector-top");

export async function cargarRanking() {
    contenedor.innerHTML = "<p>Cargando ranking…</p>";
    try {
        const datos = await obtenerRanking(Number(selectorTop.value));
        const primerRanking = Object.values(datos.rankings)[0];
        const elementos = primerRanking.map((t) => ({ etiqueta: t.termino, valor: t.frecuencia }));
        contenedor.innerHTML =
            crearResumen(datos) +
            crearTabla(datos.rankings) +
            crearGrafico(elementos);
        // Chart.js necesita que el <canvas> ya este en la pagina
        dibujarBarrasChartJs("grafico-ranking-chartjs", elementos);
    } catch (error) {
        mostrarError(contenedor, error);
    }
}

/** Datos del corpus y si los tres algoritmos coinciden. */
function crearResumen(datos) {
    const validacion = datos.coinciden
        ? `<span class="insignia insignia--ok">Los 3 algoritmos producen el mismo ranking</span>`
        : `<span class="insignia insignia--error">Los rankings NO coinciden</span>`;
    return `
        <div class="resumen">
            <p>Términos distintos (n): <strong>${datos.terminosDistintos}</strong></p>
            <p>Frecuencia máxima (k): <strong>${datos.frecuenciaMaxima}</strong></p>
            ${validacion}
        </div>`;
}

/**
 * Tabla: una fila por posicion y, por cada algoritmo, el termino y su frecuencia.
 * Si un algoritmo no coincide con el primero en alguna posicion, la celda se marca.
 */
function crearTabla(rankings) {
    const nombres = Object.keys(rankings);
    const referencia = rankings[nombres[0]];

    const encabezados = nombres.map((nombre) => `<th colspan="2">${escaparHtml(nombre)}</th>`).join("");

    const filas = referencia.map((_, i) => {
        const celdas = nombres.map((nombre) => {
            const termino = rankings[nombre][i];
            const esIgual = termino.termino === referencia[i].termino
                && termino.frecuencia === referencia[i].frecuencia;
            const marca = esIgual ? "" : "celda-diferente";
            return `<td class="${marca}">${escaparHtml(termino.termino)}</td>`
                + `<td class="numero ${marca}">${termino.frecuencia}</td>`;
        }).join("");
        return `<tr><td class="numero">${i + 1}</td>${celdas}</tr>`;
    }).join("");

    return `
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead><tr><th>#</th>${encabezados}</tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

/** Los dos graficos de frecuencia, uno al lado del otro. */
function crearGrafico(elementos) {
    return `
        <h3>Frecuencia de los términos</h3>
        <div class="comparacion-graficos">
            <div>
                <h4>Sin librerías (HTML y CSS)</h4>
                ${crearGraficoBarras(elementos)}
            </div>
            <div>
                <h4>Con librerías (Chart.js)</h4>
                ${crearContenedorChartJs("grafico-ranking-chartjs", elementos.length)}
            </div>
        </div>`;
}