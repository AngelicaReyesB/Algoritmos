/**
 * Graficos con Chart.js (version con librerias).
 *
 * Chart.js se carga en index.html con un <script> normal y queda disponible
 * como la variable global "Chart". Dibuja sobre un <canvas> y agrega, sin
 * programarlo, ejes con escala, tooltips al pasar el mouse y animacion.
 */

/** Graficos ya creados (id del canvas -> grafico), para destruirlos antes de redibujar. */
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
    Chart.defaults.font.family = estilos.getPropertyValue("--fuente").trim();

    graficos[idCanvas] = new Chart(canvas, {
        type: "bar",
        data: {
            labels: elementos.map((e) => e.etiqueta),
            datasets: [{
                data: elementos.map((e) => e.valor),
                backgroundColor: estilos.getPropertyValue("--color-primario").trim(),
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