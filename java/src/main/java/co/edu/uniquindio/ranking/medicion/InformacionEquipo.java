package co.edu.uniquindio.ranking.medicion;

import java.lang.management.ManagementFactory;

/**
 * Caracteristicas del equipo donde se hicieron las mediciones.
 * Se reportan en el informe porque los tiempos dependen del hardware.
 *
 * @param procesador        identificador del procesador (en Windows, variable PROCESSOR_IDENTIFIER)
 * @param nucleos           nucleos logicos disponibles para Java
 * @param memoriaTotalMb    memoria RAM total del equipo en MB
 * @param sistemaOperativo  nombre y version del sistema operativo
 * @param versionJava       version de Java y de la maquina virtual
 */
public record InformacionEquipo(String procesador, int nucleos, long memoriaTotalMb,
                                String sistemaOperativo, String versionJava) {

    public static InformacionEquipo obtener() {
        String procesador = System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "desconocido");
        int nucleos = Runtime.getRuntime().availableProcessors();

        long memoriaTotalMb = 0;
        if (ManagementFactory.getOperatingSystemMXBean()
                instanceof com.sun.management.OperatingSystemMXBean sistema) {
            memoriaTotalMb = sistema.getTotalMemorySize() / (1024 * 1024);
        }

        String sistemaOperativo = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String versionJava = System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")";

        return new InformacionEquipo(procesador, nucleos, memoriaTotalMb, sistemaOperativo, versionJava);
    }
}