/**
 * Vista "Similitud" (Requerimiento 1).
 *
 * 1. Muestra los 20 articulos con una casilla para elegirlos.
 * 2. Envia los ids elegidos a POST /api/similitud.
 * 3. Muestra una tabla resumen (complejidad, tiempo y par mas parecido de cada
 *    algoritmo) y una matriz de similitud por algoritmo.
 *    Cada celda se colorea: mas azul = mas parecidos.
 */
import { compararSimilitud, obtenerArticulos } from "./api.js";
import { escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const listaArticulos = document.getElementById("seleccion-articulos");
const contador = document.getElementById("contador-seleccion");
const contenedor = document.getElementById("contenido-similitud");
const botonComparar = document.getElementById("boton-comparar");
const botonTodos = document.getElementById("boton-seleccionar-todos");

/** Los dos algoritmos que usan modelos de IA; los otros cuatro son clasicos. */
const ALGORITMOS_IA = ["FastText", "MiniLM"];

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Dibuja una casilla por cada articulo del corpus. */
export async function cargarSeleccionArticulos() {
    mostrarCargando(listaArticulos, "Cargando artículos…");
    try {
        const articulos = await obtenerArticulos();
        listaArticulos.innerHTML = articulos.map((articulo) => `
            <label class="seleccion__opcion" title="${escaparHtml(articulo.titulo)}">
                <input type="checkbox" value="${articulo.id}">
                <span class="seleccion__numero">${numeroDelArticulo(articulo)}</span>
                <span class="seleccion__titulo">${escaparHtml(articulo.titulo)}</span>
            </label>`).join("");
        actualizarContador();
    } catch (error) {
        mostrarError(listaArticulos, error);
    }
}

function casillas() {
    return [...listaArticulos.querySelectorAll("input[type=checkbox]")];
}

/** "3 de 20 seleccionados", el texto del boton "Seleccionar todos" y si se puede comparar. */
function actualizarContador() {
    const todas = casillas();
    const marcadas = todas.filter((casilla) => casilla.checked).length;
    const pista = marcadas < 2 ? "elija al menos 2" : "listo para comparar";
    contador.innerHTML = `<strong>${marcadas} de ${todas.length}</strong> seleccionados · ${pista}`;
    botonTodos.textContent = marcadas === todas.length ? "Quitar selección" : "Seleccionar todos";
    botonComparar.disabled = marcadas < 2;
}

listaArticulos.addEventListener("change", actualizarContador);

/** Marca todas las casillas; si ya estaban todas marcadas, las desmarca. */
export function seleccionarTodos() {
    const todas = casillas();
    const todasMarcadas = todas.every((casilla) => casilla.checked);
    todas.forEach((casilla) => {
        casilla.checked = !todasMarcadas;
    });
    actualizarContador();
}

/** Envia los articulos marcados a la API y muestra los resultados. */
export async function compararSeleccionados() {
    const ids = casillas().filter((casilla) => casilla.checked).map((casilla) => Number(casilla.value));
    if (ids.length < 2) {
        contenedor.innerHTML = crearVacio("Seleccione al menos 2 artículos para realizar la comparación.",
            "Marque las casillas de la lista y pulse «Comparar seleccionados».");
        return;
    }

    botonComparar.disabled = true;
    botonComparar.setAttribute("aria-busy", "true");
    mostrarCargando(contenedor, "Calculando…",
        "La primera vez tarda unos segundos porque se cargan los modelos de IA.");
    try {
        const datos = await compararSimilitud(ids);
        contenedor.innerHTML =
            crearLeyenda(datos.articulos) +
            crearTablaResumen(datos.resultados, datos.articulos) +
            crearNotacion() +
            crearMatrices(datos.resultados, datos.articulos);
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        botonComparar.removeAttribute("aria-busy");
        actualizarContador();
    }
}

function crearVacio(titulo, detalle) {
    return `
        <div class="vacio">
            <p class="vacio__titulo">${escaparHtml(titulo)}</p>
            <p>${escaparHtml(detalle)}</p>
        </div>`;
}

/** Fila de "fichas" con el numero y el titulo de cada articulo comparado. */
function crearLeyenda(articulos) {
    const fichas = articulos.map((articulo) => `
        <span class="ficha" title="${escaparHtml(articulo.titulo)}">
            <strong>${numeroDelArticulo(articulo)}</strong>
            <span class="ficha__texto">— ${escaparHtml(articulo.titulo)}</span>
        </span>`).join("");
    return `
        <div class="leyenda-articulos">
            <span class="leyenda-articulos__titulo">Artículos comparados (k = ${articulos.length}):</span>
            ${fichas}
        </div>`;
}

/**
 * Par mas parecido de una matriz: el valor mas alto fuera de la diagonal.
 * Solo se recorre la parte de arriba (j > i) porque la matriz es simetrica.
 */
function parMasParecido(matriz) {
    let mejor = { i: 0, j: 1, valor: matriz[0][1] };
    for (let i = 0; i < matriz.length; i++) {
        for (let j = i + 1; j < matriz.length; j++) {
            if (matriz[i][j] > mejor.valor) {
                mejor = { i, j, valor: matriz[i][j] };
            }
        }
    }
    return mejor;
}

/** Tabla resumen: los 4 clasicos y los 2 de IA, separados por una fila de titulo. */
function crearTablaResumen(resultados, articulos) {
    const k = articulos.length;
    const pares = (k * (k - 1)) / 2;
    const clasicos = resultados.filter((r) => !ALGORITMOS_IA.includes(r.algoritmo));
    const conIa = resultados.filter((r) => ALGORITMOS_IA.includes(r.algoritmo));

    const fila = (resultado) => {
        const par = parMasParecido(resultado.matriz);
        const nombrePar = `${numeroDelArticulo(articulos[par.i])} y ${numeroDelArticulo(articulos[par.j])}`;
        const etiquetaIa = ALGORITMOS_IA.includes(resultado.algoritmo)
            ? ` <span class="insignia insignia--suave">IA</span>` : "";
        return `
            <tr>
                <td><strong>${escaparHtml(resultado.algoritmo)}</strong>${etiquetaIa}</td>
                <td class="mono">${escaparHtml(resultado.complejidad)}</td>
                <td class="numero">${resultado.tiempoMs.toFixed(3)}</td>
                <td class="numero">${nombrePar}</td>
                <td class="numero"><strong>${par.valor.toFixed(3)}</strong></td>
                <td>${crearBarraEscala(par.valor)}</td>
            </tr>`;
    };

    return `
        <section class="panel">
            <div class="panel__encabezado">
                <h3>Resultados de similitud</h3>
                <span class="dato-suave">${pares} pares evaluados por algoritmo</span>
            </div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla">
                    <thead>
                        <tr>
                            <th>Algoritmo</th><th>Complejidad por par</th><th class="numero">Tiempo (ms)</th>
                            <th class="numero">Par más parecido</th><th class="numero">Similitud</th>
                            <th><span class="solo-lector">Escala</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr class="fila-grupo"><td colspan="6">Algoritmos clásicos</td></tr>
                        ${clasicos.map(fila).join("")}
                        <tr class="fila-grupo"><td colspan="6">Modelos de IA</td></tr>
                        ${conIa.map(fila).join("")}
                    </tbody>
                </table>
            </div>
        </section>`;
}

/** Barra pequena de 0 a 1 que acompana un valor de similitud. */
function crearBarraEscala(valor) {
    return `
        <span class="proporcion__pista proporcion__pista--escala">
            <span class="proporcion__relleno" style="width: ${(valor * 100).toFixed(1)}%"></span>
        </span>`;
}

function crearNotacion() {
    return `
        <aside class="recuadro recuadro--separado">
            <p><strong>Notación.</strong>
            <span class="mono">n</span> y <span class="mono">m</span>: cantidad de palabras de cada abstract ·
            <span class="mono">d</span>: dimensión del vector (FastText d = 300, MiniLM d = 384).
            Con k artículos se comparan k(k − 1)/2 pares, así que el costo de la matriz es esa cantidad
            de pares por la complejidad de un par. El tiempo incluye calcular toda la matriz; en FastText
            y MiniLM la primera comparación también incluye cargar el modelo.</p>
        </aside>`;
}

/** Color de una celda: blanco = 0 (nada en comun), azul = 1 (iguales). */
function colorDeFondo(valor) {
    // --heat es el azul del tema (claro u oscuro), definido en estilos.css
    return `rgba(var(--heat), ${(valor * 0.55).toFixed(2)})`;
}

/** Panel con la leyenda de colores y una matriz por algoritmo. */
function crearMatrices(resultados, articulos) {
    const k = articulos.length;
    // Con muchos articulos cada matriz necesita todo el ancho
    const claseAncha = k > 6 ? "matrices--anchas" : "";
    return `
        <section class="panel panel--separado">
            <div class="panel__encabezado">
                <div>
                    <h3>Matrices de similitud (${k} × ${k})</h3>
                    <p class="dato-suave">Filas y columnas: artículos seleccionados. El recuadro marca el par más parecido.</p>
                </div>
                <div class="leyenda-escala" aria-hidden="true">
                    <span>0,00</span><span class="leyenda-escala__gradiente"></span><span>1,00</span>
                    <span class="leyenda-escala__diagonal"></span><span>Diagonal</span>
                </div>
            </div>
            <div class="panel__cuerpo matrices ${claseAncha}">
                ${resultados.map((resultado) => crearMatriz(resultado, articulos)).join("")}
            </div>
        </section>`;
}

/** Matriz de un algoritmo: filas y columnas son los articulos comparados. */
function crearMatriz(resultado, articulos) {
    const par = parMasParecido(resultado.matriz);
    const etiquetaIa = ALGORITMOS_IA.includes(resultado.algoritmo)
        ? ` <span class="insignia insignia--suave">IA</span>` : "";
    const encabezados = articulos.map((articulo) =>
        `<th title="${escaparHtml(articulo.titulo)}">${numeroDelArticulo(articulo)}</th>`).join("");

    const filas = resultado.matriz.map((fila, i) => {
        const celdas = fila.map((valor, j) => {
            let clase = "";
            if (i === j) {
                clase = "celda-diagonal";
            } else if ((i === par.i && j === par.j) || (i === par.j && j === par.i)) {
                clase = "celda-maxima";
            }
            const fondo = i === j ? "" : `style="background-color: ${colorDeFondo(valor)}"`;
            return `<td class="numero ${clase}" ${fondo}>${valor.toFixed(3)}</td>`;
        }).join("");
        return `<tr><th title="${escaparHtml(articulos[i].titulo)}">${numeroDelArticulo(articulos[i])}</th>${celdas}</tr>`;
    }).join("");

    return `
        <div class="matriz">
            <div class="matriz__encabezado">
                <span><strong>${escaparHtml(resultado.algoritmo)}</strong>${etiquetaIa}</span>
                <span class="dato-suave">máx. ${par.valor.toFixed(3)}</span>
            </div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla matriz-similitud">
                    <thead><tr><th></th>${encabezados}</tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>
        </div>`;
}