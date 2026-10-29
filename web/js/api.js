/**
 * Funciones para consultar la API REST del backend.
 * Todas las vistas usan estas funciones en lugar de llamar a fetch directamente.
 */

/**
 * Hace una peticion GET y devuelve la respuesta convertida desde JSON.
 * Si el servidor responde con error (400, 500...), lanza una excepcion
 * con el mensaje que envio el backend.
 */
export async function obtenerJson(url) {
    const respuesta = await fetch(url);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        throw new Error(datos.message || `Error ${respuesta.status}`);
    }
    return datos;
}

/**
 * Hace una peticion POST enviando "cuerpo" como JSON y devuelve la respuesta.
 * Maneja los errores igual que obtenerJson.
 */
export async function enviarJson(url, cuerpo) {
    const respuesta = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cuerpo),
    });
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        throw new Error(datos.message || `Error ${respuesta.status}`);
    }
    return datos;
}

export function obtenerArticulos() {
    return obtenerJson("/api/articulos");
}

export function obtenerRanking(top = 15) {
    return obtenerJson(`/api/ranking?top=${top}`);
}

export function obtenerTiempos() {
    return obtenerJson("/api/tiempos");
}

export function compararSimilitud(ids) {
    return enviarJson("/api/similitud", { ids });
}

export function obtenerAgrupamiento(similitud = "tfidf") {
    return obtenerJson(`/api/agrupamiento?similitud=${encodeURIComponent(similitud)}`);
}