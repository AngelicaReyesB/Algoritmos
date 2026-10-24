/**
 * Vista "Tiempos": ejecuta la medicion en el backend y muestra los resultados.
 *
 * La medicion tarda unos segundos (la extraccion de los PDF se repite 5 veces),
 * por eso no se ejecuta al abrir la pagina sino al presionar el boton.
 */
import { obtenerTiempos } from "./api.js";
import { crearGraficoBarras, escaparHtml, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-tiempos");
const boton = document.getElementById("boton-medir");

const ALGORITMOS = ["Counting Sort", "Radix Sort", "Bucket Sort"];

/** Formatea milisegundos con 4 decimales, al estilo colombiano: 0,0158 */
function formatearMs(valor) {
    return valor.toLocaleString("es-CO", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

export async function medirTiempos() {
    boton.disabled = true; // evita lanzar otra medicion mientras esta corre
    boton.textContent = "Midiendo…";
    contenedor.innerHTML = "<p>Ejecutando la medición en el servidor. Tarda unos segundos…</p>";
    try {
        const reporte = await obtenerTiempos();
        contenedor.innerHTML =
            crearResumen(reporte) +
            crearTabla(reporte.resultados) +
            crearGraficoAlgoritmos(reporte.resultados) +
            crearEquipo(reporte.equipo);
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        boton.disabled = false;
        boton.textContent = "Medir de nuevo";
    }
}

/** Datos del corpus, validacion y comparacion entre extraccion y ordenamiento. */
function crearResumen(reporte) {
    const extraccion = reporte.resultados.find((r) => r.etapa.startsWith("Extraccion"));
    const counting = reporte.resultados.find((r) => r.etapa === "Counting Sort");
    const veces = Math.round(extraccion.mediaMs / counting.mediaMs).toLocaleString("es-CO");
    const validacion = reporte.coinciden
        ? `<span class="insignia insignia--ok">Los 3 algoritmos producen el mismo ranking</span>`
        : `<span class="insignia insignia--error">Los rankings NO coinciden</span>`;
    return `
        <div class="resumen">
            <p>Términos totales: <strong>${reporte.terminosTotales}</strong></p>
            <p>Términos distintos (n): <strong>${reporte.terminosDistintos}</strong></p>
            <p>Términos distintos con Lucene: <strong>${reporte.terminosDistintosLucene}</strong></p>            
            <p>Frecuencia máxima (k): <strong>${reporte.frecuenciaMaxima}</strong></p>
            ${validacion}
        </div>
        <p class="nota">La extracción de los PDF tarda unas <strong>${veces}</strong> veces más
        que Counting Sort. Por eso cada etapa se mide por separado.</p>`;
}

/** Tabla con las estadisticas de cada etapa. */
function crearTabla(resultados) {
    const filas = resultados.map((r) => {
        const esAlgoritmo = ALGORITMOS.includes(r.etapa);
        return `
            <tr class="${esAlgoritmo ? "fila-destacada" : ""}">
                <td>${escaparHtml(r.etapa)}</td>
                <td class="numero">${r.repeticiones}</td>
                <td class="numero">${formatearMs(r.mediaMs)}</td>
                <td class="numero">${formatearMs(r.desviacionMs)}</td>
                <td class="numero">${formatearMs(r.minimoMs)}</td>
            </tr>`;
    }).join("");

    return `
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead>
                    <tr>
                        <th>Etapa</th>
                        <th class="numero">Repeticiones</th>
                        <th class="numero">Media (ms)</th>
                        <th class="numero">Desv. estándar (ms)</th>
                        <th class="numero">Mínimo (ms)</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

/**
 * Grafico solo con los tres algoritmos. Si se incluyera la extraccion de PDF
 * (cientos de ms), las barras de los algoritmos serian invisibles.
 */
function crearGraficoAlgoritmos(resultados) {
    const elementos = resultados
        .filter((r) => ALGORITMOS.includes(r.etapa))
        .map((r) => ({ etiqueta: r.etapa, valor: r.mediaMs }));
    return `<h3>Algoritmos de ordenamiento (media en ms)</h3>${crearGraficoBarras(elementos, formatearMs)}`;
}

/** Caracteristicas del equipo donde se midio. */
function crearEquipo(equipo) {
    const memoriaGb = (equipo.memoriaTotalMb / 1024).toLocaleString("es-CO", { maximumFractionDigits: 1 });
    return `
        <h3>Equipo de la medición</h3>
        <dl class="lista-datos">
            <dt>Procesador</dt><dd>${escaparHtml(equipo.procesador)}</dd>
            <dt>Núcleos lógicos</dt><dd>${equipo.nucleos}</dd>
            <dt>Memoria RAM</dt><dd>${memoriaGb} GB</dd>
            <dt>Sistema operativo</dt><dd>${escaparHtml(equipo.sistemaOperativo)}</dd>
            <dt>Java</dt><dd>${escaparHtml(equipo.versionJava)}</dd>
        </dl>`;
}