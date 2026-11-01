# Publicar la aplicación con Ngrok (DEP-02)

Ngrok crea un túnel HTTPS desde internet hasta la aplicación que corre en este equipo.
La base de datos **no** se expone: el túnel apunta solo a la aplicación (`app:8080`).

## URL pública

> **URL:** https://ranked-erased-clothing.ngrok-free.dev
> Vigente mientras el equipo y los contenedores estén encendidos (el dominio es fijo gracias a `NGROK_DOMAIN`).

La primera vez, Ngrok (plan gratuito) muestra una página de aviso: pulsar **Visit Site**.

## Requisitos (una sola vez)

1. Docker Desktop instalado.
2. Cuenta gratuita en https://ngrok.com y su *authtoken* (panel → *Your Authtoken*).
3. Opcional: en *Domains* reclamar el dominio gratuito de la cuenta, para que la URL no cambie.
4. Copiar `.env.example` como `.env` y completar `DB_PASSWORD`, `NGROK_AUTHTOKEN` y, si se quiere, `NGROK_DOMAIN`.

## Levantar

```bash
docker compose --profile publico up -d --build
docker compose ps                      # postgres, app y ngrok en "running"
docker compose logs -f app             # esperar "Started ... in N seconds"
```

La URL pública aparece en http://localhost:4040 (panel de Ngrok) o con
`docker compose logs ngrok | grep -i url=`.

## Variables de entorno

| Variable | Dónde se usa | Para qué |
|---|---|---|
| `DB_URL` | `app` (compose) | `jdbc:postgresql://postgres:5432/ranking`; `postgres` es el nombre del servicio |
| `DB_USER`, `DB_PASSWORD` | `app` y `postgres` | credenciales; la clave viene del `.env` |
| `JAVA_OPTS` | `app` | `-Xmx350m`, tope del heap de Java |
| `NGROK_AUTHTOKEN` | `ngrok` | identifica la cuenta |
| `NGROK_DOMAIN` | `ngrok` | opcional, URL fija |

## Prueba desde la URL pública

Desde un celular con datos móviles o desde otra red (no desde el mismo equipo):

1. Abrir la URL y comprobar que carga con candado HTTPS.
2. Recorrer **las seis pestañas**; en cada una ejecutar su acción principal y confirmar que
   no aparece un mensaje de error (abrir F12 → Consola para ver errores en rojo).
   Anotar el resultado:

| Pestaña | Para qué sirve | Resultado desde la URL pública |
|---|---|---|
| Artículos | Consultar el corpus bibliométrico | Correcto |
| Top 15 | Ver los términos/artículos mejor posicionados | Correcto |
| Tiempos | Rendimiento de los algoritmos y procesos (`/api/tiempos`) | Correcto, sin errores de memoria |
| Similitud | Comparar artículos con FastText y MiniLM (20 artículos) | Correcto |
| Agrupamiento | Organizar artículos según su similitud (20 artículos) | Correcto |
| Búsqueda | Localizar patrones con KMP, Boyer-Moore y Rabin-Karp | Correcto |

Probado también desde un celular con datos móviles.

3. Comprobar la API directamente (cambiar `URL`):

```bash
curl -s "URL/api/articulos" | head -c 300
curl -s "URL/api/ranking?top=5"
curl -s "URL/api/busqueda?patron=model" | head -c 400
```

## Problemas encontrados y cómo se resolvieron

**502 Bad Gateway al abrir la URL.** La aplicación no arrancaba porque la contraseña de `.env`
no coincidía con la guardada en PostgreSQL: el volumen conserva la clave con la que se creó la base,
y cambiar `.env` después no la modifica. Solución, sin borrar el volumen ni los datos:

```bash
docker exec -it ranking-postgres psql -U ranking -d ranking -c "ALTER USER ranking WITH PASSWORD 'la_clave_del_.env';"
docker compose restart app
```

**Memoria.** Con MiniLM cargado, la aplicación llegaba a 502,9 MiB de 512 (98 %). Con `-Xmx256m` o
`-Xmx300m` fallaba con `OutOfMemoryError` al cargar el modelo. Solución: los vectores de MiniLM se
calculan una vez y se guardan en `data/embeddings/minilm-vectores.json`; el contenedor los lee del
archivo y no carga el modelo. Son los mismos vectores, calculados por el mismo modelo.

| Configuración | Memoria (RSS) |
|---|---|
| `-Xmx350m`, MiniLM cargado | 502,9 MiB |
| `-Xmx256m`, vectores en archivo | 253,8 MiB |

Medido con `/api/tiempos` y Similitud y Agrupamiento con 20 artículos ya ejecutados, sin errores de memoria.
Opciones de JVM finales: `-Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -Xss512k -XX:ReservedCodeCacheSize=48m`.

## Verificar la memoria (límite de 512 MB)

Con la aplicación levantada y después de usar las seis pestañas:

```bash
docker stats --no-stream ranking-app ranking-postgres ranking-ngrok
```

La columna `MEM USAGE / LIMIT` de `ranking-app` debe quedar **por debajo de ~450 MiB** (límite 512).
La memoria real (sin caché de disco) se ve con:

```bash
docker exec ranking-app sh -c "grep -E '^(total_)?rss ' /sys/fs/cgroup/memory/memory.stat"
```

Si supera ese valor, o si Docker reinicia el contenedor (`docker inspect -f '{{.State.OOMKilled}}' ranking-app`
devuelve `true`), hay que reducir MiniLM: ver la sección siguiente.

## Alternativa no usada: versión cuantizada `-q`

No hizo falta (con los vectores en archivo bastó). Queda como respaldo si algún día se necesita calcular vectores en el servidor.

La versión cuantizada guarda los pesos como enteros de 8 bits: ocupa unas cuatro veces menos y
da vectores casi iguales (cambian en decimales).

1. En `java/pom.xml` cambiar el artefacto:

   ```xml
   <artifactId>langchain4j-embeddings-all-minilm-l6-v2-q</artifactId>
   ```

2. En el código donde se crea el modelo, cambiar la clase y el `import`:

   ```java
   // antes
   import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
   // después
   import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
   ```

   (para encontrarlo: `grep -rn "AllMiniLmL6V2" java/src`).

3. **Regenerar los vectores guardados** en `data/embeddings`, para que los vectores de los
   artículos y los de las consultas salgan del mismo modelo. Si no, la similitud mezclaría
   dos modelos y daría números ligeramente distintos.
4. `docker compose --profile publico up -d --build` y repetir la medición de memoria.

## Detener

```bash
docker compose --profile publico down        # la base se conserva
```