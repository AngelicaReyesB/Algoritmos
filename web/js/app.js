/**
 * Punto de entrada del frontend:
 * - Cambia de vista al hacer clic en las pestanas.
 * - Verifica que la API responda y muestra un resumen del corpus.
 */
import { obtenerRanking } from "./api.js";
import { cargarArticulos } from "./vistaArticulos.js";
import { cargarRanking } from "./vistaRanking.js";
import { medirTiempos } from "./vistaTiempos.js";
import { cargarSeleccionArticulos, compararSeleccionados, seleccionarTodos } from "./vistaSimilitud.js";
import { agrupar } from "./vistaAgrupamiento.js";

const pestanas = document.querySelectorAll(".pestana");
const vistas = document.querySelectorAll(".vista");

/** Muestra la vista indicada y oculta las demas. */
function mostrarVista(nombre) {
    pestanas.forEach((pestana) => {
        const activa = pestana.dataset.vista === nombre;
        pestana.setAttribute("aria-selected", activa);
    });
    vistas.forEach((vista) => {
        vista.hidden = vista.id !== `vista-${nombre}`;
    });
}

/** Consulta la API y muestra cuantos terminos tiene el corpus. */
async function verificarApi() {
    const estado = document.getElementById("estado-api");
    try {
        const ranking = await obtenerRanking(1);
        estado.textContent =
            `API conectada · ${ranking.terminosDistintos} términos distintos · ` +
            `frecuencia máxima ${ranking.frecuenciaMaxima}`;
        estado.classList.add("estado--ok");
    } catch (error) {
        estado.textContent = `No se pudo conectar con la API: ${error.message}`;
        estado.classList.add("estado--error");
    }
}

pestanas.forEach((pestana) => {
    pestana.addEventListener("click", () => mostrarVista(pestana.dataset.vista));
});

document.getElementById("selector-top").addEventListener("change", cargarRanking);
document.getElementById("boton-medir").addEventListener("click", medirTiempos);
document.getElementById("boton-seleccionar-todos").addEventListener("click", seleccionarTodos);
document.getElementById("boton-comparar").addEventListener("click", compararSeleccionados);
document.getElementById("boton-agrupar").addEventListener("click", agrupar);

verificarApi();
cargarArticulos();
cargarRanking();
cargarSeleccionArticulos();