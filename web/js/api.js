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

export function obtenerArticulos() {
    return obtenerJson("/api/articulos");
}

export function obtenerRanking(top = 15) {
    return obtenerJson(`/api/ranking?top=${top}`);
}

export function obtenerTiempos() {
    return obtenerJson("/api/tiempos");
}