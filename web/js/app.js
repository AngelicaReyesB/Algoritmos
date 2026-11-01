/**
 * Punto de entrada del frontend:
 * - Cambia de vista al hacer clic en las pestanas.
 * - Verifica que la API responda y muestra un resumen del corpus.
 * - Cambia entre el tema claro y el oscuro.
 */
import { obtenerRanking } from "./api.js";
import { cargarArticulos } from "./vistaArticulos.js";
import { cargarRanking } from "./vistaRanking.js";
import { medirTiempos } from "./vistaTiempos.js";
import { buscar } from "./vistaBusqueda.js";
import { cargarSeleccionArticulos, compararSeleccionados, seleccionarTodos } from "./vistaSimilitud.js";
import { agrupar } from "./vistaAgrupamiento.js";
import { escaparHtml } from "./utilidades.js";

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

/** Iconos de la pastilla de estado: visto bueno y equis. */
const ICONO_OK = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M5 12l5 5L20 7"></path></svg>';
const ICONO_ERROR = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"></path></svg>';

/** Consulta la API y muestra cuantos terminos tiene el corpus. */
async function verificarApi() {
    const estado = document.getElementById("estado-api");
    try {
        const ranking = await obtenerRanking(1);
        estado.innerHTML =
            `<span class="estado__pastilla">${ICONO_OK}API conectada</span>` +
            `<span>${ranking.terminosDistintos} términos distintos</span>` +
            `<span aria-hidden="true">·</span>` +
            `<span>frecuencia máxima: ${ranking.frecuenciaMaxima}</span>`;
        estado.classList.add("estado--ok");
    } catch (error) {
        estado.innerHTML =
            `<span class="estado__pastilla">${ICONO_ERROR}Error de conexión con la API</span>` +
            `<span>${escaparHtml(error.message)}</span>`;
        estado.classList.add("estado--error");
    }
}

/**
 * Tema claro u oscuro. El script del <head> ya aplico el tema guardado;
 * aqui solo se cambia al hacer clic y se recuerda en el navegador.
 */
const botonTema = document.getElementById("boton-tema");
const textoTema = document.getElementById("texto-tema");

function mostrarTema() {
    const oscuro = document.documentElement.dataset.theme === "dark";
    textoTema.textContent = oscuro ? "Tema claro" : "Tema oscuro";
    botonTema.setAttribute("aria-pressed", oscuro);
}

botonTema.addEventListener("click", () => {
    const nuevo = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nuevo;
    try {
        localStorage.setItem("tema", nuevo);
    } catch (error) {
        // Navegador sin almacenamiento (modo privado): el tema solo dura esta visita
    }
    mostrarTema();
});

pestanas.forEach((pestana) => {
    pestana.addEventListener("click", () => mostrarVista(pestana.dataset.vista));
});


document.getElementById("boton-medir").addEventListener("click", medirTiempos);
document.getElementById("formulario-busqueda").addEventListener("submit", (evento) => {
    evento.preventDefault(); // sin esto el formulario recargaria la pagina
    buscar();
});
document.getElementById("boton-seleccionar-todos").addEventListener("click", seleccionarTodos);
document.getElementById("boton-comparar").addEventListener("click", compararSeleccionados);
document.getElementById("boton-agrupar").addEventListener("click", agrupar);

mostrarTema();
verificarApi();
cargarArticulos();
cargarRanking();
cargarSeleccionArticulos();