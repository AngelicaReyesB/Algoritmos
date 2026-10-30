package co.edu.uniquindio.ranking.service;

import co.edu.uniquindio.ranking.medicion.MedicionManual;

/** Permite que las pruebas de otros paquetes creen el servicio con pocas repeticiones. */
public final class ServicioBusquedaTestApoyo {

    private ServicioBusquedaTestApoyo() {
    }

    public static ServicioBusqueda servicioCon(String... resumenes) {
        return new ServicioBusqueda(ServicioBusquedaTest.repositorioCon(resumenes), new MedicionManual(), 1, 3);
    }
}
