/**
 * Vista "Agrupamiento" (Requerimiento 2).
 *
 * Pide a GET /api/agrupamiento los dendrogramas de los 3 criterios
 * (Single, Complete y Average) y los dibuja en SVG, sin librerias.
 * Tambien muestra la complejidad y el tiempo de cada etapa, y la
 * correlacion cofenetica de cada criterio.
 */
import { obtenerAgrupamiento } from "./api.js";
import { escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const contenedor = document.getElementById("contenido-agrupamiento");
const subtitulo = document.getElementById("resumen-agrupamiento");
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
const ALTO_EJE = 50;         // espacio para el eje de distancias y su titulo

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Pide los dendrogramas a la API y los muestra. */
export async function agrupar() {
    const nombreSimilitud = selectorSimilitud.options[selectorSimilitud.selectedIndex].text;
    botonAgrupar.disabled = true;
    botonAgrupar.setAttribute("aria-busy", "true");
    mostrarCargando(contenedor, "Calculando…",
        "Construyendo la matriz de distancias y los tres dendrogramas. Con MiniLM la primera vez tarda unos segundos.");
    try {
        const datos = await obtenerAgrupamiento(selectorSimilitud.value);
        subtitulo.textContent =
            `k = ${datos.articulos.length} artículos · distancia = 1 − similitud · ${nombreSimilitud}`;
        contenedor.innerHTML = `
            <div class="rejilla-dos">
                ${crearTablaEtapas(datos)}
                ${crearTablaCorrelaciones(datos)}
            </div>
            ${crearPanelDendrogramas(datos)}`;
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        botonAgrupar.disabled = false;
        botonAgrupar.removeAttribute("aria-busy");
    }
}

/** Tabla con la complejidad y el tiempo de cada etapa del agrupamiento. */
function crearTablaEtapas(datos) {
    const fila = (etapa, complejidad, tiempoMs) => `
        <tr>
            <td>${etapa}</td>
            <td class="mono">${escaparHtml(complejidad)}</td>
            <td class="numero">${tiempoMs.toFixed(3)}</td>
        </tr>`;

    const filasCriterios = datos.dendrogramas.map((dendrograma) =>
        fila(NOMBRES_CRITERIO[dendrograma.criterio], dendrograma.complejidad, dendrograma.tiempoMs)
    ).join("");
    const tiempoCorrelaciones = datos.dendrogramas.reduce((suma, d) => suma + d.tiempoCorrelacionMs, 0);

    return `
        <section class="panel">
            <div class="panel__encabezado"><h3>Complejidad y tiempos por etapa</h3></div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla">
                    <thead><tr><th>Etapa</th><th>Complejidad</th><th class="numero">Tiempo (ms)</th></tr></thead>
                    <tbody>
                        ${fila("Matriz de distancias", datos.complejidadMatriz, datos.tiempoMatrizMs)}
                        ${filasCriterios}
                        ${fila("Correlación cofenética (los 3)", `${datos.complejidadCorrelacion} cada una`, tiempoCorrelaciones)}
                    </tbody>
                </table>
            </div>
            <p class="nota nota--pie panel__nota">
                <span class="mono">k</span>: número de artículos ·
                <span class="mono">n</span>, <span class="mono">m</span>: palabras de los dos abstracts comparados.
                La matriz se calcula una vez y la usan los tres criterios.
            </p>
        </section>`;
}

/** Tabla con la correlacion cofenetica de cada criterio; el mejor se resalta. */
function crearTablaCorrelaciones(datos) {
    const filas = datos.dendrogramas.map((dendrograma) => {
        const esMejor = dendrograma.criterio === datos.mejorCriterio;
        const insignia = esMejor ? ` <span class="insignia insignia--ok">Más coherente</span>` : "";
        const valor = dendrograma.correlacionCofenetica;
        return `
            <tr class="${esMejor ? "fila-destacada" : ""}">
                <td>${NOMBRES_CRITERIO[dendrograma.criterio]}${insignia}</td>
                <td class="numero"><strong>${valor.toFixed(4)}</strong></td>
                <td>
                    <span class="proporcion__pista proporcion__pista--escala">
                        <span class="proporcion__relleno" style="width: ${(Math.max(0, valor) * 100).toFixed(1)}%"></span>
                    </span>
                </td>
            </tr>`;
    }).join("");
    return `
        <section class="panel">
            <div class="panel__encabezado">
                <div>
                    <h3>Correlación cofenética</h3>
                    <p class="dato-suave">Qué tan bien el dendrograma conserva las distancias originales (1 = perfecto)</p>
                </div>
            </div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla">
                    <thead><tr><th>Criterio</th><th class="numero">Correlación</th><th><span class="solo-lector">Escala</span></th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>
        </section>`;
}

/**
 * Panel con los tres dendrogramas y unas pestanas para ver los tres o uno solo.
 * Los tres usan la MISMA escala de distancia para que se puedan comparar.
 */
function crearPanelDendrogramas(datos) {
    const todasLasAlturas = datos.dendrogramas.flatMap((d) => d.uniones.map((union) => union.altura));
    const escala = {
        inicio: Math.floor(Math.min(...todasLasAlturas) * 10) / 10,
        fin: Math.max(...todasLasAlturas),
    };

    const pestanas = [{ valor: "todos", texto: "Los tres" }]
        .concat(datos.dendrogramas.map((d) => ({ valor: d.criterio, texto: NOMBRES_CRITERIO[d.criterio].split(" ")[0] })))
        .map((p, i) => `<button type="button" role="radio" aria-checked="${i === 0}" data-dendrograma="${p.valor}">${p.texto}</button>`)
        .join("");

    const tarjetas = datos.dendrogramas.map((dendrograma) => `
        <div class="tarjeta-dendrograma" data-criterio="${dendrograma.criterio}">
            <div class="tarjeta-dendrograma__encabezado">
                <h4>${NOMBRES_CRITERIO[dendrograma.criterio]}</h4>
                <span class="dato-suave">Correlación cofenética: <strong>${dendrograma.correlacionCofenetica.toFixed(4)}</strong></span>
            </div>
            <div class="dendrograma">${dibujarDendrograma(dendrograma.uniones, datos.articulos, escala)}</div>
        </div>`).join("");

    return `
        <section class="panel panel--separado">
            <div class="panel__encabezado">
                <div>
                    <h3>Dendrogramas</h3>
                    <p class="dato-suave">Misma escala de distancia en los tres: de ${escala.inicio.toFixed(2)} a ${escala.fin.toFixed(2)}</p>
                </div>
                <div class="segmentado segmentado--pequeno" role="radiogroup" aria-label="Dendrogramas a mostrar">${pestanas}</div>
            </div>
            <div class="panel__cuerpo dendrogramas">${tarjetas}</div>
        </section>`;
}

/** Botones "Los tres / Single / Complete / Average": muestran todos o solo uno. */
contenedor.addEventListener("click", (evento) => {
    const boton = evento.target.closest("[data-dendrograma]");
    if (!boton) {
        return;
    }
    const elegido = boton.dataset.dendrograma;
    boton.parentElement.querySelectorAll("button").forEach((b) => b.setAttribute("aria-checked", b === boton));
    contenedor.querySelectorAll(".tarjeta-dendrograma").forEach((tarjeta) => {
        tarjeta.hidden = elegido !== "todos" && tarjeta.dataset.criterio !== elegido;
    });
});

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
 *
 * @param escala { inicio, fin }: la misma para los tres dendrogramas
 */
function dibujarDendrograma(uniones, articulos, escala) {
    const n = articulos.length;
    const orden = ordenDeLasHojas(uniones, n);

    const posicionX = (altura) =>
        MARGEN_IZQUIERDO + ((altura - escala.inicio) / (escala.fin - escala.inicio || 1)) * ANCHO_ARBOL;

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

    // Titulos de los articulos, a la izquierda de cada hoja (el completo aparece al pasar el cursor)
    const etiquetas = orden.map((indice, fila) => {
        const articulo = articulos[indice];
        const titulo = articulo.titulo.length > 42 ? `${articulo.titulo.slice(0, 42)}…` : articulo.titulo;
        const y = MARGEN_SUPERIOR + fila * ALTO_FILA + ALTO_FILA / 2 + 4;
        return `<text x="${MARGEN_IZQUIERDO - 8}" y="${y}" text-anchor="end">
                    <title>${escaparHtml(articulo.titulo)}</title><tspan class="dendrograma__numero">${numeroDelArticulo(articulo)}</tspan> ${escaparHtml(titulo)}</text>`;
    }).join("");

    // Eje inferior con 5 marcas y su titulo
    const yEje = MARGEN_SUPERIOR + n * ALTO_FILA + 6;
    let eje = `<line class="dendrograma__eje" x1="${MARGEN_IZQUIERDO}" y1="${yEje}" x2="${MARGEN_IZQUIERDO + ANCHO_ARBOL}" y2="${yEje}" />`;
    for (let k = 0; k <= 4; k++) {
        const altura = escala.inicio + ((escala.fin - escala.inicio) * k) / 4;
        const x = posicionX(altura);
        eje += `<line class="dendrograma__eje" x1="${x}" y1="${yEje}" x2="${x}" y2="${yEje + 5}" />
                <text class="dendrograma__marca" x="${x}" y="${yEje + 18}" text-anchor="middle">${altura.toFixed(2)}</text>`;
    }
    eje += `<text class="dendrograma__marca" x="${MARGEN_IZQUIERDO + ANCHO_ARBOL / 2}" y="${yEje + 38}"
                  text-anchor="middle">Distancia (1 − similitud)</text>`;

    const ancho = MARGEN_IZQUIERDO + ANCHO_ARBOL + 30;
    const alto = yEje + ALTO_EJE;
    return `
        <svg viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Dendrograma">
            <g class="dendrograma__etiquetas">${etiquetas}</g>
            <g class="dendrograma__lineas">${lineas}</g>
            <g>${eje}</g>
        </svg>`;
}