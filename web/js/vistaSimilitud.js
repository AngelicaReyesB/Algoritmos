/**
 * Vista "Similitud" (Requerimiento 1).
 *
 * 1. Muestra los 20 articulos con una casilla para elegirlos.
 * 2. Envia los ids elegidos a POST /api/similitud.
 * 3. Muestra una tabla de tiempos y una matriz de similitud por cada algoritmo.
 *    Cada celda se colorea: mas azul = mas parecidos.
 */
import { compararSimilitud, obtenerArticulos } from "./api.js";
import { escaparHtml, mostrarError } from "./utilidades.js";

const listaArticulos = document.getElementById("seleccion-articulos");
const contenedor = document.getElementById("contenido-similitud");
const botonComparar = document.getElementById("boton-comparar");

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Dibuja una casilla por cada articulo del corpus. */
export async function cargarSeleccionArticulos() {
    listaArticulos.innerHTML = "<p>Cargando artículos…</p>";
    try {
        const articulos = await obtenerArticulos();
        listaArticulos.innerHTML = articulos.map((articulo) => `
            <label class="seleccion__opcion">
                <input type="checkbox" value="${articulo.id}">
                <span><strong>${numeroDelArticulo(articulo)}.</strong> ${escaparHtml(articulo.titulo)}</span>
            </label>`).join("");
    } catch (error) {
        mostrarError(listaArticulos, error);
    }
}

/** Marca todas las casillas; si ya estaban todas marcadas, las desmarca. */
export function seleccionarTodos() {
    const casillas = [...listaArticulos.querySelectorAll("input[type=checkbox]")];
    const todasMarcadas = casillas.every((casilla) => casilla.checked);
    casillas.forEach((casilla) => {
        casilla.checked = !todasMarcadas;
    });
}

/** Envia los articulos marcados a la API y muestra los resultados. */
export async function compararSeleccionados() {
    const ids = [...listaArticulos.querySelectorAll("input:checked")].map((casilla) => Number(casilla.value));
    if (ids.length < 2) {
        contenedor.innerHTML = `<p class="mensaje-error">Seleccione al menos 2 artículos.</p>`;
        return;
    }

    botonComparar.disabled = true;
    contenedor.innerHTML = "<p>Calculando… La primera vez tarda unos segundos porque se cargan los modelos de IA.</p>";
    try {
        const datos = await compararSimilitud(ids);
        contenedor.innerHTML =
            crearLeyenda(datos.articulos) +
            crearTablaResumen(datos.resultados, datos.articulos) +
            datos.resultados.map((resultado) => crearMatriz(resultado, datos.articulos)).join("");
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        botonComparar.disabled = false;
    }
}

/** Lista de los articulos comparados: numero y titulo. */
function crearLeyenda(articulos) {
    const elementos = articulos.map((articulo) =>
        `<li><strong>${numeroDelArticulo(articulo)}.</strong> ${escaparHtml(articulo.titulo)}</li>`).join("");
    return `<h3>Artículos comparados</h3><ul class="leyenda-articulos">${elementos}</ul>`;
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

/** Tabla con el tiempo de cada algoritmo y su par de articulos mas parecido. */
function crearTablaResumen(resultados, articulos) {
    const filas = resultados.map((resultado) => {
        const par = parMasParecido(resultado.matriz);
        const nombrePar = `${numeroDelArticulo(articulos[par.i])} y ${numeroDelArticulo(articulos[par.j])}`;
        return `
            <tr>
                <td>${escaparHtml(resultado.algoritmo)}</td>
                <td class="numero">${resultado.tiempoMs.toFixed(3)}</td>
                <td class="numero">${nombrePar}</td>
                <td class="numero">${par.valor.toFixed(3)}</td>
            </tr>`;
    }).join("");
    return `
        <h3>Resumen</h3>
        <p class="nota">El tiempo incluye calcular toda la matriz. En FastText y MiniLM la primera
        comparación también incluye cargar el modelo.</p>
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead><tr><th>Algoritmo</th><th>Tiempo (ms)</th><th>Par más parecido</th><th>Similitud</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

/** Color de una celda: blanco = 0 (nada en comun), azul = 1 (iguales). */
function colorDeFondo(valor) {
    return `rgba(36, 89, 194, ${(valor * 0.55).toFixed(2)})`;
}

/** Matriz de un algoritmo: filas y columnas son los articulos comparados. */
function crearMatriz(resultado, articulos) {
    const encabezados = articulos.map((articulo) =>
        `<th title="${escaparHtml(articulo.titulo)}">${numeroDelArticulo(articulo)}</th>`).join("");

    const filas = resultado.matriz.map((fila, i) => {
        const celdas = fila.map((valor) =>
            `<td class="numero" style="background-color: ${colorDeFondo(valor)}">${valor.toFixed(3)}</td>`).join("");
        return `<tr><th title="${escaparHtml(articulos[i].titulo)}">${numeroDelArticulo(articulos[i])}</th>${celdas}</tr>`;
    }).join("");

    return `
        <h3>${escaparHtml(resultado.algoritmo)}</h3>
        <div class="tabla-desplazable">
            <table class="tabla matriz-similitud">
                <thead><tr><th></th>${encabezados}</tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}