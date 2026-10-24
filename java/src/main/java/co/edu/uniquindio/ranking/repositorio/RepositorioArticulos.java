package co.edu.uniquindio.ranking.repositorio;

import co.edu.uniquindio.ranking.modelo.Articulo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a la tabla "articulos" con SQL escrito a mano (JdbcTemplate).
 */
@Repository
public class RepositorioArticulos {

    private final JdbcTemplate jdbc;

    public RepositorioArticulos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserta el articulo, o lo actualiza si ya existe un registro con el mismo archivo.
     *
     * Regla para no perder correcciones manuales:
     * - Un articulo corregido a mano (manual = TRUE) NO se sobrescribe con
     *   una extraccion automatica.
     * - Una correccion manual SI reemplaza una extraccion automatica.
     */
    public void guardar(Articulo articulo) {
        String sql = """
                INSERT INTO articulos (archivo, titulo, autores, resumen, manual)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (archivo) DO UPDATE
                SET titulo  = EXCLUDED.titulo,
                    autores = EXCLUDED.autores,
                    resumen = EXCLUDED.resumen,
                    manual  = EXCLUDED.manual
                WHERE articulos.manual = FALSE OR EXCLUDED.manual = TRUE
                """;
        jdbc.update(sql, articulo.archivo(), articulo.titulo(), articulo.autores(),
                articulo.resumen(), articulo.manual());
    }

    /** Todos los articulos, ordenados por nombre de archivo. */
    public List<Articulo> listar() {
        String sql = "SELECT id, archivo, titulo, autores, resumen, manual FROM articulos ORDER BY archivo";
        return jdbc.query(sql, (fila, numeroFila) -> new Articulo(
                fila.getInt("id"),
                fila.getString("archivo"),
                fila.getString("titulo"),
                fila.getString("autores"),
                fila.getString("resumen"),
                fila.getBoolean("manual")));
    }

    /** Solo los abstracts, en el mismo orden. Es lo que necesita el conteo de frecuencias. */
    public List<String> listarResumenes() {
        return jdbc.queryForList("SELECT resumen FROM articulos ORDER BY archivo", String.class);
    }
}