package co.edu.uniquindio.ranking.algoritmos;

/** Aplica las pruebas comunes a Radix Sort. */
class RadixSortTest extends PruebasAlgoritmoOrdenamiento {

    @Override
    protected AlgoritmoOrdenamiento crearAlgoritmo() {
        return new RadixSort();
    }
}