/**
 * Graficos con Chart.js (version con librerias).
 *
 * Chart.js se carga en index.html con un <script> normal y queda disponible
 * como la variable global "Chart". Dibuja sobre un <canvas> y agrega, sin
 * programarlo, ejes con escala, tooltips al pasar el mouse y animacion.
 *
 * Tambien tiene el selector "HTML y CSS" / "Chart.js" que usan Top 15 y
 * Tiempos para mostrar el mismo grafico con y sin librerias.
 */
import { crearGraficoBarras } from "./utilidades.js";
const graficos = {};

/** HTML del contenedor donde Chart.js dibuja. La altura depende de cuantas barras haya. */
export function crearContenedorChartJs(idCanvas, cantidadBarras) {
    const alto = cantidadBarras * 28 + 40;
    return `<div class="grafico-chartjs" style="height: ${alto}px"><canvas id="${idCanvas}"></canvas></div>`;
}

/**
 * Dibuja un grafico de barras horizontales en el canvas indicado.
 *
 * @param idCanvas       id del <canvas> (debe existir en la pagina)
 * @param elementos      arreglo de { etiqueta, valor } (mismo formato que crearGraficoBarras)
 * @param formatearValor funcion para mostrar los valores en el eje y en los tooltips
 */
export function dibujarBarrasChartJs(idCanvas, elementos, formatearValor = (valor) => valor) {
    const canvas = document.getElementById(idCanvas);
    if (typeof Chart === "undefined") {
        canvas.parentElement.innerHTML = '<p class="mensaje-error">No se pudo cargar Chart.js.</p>';
        return;
    }

    // Si ya habia un grafico en este canvas, se libera antes de crear el nuevo
    if (graficos[idCanvas]) {
        graficos[idCanvas].destroy();
    }

    // Mismos colores y fuente que el resto del sitio (variables de estilos.css)
    const estilos = getComputedStyle(document.documentElement);
    Chart.defaults.font.family = estilos.getPropertyValue("--font-sans").trim();
    Chart.defaults.color = estilos.getPropertyValue("--text-2").trim();            // textos de los ejes
    Chart.defaults.borderColor = estilos.getPropertyValue("--border").trim();      // lineas de la cuadricula
    graficos[idCanvas] = new Chart(canvas, {
        type: "bar",
        data: {
            labels: elementos.map((e) => e.etiqueta),
            datasets: [{
                data: elementos.map((e) => e.valor),
                backgroundColor: estilos.getPropertyValue("--primary").trim(),
                borderRadius: 4,
            }],
        },
        options: {
            indexAxis: "y",            // barras horizontales
            maintainAspectRatio: false, // usa la altura del contenedor
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: { label: (contexto) => formatearValor(contexto.parsed.x) },
                },
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: { callback: (valor) => formatearValor(valor) },
                },
            },
        },
    });
}


/**
 * Botones "HTML y CSS" / "Chart.js". El prefijo distingue cada grafico de la
 * pagina (por ejemplo "ranking" o "tiempos"), porque los ids no se pueden repetir.
 */
export function crearSelectorGrafico(prefijo) {
    return `
        <div class="segmentado segmentado--pequeno" role="radiogroup" aria-label="Versión del gráfico">
            <button type="button" role="radio" aria-checked="true" data-grafico="html" data-prefijo="${prefijo}">HTML y CSS</button>
            <button type="button" role="radio" aria-checked="false" data-grafico="chartjs" data-prefijo="${prefijo}">Chart.js</button>
        </div>`;
}

/** Las dos versiones del grafico: se ve la de HTML y CSS; la de Chart.js empieza oculta. */
export function crearGraficosAlternables(prefijo, elementos, formatearValor = (valor) => valor) {
    return `
        <div id="${prefijo}-html">${crearGraficoBarras(elementos, formatearValor)}</div>
        <div id="${prefijo}-chartjs" hidden>${crearContenedorChartJs(`${prefijo}-canvas`, elementos.length)}</div>`;
}

/**
 * Se llama al hacer clic en uno de los botones de crearSelectorGrafico:
 * marca el boton elegido, muestra esa version y oculta la otra.
 * Chart.js se dibuja cuando su contenedor ya es visible, para que tome bien el tamano.
 */
export function mostrarVersionGrafico(boton, elementos, formatearValor = (valor) => valor) {
    const prefijo = boton.dataset.prefijo;
    const usarChartJs = boton.dataset.grafico === "chartjs";
    boton.parentElement.querySelectorAll("button").forEach((b) => b.setAttribute("aria-checked", b === boton));
    document.getElementById(`${prefijo}-html`).hidden = usarChartJs;
    document.getElementById(`${prefijo}-chartjs`).hidden = !usarChartJs;
    if (usarChartJs) {
        dibujarBarrasChartJs(`${prefijo}-canvas`, elementos, formatearValor);
    }
}