/**
 * Vista "Agrupamiento" (Requerimiento 2).
 *
 * Pide a GET /api/agrupamiento los dendrogramas de los 3 criterios
 * (Single, Complete y Average) y los dibuja en SVG, sin librerias.
 * Tambien muestra la complejidad y el tiempo de cada etapa, y la
 * correlacion cofenetica de cada criterio.
 */
import { obtenerAgrupamiento } from "./api.js";
import { escaparHtml, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-agrupamiento");
const selectorSimilitud = document.getElementById("selector-similitud");
const botonAgrupar = document.getElementById("boton-agrupar");

/** Nombres para mostrar de cada criterio. */
const NOMBRES_CRITERIO = {
    SINGLE: "Single linkage",
    COMPLETE: "Complete linkage",
    AVERAGE: "Average linkage",
};

/** Medidas del dibujo (en unidades del SVG). */
const ALTO_FILA = 24;        // espacio vertical para cada articulo
const MARGEN_IZQUIERDO = 330; // espacio para los titulos de los articulos
const ANCHO_ARBOL = 520;     // ancho de la parte del arbol
const MARGEN_SUPERIOR = 10;
const ALTO_EJE = 36;         // espacio para el eje de alturas

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Pide los dendrogramas a la API y los muestra. */
export async function agrupar() {
    botonAgrupar.disabled = true;
    contenedor.innerHTML = "<p>Agrupando… Con MiniLM la primera vez tarda unos segundos porque se carga el modelo.</p>";
    try {
        const datos = await obtenerAgrupamiento(selectorSimilitud.value);
        contenedor.innerHTML =
            crearTablaEtapas(datos) +
            crearTablaCorrelaciones(datos) +
            datos.dendrogramas.map((dendrograma) => crearSeccionDendrograma(dendrograma, datos)).join("");
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        botonAgrupar.disabled = false;
    }
}

/** Tabla con la complejidad y el tiempo de cada etapa del agrupamiento. */
function crearTablaEtapas(datos) {
    const fila = (etapa, complejidad, tiempoMs) => `
        <tr>
            <td>${etapa}</td>
            <td>${escaparHtml(complejidad)}</td>
            <td class="numero">${tiempoMs.toFixed(3)}</td>
        </tr>`;

    const filasCriterios = datos.dendrogramas.map((dendrograma) =>
        fila(`Agrupamiento ${NOMBRES_CRITERIO[dendrograma.criterio]}`, dendrograma.complejidad, dendrograma.tiempoMs)
    ).join("");
    const tiempoCorrelaciones = datos.dendrogramas.reduce((suma, d) => suma + d.tiempoCorrelacionMs, 0);

    return `
        <h3>Complejidad y tiempos por etapa</h3>
        <p class="nota">k = cantidad de artículos (${datos.articulos.length}); n y m = palabras de cada abstract.
        La matriz de distancias se calcula una vez y la usan los tres criterios.</p>
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead><tr><th>Etapa</th><th>Complejidad</th><th>Tiempo (ms)</th></tr></thead>
                <tbody>
                    ${fila("Matriz de distancias", datos.complejidadMatriz, datos.tiempoMatrizMs)}
                    ${filasCriterios}
                    ${fila("Correlación cofenética (los 3 criterios)", `${datos.complejidadCorrelacion} cada una`, tiempoCorrelaciones)}
                </tbody>
            </table>
        </div>`;
}

/** Tabla con la correlacion cofenetica de cada criterio; el mejor se resalta. */
function crearTablaCorrelaciones(datos) {
    const filas = datos.dendrogramas.map((dendrograma) => {
        const esMejor = dendrograma.criterio === datos.mejorCriterio;
        const insignia = esMejor ? ` <span class="insignia insignia--ok">Más coherente</span>` : "";
        return `
            <tr class="${esMejor ? "fila-destacada" : ""}">
                <td>${NOMBRES_CRITERIO[dendrograma.criterio]}${insignia}</td>
                <td class="numero">${dendrograma.correlacionCofenetica.toFixed(4)}</td>
            </tr>`;
    }).join("");
    return `
        <h3>Correlación cofenética</h3>
        <p class="nota">Compara las distancias originales con las alturas del dendrograma.
        Cuanto más cerca de 1, mejor representa el dendrograma las distancias reales.</p>
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead><tr><th>Criterio</th><th>Correlación</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

function crearSeccionDendrograma(dendrograma, datos) {
    return `
        <h3>${NOMBRES_CRITERIO[dendrograma.criterio]}</h3>
        <div class="dendrograma">${dibujarDendrograma(dendrograma.uniones, datos.articulos)}</div>`;
}

/**
 * Orden de las hojas: se recorre el arbol desde la raiz (la ultima union),
 * primero el grupo A y despues el B. Asi los articulos de un mismo grupo
 * quedan uno al lado del otro y las lineas no se cruzan.
 */
function ordenDeLasHojas(uniones, cantidadArticulos) {
    const unionQueCrea = new Map();
    uniones.forEach((union) => unionQueCrea.set(union.grupoNuevo, union));

    function hojasDe(grupo) {
        if (grupo < cantidadArticulos) {
            return [grupo]; // los grupos 0..n-1 son los articulos
        }
        const union = unionQueCrea.get(grupo);
        return [...hojasDe(union.grupoA), ...hojasDe(union.grupoB)];
    }

    const raiz = uniones[uniones.length - 1].grupoNuevo;
    return hojasDe(raiz);
}

/**
 * Dibuja el dendrograma de lado: los articulos a la izquierda y la altura
 * de las uniones hacia la derecha.
 *
 * Cada union se dibuja con tres lineas: una horizontal desde cada grupo
 * hasta la altura de la union, y una vertical que los conecta.
 */
function dibujarDendrograma(uniones, articulos) {
    const n = articulos.length;
    const orden = ordenDeLasHojas(uniones, n);

    // Eje de alturas: empieza un poco antes de la union mas baja para que se vean las diferencias
    const alturas = uniones.map((union) => union.altura);
    const alturaMaxima = Math.max(...alturas);
    const inicioEje = Math.floor(Math.min(...alturas) * 10) / 10;
    const posicionX = (altura) =>
        MARGEN_IZQUIERDO + ((altura - inicioEje) / (alturaMaxima - inicioEje || 1)) * ANCHO_ARBOL;

    // Posicion de cada grupo. Las hojas van en el borde izquierdo del arbol.
    const posicion = new Map();
    orden.forEach((articulo, fila) => {
        posicion.set(articulo, { x: MARGEN_IZQUIERDO, y: MARGEN_SUPERIOR + fila * ALTO_FILA + ALTO_FILA / 2 });
    });

    let lineas = "";
    for (const union of uniones) {
        const a = posicion.get(union.grupoA);
        const b = posicion.get(union.grupoB);
        const x = posicionX(union.altura);
        lineas += `
            <line x1="${a.x}" y1="${a.y}" x2="${x}" y2="${a.y}" />
            <line x1="${b.x}" y1="${b.y}" x2="${x}" y2="${b.y}" />
            <line x1="${x}" y1="${a.y}" x2="${x}" y2="${b.y}"><title>Altura ${union.altura.toFixed(4)}</title></line>`;
        posicion.set(union.grupoNuevo, { x, y: (a.y + b.y) / 2 });
    }

    // Titulos de los articulos, a la izquierda de cada hoja
    const etiquetas = orden.map((indice, fila) => {
        const articulo = articulos[indice];
        const titulo = articulo.titulo.length > 42 ? `${articulo.titulo.slice(0, 42)}…` : articulo.titulo;
        const y = MARGEN_SUPERIOR + fila * ALTO_FILA + ALTO_FILA / 2 + 4;
        return `<text x="${MARGEN_IZQUIERDO - 8}" y="${y}" text-anchor="end">
                    <title>${escaparHtml(articulo.titulo)}</title>${numeroDelArticulo(articulo)}. ${escaparHtml(titulo)}</text>`;
    }).join("");

    // Eje inferior con 5 marcas
    const yEje = MARGEN_SUPERIOR + n * ALTO_FILA + 6;
    let eje = `<line class="dendrograma__eje" x1="${MARGEN_IZQUIERDO}" y1="${yEje}" x2="${MARGEN_IZQUIERDO + ANCHO_ARBOL}" y2="${yEje}" />`;
    for (let k = 0; k <= 4; k++) {
        const altura = inicioEje + ((alturaMaxima - inicioEje) * k) / 4;
        const x = posicionX(altura);
        eje += `<line class="dendrograma__eje" x1="${x}" y1="${yEje}" x2="${x}" y2="${yEje + 5}" />
                <text class="dendrograma__marca" x="${x}" y="${yEje + 18}" text-anchor="middle">${altura.toFixed(2)}</text>`;
    }

    const ancho = MARGEN_IZQUIERDO + ANCHO_ARBOL + 30;
    const alto = yEje + ALTO_EJE;
    return `
        <svg viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Dendrograma">
            <g class="dendrograma__etiquetas">${etiquetas}</g>
            <g class="dendrograma__lineas">${lineas}</g>
            <g>${eje}</g>
        </svg>
        <p class="nota">Eje horizontal: distancia (1 − similitud) a la que se unen los grupos.
        Empieza en ${inicioEje.toFixed(2)} para que se vean las diferencias.</p>`;
}