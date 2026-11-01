/**
 * Vista "Busqueda": busca un patron en los 20 abstracts con KMP, Boyer-Moore y Rabin-Karp.
 *
 * El servidor responde con las POSICIONES de cada ocurrencia. Aqui se arma el HTML:
 * el abstract se corta en segmentos, cada segmento se pasa por escaparHtml (el texto
 * viene de la API y nunca debe interpretarse como HTML) y solo las coincidencias se
 * envuelven en <mark>. Asi el resaltado no abre ningun camino para inyectar codigo.
 */
import { buscarPatron } from "./api.js";
import { escaparHtml, mostrarCargando, mostrarError } from "./utilidades.js";

const PATRON_MAXIMO = 100; // el mismo limite del servidor

const contenedor = document.getElementById("contenido-busqueda");
const subtitulo = document.getElementById("resumen-busqueda");
const campo = document.getElementById("campo-patron");
const contadorCaracteres = document.getElementById("contador-patron");
const boton = document.getElementById("boton-buscar");

const ICONO_OK = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" aria-hidden="true"><path d="M5 12l5 5L20 7"></path></svg>';
const ICONO_ERROR = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"></path></svg>';

/** Formatea milisegundos con 5 decimales, al estilo colombiano: 0,00158 */
function formatearMs(valor) {
    return valor.toLocaleString("es-CO", { minimumFractionDigits: 5, maximumFractionDigits: 5 });
}

/** Numero con separador de miles al estilo colombiano: 24.913 */
function formatearEntero(valor) {
    return valor.toLocaleString("es-CO");
}

/** Numero del articulo, tomado del nombre del archivo: "12. A meta review.pdf" -> 12. */
function numeroDelArticulo(articulo) {
    return parseInt(articulo.archivo, 10);
}

/** Contador "7 / 100" debajo del campo. */
campo.addEventListener("input", () => {
    contadorCaracteres.textContent = `${campo.value.length} / ${PATRON_MAXIMO}`;
});

/** Se ejecuta al enviar el formulario (boton o Enter). */
export async function buscar() {
    const patron = campo.value;
    // El servidor tambien valida (responde 400); aqui solo se evita una peticion inutil
    if (patron.length === 0) {
        contenedor.innerHTML = crearVacio("Escriba el patrón que desea buscar.",
            "Por ejemplo: model, ChatGPT o higher education.");
        return;
    }
    if (patron.length > PATRON_MAXIMO) {
        contenedor.innerHTML = crearVacio(`El patrón no puede tener más de ${PATRON_MAXIMO} caracteres.`,
            "Acórtelo e intente de nuevo.");
        return;
    }

    boton.disabled = true; // evita lanzar otra busqueda mientras esta corre (alteraria los tiempos)
    boton.setAttribute("aria-busy", "true");
    mostrarCargando(contenedor, "Buscando y midiendo en el servidor…",
        "Cada algoritmo se ejecuta varias veces para medir el tiempo.");
    try {
        const resultado = await buscarPatron(patron);
        subtitulo.textContent = `KMP, Boyer-Moore y Rabin-Karp sobre los ${resultado.totalArticulos} abstracts · `
            + `n = ${formatearEntero(resultado.longitudTexto)} caracteres`;
        const hayOcurrencias = resultado.totalOcurrencias > 0;
        contenedor.innerHTML = `
            ${hayOcurrencias ? crearResultados(resultado) : crearSinOcurrencias(resultado)}
            ${crearTablaAnalisis(resultado)}`;
    } catch (error) {
        mostrarError(contenedor, error);
    } finally {
        boton.disabled = false;
        boton.removeAttribute("aria-busy");
    }
}

function crearVacio(titulo, detalle) {
    return `
        <div class="vacio">
            <p class="vacio__titulo">${escaparHtml(titulo)}</p>
            <p>${escaparHtml(detalle)}</p>
        </div>`;
}

/** Un patron de solo espacios se veria vacio: se muestra con puntos medios (·). */
function visibilizarEspacios(patron) {
    return patron.trim() === "" ? patron.replaceAll(" ", "·") : patron;
}

function crearSinOcurrencias(resultado) {
    return `
        <div class="vacio">
            <p class="vacio__titulo">El patrón no aparece en ninguno de los abstracts analizados.</p>
            <p>Revise mayúsculas y minúsculas o pruebe con un patrón más corto.
            Patrón buscado: <span class="mono">"${escaparHtml(visibilizarEspacios(resultado.patron))}"</span></p>
        </div>`;
}

/** Resultados por algoritmo (izquierda) y articulos con el patron resaltado (derecha). */
function crearResultados(resultado) {
    return `
        <div class="rejilla-busqueda">
            <div class="columna">
                ${crearAvisoCoinciden(resultado)}
                ${crearTablaAlgoritmos(resultado)}
            </div>
            ${crearListaArticulos(resultado)}
        </div>`;
}

function crearAvisoCoinciden(resultado) {
    if (resultado.coinciden) {
        return `
            <div class="aviso aviso--ok">
                ${ICONO_OK}
                <p><strong>Los tres algoritmos coinciden:</strong>
                ${resultado.totalOcurrencias} ocurrencias en ${resultado.articulosConOcurrencias}
                de ${resultado.totalArticulos} artículos.</p>
            </div>`;
    }
    return `
        <div class="aviso aviso--error">
            ${ICONO_ERROR}
            <p><strong>Los algoritmos no encuentran las mismas posiciones.</strong></p>
        </div>`;
}

/** Ocurrencias que encontro cada algoritmo en total y su tiempo de busqueda. */
function crearTablaAlgoritmos(resultado) {
    const masRapido = resultado.tiempos.reduce((mejor, t) =>
        t.busqueda.mediaMs < mejor.busqueda.mediaMs ? t : mejor);

    const filas = resultado.tiempos.map((t) => {
        const ocurrencias = resultado.articulos.reduce((suma, a) => suma + (a.ocurrencias[t.algoritmo] ?? 0), 0);
        const insignia = t === masRapido ? ` <span class="insignia insignia--ok">Más rápido</span>` : "";
        return `
            <tr>
                <td><strong>${escaparHtml(t.algoritmo)}</strong>${insignia}</td>
                <td class="numero">${ocurrencias}</td>
                <td class="numero">${formatearMs(t.busqueda.mediaMs)}</td>
            </tr>`;
    }).join("");

    return `
        <div class="tabla-desplazable tabla-desplazable--tarjeta">
            <table class="tabla">
                <caption>Resultados por algoritmo</caption>
                <thead><tr><th>Algoritmo</th><th class="numero">Ocurrencias</th><th class="numero">Búsqueda (ms)</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

/** Articulos con ocurrencias (con el patron resaltado) y, plegados, los que no las tienen. */
function crearListaArticulos(resultado) {
    const conOcurrencias = resultado.articulos.filter((a) => a.posiciones.length > 0);
    const sinOcurrencias = resultado.articulos.filter((a) => a.posiciones.length === 0);

    const tarjetas = conOcurrencias.map((a) => crearArticulo(a, resultado.longitudPatron)).join("");
    const resto = sinOcurrencias.length === 0 ? "" : `
        <details class="plegable">
            <summary>Artículos sin ocurrencias (${sinOcurrencias.length})</summary>
            <ul class="lista-simple">
                ${sinOcurrencias.map((a) => `<li>${numeroDelArticulo(a)}. ${escaparHtml(a.titulo ?? "")}</li>`).join("")}
            </ul>
        </details>`;

    return `
        <section class="panel">
            <div class="panel__encabezado"><h3>Artículos donde aparece el patrón</h3></div>
            <ol class="resultados-busqueda">${tarjetas}</ol>
            ${resto}
        </section>`;
}

/** Un articulo: numero, titulo, ocurrencias y el abstract con el patron resaltado. */
function crearArticulo(articulo, longitudPatron) {
    const cantidad = articulo.posiciones.length;
    const aviso = articulo.coinciden
        ? ""
        : ` <span class="insignia insignia--error">Los algoritmos difieren</span>`;
    return `
        <li class="resultado-busqueda">
            <div class="resultado-busqueda__encabezado">
                <p class="resultado-busqueda__titulo">
                    <span class="tarjeta-articulo__numero">${numeroDelArticulo(articulo)}</span>
                    ${escaparHtml(articulo.titulo ?? "")}
                </p>
                <span class="dato-suave">${cantidad} ${cantidad === 1 ? "ocurrencia" : "ocurrencias"}</span>${aviso}
            </div>
            <p class="resultado-busqueda__texto">${resaltar(articulo.resumen ?? "", articulo.posiciones, longitudPatron)}</p>
        </li>`;
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

/**
 * La API manda la complejidad como texto simple ("O(m^3)"). Aqui se escapa y solo
 * el exponente ^3 se convierte en <sup>3</sup>: se ve O(m³) sin permitir otro HTML.
 */
export function formatearComplejidad(texto) {
    return escaparHtml(texto).replace(/\^(\d+)/g, "<sup>$1</sup>");
}

/**
 * Tabla del analisis critico: complejidad del codigo frente al tiempo medido, separando
 * preprocesamiento y busqueda, con las colisiones de Rabin-Karp en la misma tabla.
 */
function crearTablaAnalisis(resultado) {
    const filas = resultado.tiempos.map((t) => {
        const sinColisiones = t.colisiones === null || t.colisiones === undefined;
        return `
            <tr>
                <td rowspan="2" class="celda-algoritmo"><strong>${escaparHtml(t.algoritmo)}</strong></td>
                ${crearCeldasEtapa("Preprocesamiento", t.complejidadPreprocesamiento, t.preprocesamiento)}
                <td class="numero dato-suave">—</td>
            </tr>
            <tr>
                ${crearCeldasEtapa("Búsqueda", t.complejidadBusqueda, t.busqueda)}
                <td class="numero ${sinColisiones ? "dato-suave" : ""}">${sinColisiones ? "—" : t.colisiones}</td>
            </tr>`;
    }).join("");

    return `
        <section class="panel panel--separado">
            <div class="panel__encabezado">
                <h3>Análisis crítico: complejidad y tiempo medido</h3>
                <span class="dato-suave mono">n = ${formatearEntero(resultado.longitudTexto)} · m = ${resultado.longitudPatron} · σ = ${formatearEntero(resultado.tamanoAlfabeto)}</span>
            </div>
            <div class="tabla-desplazable tabla-desplazable--interna">
                <table class="tabla tabla--analisis">
                    <thead>
                        <tr>
                            <th>Algoritmo</th>
                            <th>Etapa</th>
                            <th>Complejidad del código</th>
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
            <div class="panel__cuerpo">
                <ul class="lista-simple notas-analisis">
                    <li><span class="mono">n</span>: caracteres de los ${resultado.totalArticulos} abstracts juntos ·
                        <span class="mono">m</span>: largo del patrón ·
                        <span class="mono">σ</span>: caracteres distintos del texto.</li>
                    <li>Cada etapa se mide sobre los ${resultado.totalArticulos} abstracts completos, con calentamiento
                        y repeticiones. En KMP y Boyer-Moore el preprocesamiento depende solo del patrón (se hace
                        una vez); en Rabin-Karp se hace por abstract, porque el hash de la primera ventana
                        depende del texto.</li>
                    <li>El sufijo bueno de Boyer-Moore se construye desde su definición, por eso el preprocesamiento
                        es O(m<sup>3</sup>). Existe una construcción en O(m), pero con patrones cortos el costo es
                        mínimo y se prefirió un código fácil de explicar. Pruebe con un patrón de 100 caracteres
                        para ver cómo crece.</li>
                    <li>σ aparece en la tabla del carácter malo clásica, O(m + σ); aquí se usa un mapa con los
                        caracteres del patrón y cuesta O(m).</li>
                    <li>Una colisión de Rabin-Karp es un hash igual con texto distinto: obliga a comparar carácter
                        por carácter, pero nunca se cuenta como ocurrencia.</li>
                    <li>Si varias personas buscan a la vez, los tiempos pueden salir alterados.</li>
                </ul>
            </div>
        </section>`;
}

function crearCeldasEtapa(nombre, complejidad, medicion) {
    return `
        <td>${nombre}</td>
        <td class="mono">${formatearComplejidad(complejidad)}</td>
        <td class="numero dato-suave">${medicion.repeticiones}</td>
        <td class="numero"><strong>${formatearMs(medicion.mediaMs)}</strong></td>
        <td class="numero dato-suave">± ${formatearMs(medicion.desviacionMs)}</td>
        <td class="numero dato-suave">${formatearMs(medicion.minimoMs)}</td>`;
}