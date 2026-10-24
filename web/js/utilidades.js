/**
 * Funciones de apoyo que usan varias vistas.
 */

/**
 * Convierte los caracteres especiales de HTML (< > & " ') en texto seguro.
 * Se usa siempre que se inserta en la pagina un texto que viene de la API,
 * para que nunca se interprete como codigo HTML.
 */
export function escaparHtml(texto) {
    return String(texto)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

/** Muestra un mensaje de error dentro de un contenedor. */
export function mostrarError(contenedor, error) {
    contenedor.innerHTML = `<p class="mensaje-error">No se pudo cargar: ${escaparHtml(error.message)}</p>`;
}


/**
 * Grafico de barras horizontales hecho con <div> (version sin librerias).
 * Cada barra mide (valor / valor maximo) * 100 % del ancho disponible.
 *
 * @param elementos      arreglo de { etiqueta, valor }
 * @param formatearValor funcion que convierte el valor en el texto que se muestra
 */
export function crearGraficoBarras(elementos, formatearValor = (valor) => valor) {
    const maximo = Math.max(...elementos.map((e) => e.valor));
    const barras = elementos.map((e) => {
        const ancho = maximo > 0 ? (e.valor / maximo) * 100 : 0;
        return `
            <div class="barra">
                <span class="barra__etiqueta">${escaparHtml(e.etiqueta)}</span>
                <div class="barra__pista">
                    <div class="barra__relleno" style="width: ${ancho}%"></div>
                </div>
                <span class="barra__valor">${escaparHtml(formatearValor(e.valor))}</span>
            </div>`;
    }).join("");
    return `<div class="grafico">${barras}</div>`;
}