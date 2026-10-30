/**
 * Vista "Busqueda": busca un patron en los 20 abstracts con KMP, Boyer-Moore y Rabin-Karp.
 *
 * El servidor responde con las POSICIONES de cada ocurrencia. Aqui se arma el HTML:
 * el abstract se corta en segmentos, cada segmento se pasa por escaparHtml (el texto
 * viene de la API y nunca debe interpretarse como HTML) y solo las coincidencias se
 * envuelven en <mark>. Asi el resaltado no abre ningun camino para inyectar codigo.
 */
import { buscarPatron } from "./api.js";
import { escaparHtml, mostrarError } from "./utilidades.js";

const PATRON_MAXIMO = 100; // el mismo limite del servidor

const contenedor = document.getElementById("contenido-busqueda");
const campo = document.getElementById("campo-patron");
const boton = document.getElementById("boton-buscar");

/** Formatea milisegundos con 5 decimales, al estilo colombiano: 0,00158 */
function formatearMs(valor) {
    return valor.toLocaleString("es-CO", { minimumFractionDigits: 5, maximumFractionDigits: 5 });
}

/** Se ejecuta al enviar el formulario (boton o Enter). */
export async function buscar() {
    const patron = campo.value;
    // El servidor tambien valida (responde 400); aqui solo se evita una peticion inutil
    if (patron.length === 0) {
        contenedor.innerHTML = `<p class="mensaje-error">Escriba el patrón que desea buscar.</p>`;
        return;
    }
    if (patron.length > PATRON_MAXIMO) {
        contenedor.innerHTML =
            `<p class="mensaje-error">El patrón no puede tener más de ${PATRON_MAXIMO} caracteres.</p>`;
        return;
    }

    boton.disabled = true; // evita lanzar otra busqueda mientras esta corre (alteraria los tiempos)
    boton.textContent = "Buscando…";
    contenedor.innerHTML = "<p>Buscando y midiendo en el servidor…</p>";
    try {
        const resultado = await buscarPatron(patron);
        contenedor.innerHTML =
            crearResumen(resultado) +
            crearListaArticulos(resultado) +
            crearTablaTiempos(resultado.tiempos);
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        boton.disabled = false;
        boton.textContent = "Buscar";
    }
}

/** Patron, totales y si los tres algoritmos coincidieron. */
function crearResumen(resultado) {
    const validacion = resultado.coinciden
        ? `<span class="insignia insignia--ok">Los 3 algoritmos encuentran las mismas posiciones</span>`
        : `<span class="insignia insignia--error">Los algoritmos NO coinciden</span>`;
    return `
        <div class="resumen">
            <p>Patrón: <strong>${escaparHtml(visibilizarEspacios(resultado.patron))}</strong>
                (${resultado.longitudPatron} caracteres)</p>
            <p>Ocurrencias: <strong>${resultado.totalOcurrencias}</strong></p>
            <p>Artículos con ocurrencias:
                <strong>${resultado.articulosConOcurrencias} de ${resultado.totalArticulos}</strong></p>
            ${validacion}
        </div>`;
}

/** Un patron de solo espacios se veria vacio: se muestra con puntos medios (·). */
function visibilizarEspacios(patron) {
    return patron.trim() === "" ? patron.replaceAll(" ", "·") : patron;
}

/** Lista de los articulos: primero los que tienen ocurrencias, luego los demas. */
function crearListaArticulos(resultado) {
    const conOcurrencias = resultado.articulos.filter((a) => a.posiciones.length > 0);
    const sinOcurrencias = resultado.articulos.filter((a) => a.posiciones.length === 0);

    const lista = conOcurrencias.map((a) => crearArticulo(a, resultado.longitudPatron)).join("");
    const resto = sinOcurrencias.length === 0 ? "" : `
        <details class="articulo">
            <summary>Artículos sin ocurrencias (${sinOcurrencias.length})</summary>
            <ul class="lista-simple">
                ${sinOcurrencias.map((a) => `<li>${escaparHtml(a.archivo)} · ${escaparHtml(a.titulo ?? "")}</li>`).join("")}
            </ul>
        </details>`;

    if (conOcurrencias.length === 0) {
        return `<h3>Artículos</h3><p class="nota">El patrón no aparece en ningún abstract.</p>${resto}`;
    }
    return `<h3>Artículos con ocurrencias</h3>${lista}${resto}`;
}

/** Un articulo: archivo, titulo, ocurrencias de cada algoritmo y el abstract resaltado. */
function crearArticulo(articulo, longitudPatron) {
    const porAlgoritmo = Object.entries(articulo.ocurrencias)
        .map(([algoritmo, cantidad]) => `${escaparHtml(algoritmo)}: <strong>${cantidad}</strong>`)
        .join(" · ");
    const aviso = articulo.coinciden
        ? ""
        : `<span class="insignia insignia--error">Los algoritmos difieren</span>`;
    return `
        <article class="articulo">
            <div class="articulo__encabezado">
                <h4 class="articulo__archivo">${escaparHtml(articulo.archivo)}</h4>
                <span class="insignia insignia--aviso">${articulo.posiciones.length} ocurrencias</span>
                ${aviso}
            </div>
            <p class="articulo__titulo">${escaparHtml(articulo.titulo ?? "")}</p>
            <p class="nota">${porAlgoritmo}</p>
            <p class="articulo__resumen">${resaltar(articulo.resumen ?? "", articulo.posiciones, longitudPatron)}</p>
        </article>`;
}

/**
 * Junta las ocurrencias en intervalos [inicio, fin) y fusiona las que se tocan o se solapan.
 * Ejemplo: "aa" en "aaaa" ocurre en 0, 1 y 2 -> [0,2), [1,3), [2,4) -> un solo intervalo [0,4).
 * Asi nunca se anidan marcas.
 *
 * @param posiciones      donde empieza cada ocurrencia, en orden creciente
 * @param longitudPatron  cuantos caracteres ocupa cada ocurrencia
 */
export function fusionarIntervalos(posiciones, longitudPatron) {
    const intervalos = [];
    for (const inicio of posiciones) {
        const fin = inicio + longitudPatron;
        const ultimo = intervalos[intervalos.length - 1];
        if (ultimo && inicio <= ultimo.fin) {
            ultimo.fin = Math.max(ultimo.fin, fin);
        } else {
            intervalos.push({ inicio, fin });
        }
    }
    return intervalos;
}

/**
 * Devuelve el texto como HTML seguro, con las coincidencias dentro de <mark>.
 * Cada trozo (con o sin marca) se escapa por separado ANTES de armar las etiquetas:
 * las unicas etiquetas del resultado son las <mark> que escribe este codigo.
 */
export function resaltar(texto, posiciones, longitudPatron) {
    let html = "";
    let desde = 0;
    for (const { inicio, fin } of fusionarIntervalos(posiciones, longitudPatron)) {
        html += escaparHtml(texto.slice(desde, inicio));
        html += `<mark>${escaparHtml(texto.slice(inicio, fin))}</mark>`;
        desde = fin;
    }
    return html + escaparHtml(texto.slice(desde));
}

/** Tabla de tiempos: dos filas por algoritmo (preprocesamiento y busqueda). */
function crearTablaTiempos(tiempos) {
    const filas = tiempos.map((t) => {
        const colisiones = t.colisiones === null || t.colisiones === undefined ? "—" : t.colisiones;
        return `
            <tr>
                <td rowspan="2"><strong>${escaparHtml(t.algoritmo)}</strong></td>
                ${crearCeldasEtapa("Preprocesamiento", t.preprocesamiento)}
                <td class="numero">—</td>
            </tr>
            <tr class="fila-destacada">
                ${crearCeldasEtapa("Búsqueda", t.busqueda)}
                <td class="numero">${colisiones}</td>
            </tr>`;
    }).join("");

    return `
        <h3>Tiempos de ejecución</h3>
        <div class="tabla-desplazable">
            <table class="tabla">
                <thead>
                    <tr>
                        <th>Algoritmo</th>
                        <th>Etapa</th>
                        <th class="numero">Repeticiones</th>
                        <th class="numero">Media (ms)</th>
                        <th class="numero">Desv. estándar (ms)</th>
                        <th class="numero">Mínimo (ms)</th>
                        <th class="numero">Colisiones</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>
        <p class="nota nota--tabla">
            Cada etapa se mide sobre los 20 abstracts completos, con calentamiento y repeticiones.
            En KMP y Boyer-Moore el preprocesamiento depende solo del patrón (se hace una vez);
            en Rabin-Karp se hace por abstract, porque el hash de la primera ventana depende del texto.
            Una colisión es un hash igual con texto distinto. Si varias personas buscan a la vez,
            los tiempos pueden salir alterados.
        </p>`;
}

function crearCeldasEtapa(nombre, medicion) {
    return `
        <td>${nombre}</td>
        <td class="numero">${medicion.repeticiones}</td>
        <td class="numero">${formatearMs(medicion.mediaMs)}</td>
        <td class="numero">${formatearMs(medicion.desviacionMs)}</td>
        <td class="numero">${formatearMs(medicion.minimoMs)}</td>`;
}
