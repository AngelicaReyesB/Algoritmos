package co.edu.uniquindio.ranking.algoritmos;

/** Aplica las pruebas comunes a Counting Sort. */
class CountingSortTest extends PruebasAlgoritmoOrdenamiento {

    @Override
    protected AlgoritmoOrdenamiento crearAlgoritmo() {
        return new CountingSort();
    }
}