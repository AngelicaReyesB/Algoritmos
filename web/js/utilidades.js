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