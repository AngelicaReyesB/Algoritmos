/**
 * Vista "Tiempos": ejecuta la medicion en el backend y muestra los resultados.
 *
 * La medicion tarda unos segundos (la extraccion de los PDF se repite 5 veces),
 * por eso no se ejecuta al abrir la pagina sino al presionar el boton.
 */
import { obtenerTiempos } from "./api.js";
import { crearGraficosAlternables, crearSelectorGrafico, mostrarVersionGrafico } from "./graficosChartJs.js";
import { escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-tiempos");
const subtitulo = document.getElementById("resumen-tiempos");
const boton = document.getElementById("boton-medir");
const textoBoton = document.getElementById("texto-boton-medir");

const ALGORITMOS = ["Counting Sort", "Radix Sort", "Bucket Sort"];

/** Elementos del grafico de los algoritmos, para redibujar Chart.js al cambiar de version. */
let elementosGrafico = [];

/** Formatea milisegundos con 4 decimales, al estilo colombiano: 0,0158 */
function formatearMs(valor) {
    return valor.toLocaleString("es-CO", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

/** Porcentaje con un decimal: 12,3 % */
function formatearPorcentaje(valor) {
    return `${valor.toLocaleString("es-CO", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`;
}

export async function medirTiempos() {
    boton.disabled = true; // evita lanzar otra medicion mientras esta corre
    boton.setAttribute("aria-busy", "true");
    textoBoton.textContent = "Midiendo…";
    mostrarCargando(contenedor, "Midiendo en el servidor…",
        "Cada etapa se repite varias veces para calcular la media. Tarda unos segundos.");
    try {
        const reporte = await obtenerTiempos();
        elementosGrafico = reporte.resultados
            .filter((r) => ALGORITMOS.includes(r.etapa))
            .map((r) => ({ etiqueta: r.etapa, valor: r.mediaMs }));

        const hora = new Date().toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });
        subtitulo.textContent = `Cada etapa se mide por separado con System.nanoTime() · última medición ${hora}`;
        contenedor.innerHTML = `
            ${crearCifras(reporte)}
            ${crearTabla(reporte.resultados)}
            <div class="rejilla-tiempos">
                ${crearPanelAlgoritmos(elementosGrafico)}
                ${crearEquipo(reporte.equipo)}
            </div>`;
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        boton.disabled = false;
        boton.removeAttribute("aria-busy");
        textoBoton.textContent = "Medir de nuevo";
    }
}

/** Fila de cifras del corpus y aviso de si los tres algoritmos coinciden. */
function crearCifras(reporte) {
    const extraccion = reporte.resultados.find((r) => r.etapa.startsWith("Extraccion"));
    const counting = reporte.resultados.find((r) => r.etapa === "Counting Sort");
    const veces = Math.round(extraccion.mediaMs / counting.mediaMs).toLocaleString("es-CO");
    const validacion = reporte.coinciden
        ? `<span class="insignia insignia--ok">Los 3 algoritmos producen el mismo ranking</span>`
        : `<span class="insignia insignia--error">Los rankings NO coinciden</span>`;
    return `
        <div class="cifras">
            <div class="cifra"><span class="cifra__valor">${reporte.terminosTotales.toLocaleString("es-CO")}</span><span class="cifra__nombre">Términos totales</span></div>
            <div class="cifra"><span class="cifra__valor">${reporte.terminosDistintos.toLocaleString("es-CO")}</span><span class="cifra__nombre">Términos distintos (n)</span></div>
            <div class="cifra"><span class="cifra__valor">${reporte.terminosDistintosLucene.toLocaleString("es-CO")}</span><span class="cifra__nombre">Distintos con Lucene</span></div>
            <div class="cifra"><span class="cifra__valor">${reporte.frecuenciaMaxima}</span><span class="cifra__nombre">Frecuencia máxima (k)</span></div>
        </div>
        <p class="nota">${validacion} La extracción de los PDF tarda unas <strong>${veces}</strong> veces
        más que Counting Sort. Por eso cada etapa se mide por separado.</p>`;
}

/**
 * Tabla con las estadisticas de cada etapa. La ultima columna muestra que parte
 * del tiempo total se lleva cada etapa, con una barra proporcional.
 */
function crearTabla(resultados) {
    const total = resultados.reduce((suma, r) => suma + r.mediaMs, 0);
    const mayor = Math.max(...resultados.map((r) => r.mediaMs));

    const filas = resultados.map((r) => {
        const esAlgoritmo = ALGORITMOS.includes(r.etapa);
        const ancho = (r.mediaMs / mayor) * 100;
        return `
            <tr>
                <td>${escaparHtml(r.etapa)}${esAlgoritmo ? ` <span class="insignia insignia--suave">Ordenamiento</span>` : ""}</td>
                <td class="numero dato-suave">${r.repeticiones}</td>
                <td class="numero"><strong>${formatearMs(r.mediaMs)}</strong></td>
                <td class="numero dato-suave">± ${formatearMs(r.desviacionMs)}</td>
                <td class="numero dato-suave">${formatearMs(r.minimoMs)}</td>
                <td>
                    <span class="proporcion">
                        <span class="proporcion__pista"><span class="proporcion__relleno" style="width: ${ancho}%"></span></span>
                        <span class="dato-suave">${formatearPorcentaje((r.mediaMs / total) * 100)}</span>
                    </span>
                </td>
            </tr>`;
    }).join("");

    return `
        <section class="panel">
            <div class="panel__encabezado">
                <h3>Tiempos por etapa</h3>
                <span class="dato-suave">Unidad: milisegundos (ms)</span>
            </div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla">
                    <thead>
                        <tr>
                            <th>Etapa</th>
                            <th class="numero">Repeticiones</th>
                            <th class="numero">Media</th>
                            <th class="numero">Desviación</th>
                            <th class="numero">Mínimo</th>
                            <th>Parte del total</th>
                        </tr>
                    </thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>
        </section>`;
}

/**
 * Grafico solo con los tres algoritmos. Si se incluyera la extraccion de PDF
 * (cientos de ms), las barras de los algoritmos serian invisibles.
 */
function crearPanelAlgoritmos(elementos) {
    const ordenados = [...elementos].sort((a, b) => a.valor - b.valor); // solo para hallar el mas rapido
    const masRapido = ordenados[0];
    const segundo = ordenados[1];
    const veces = (segundo.valor / masRapido.valor).toLocaleString("es-CO", { maximumFractionDigits: 1 });

    return `
        <section class="panel">
            <div class="panel__encabezado">
                <h3>Ordenamiento de frecuencias</h3>
                ${crearSelectorGrafico("tiempos")}
            </div>
            <div class="panel__cuerpo">
                ${crearGraficosAlternables("tiempos", elementos, formatearMs)}
                <p class="nota nota--pie">Tiempo medio en ms. Menor es mejor.</p>
            </div>
            <div class="panel__pie">
                <span class="insignia insignia--ok">Más rápido</span>
                <span><strong>${escaparHtml(masRapido.etiqueta)}</strong> · ${veces}× más rápido que ${escaparHtml(segundo.etiqueta)}</span>
            </div>
        </section>`;
}

/** Caracteristicas del equipo donde se midio. */
function crearEquipo(equipo) {
    const memoriaGb = (equipo.memoriaTotalMb / 1024).toLocaleString("es-CO", { maximumFractionDigits: 1 });
    return `
        <section class="recuadro recuadro--equipo">
            <p class="recuadro__titulo">Equipo de la medición</p>
            <dl class="lista-datos">
                <dt>Procesador</dt><dd>${escaparHtml(equipo.procesador)}</dd>
                <dt>Núcleos lógicos</dt><dd>${equipo.nucleos}</dd>
                <dt>Memoria RAM</dt><dd>${memoriaGb} GB</dd>
                <dt>Sistema operativo</dt><dd>${escaparHtml(equipo.sistemaOperativo)}</dd>
                <dt>Java</dt><dd class="mono">${escaparHtml(equipo.versionJava)}</dd>
            </dl>
        </section>`;
}

/** Selector "HTML y CSS" / "Chart.js" del grafico. */
contenedor.addEventListener("click", (evento) => {
    const botonGrafico = evento.target.closest("[data-grafico]");
    if (botonGrafico) {
        mostrarVersionGrafico(botonGrafico, elementosGrafico, formatearMs);
    }
});