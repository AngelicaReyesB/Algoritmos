package co.edu.uniquindio.ranking.algoritmos;

/** Aplica las pruebas comunes a Bucket Sort. */
class BucketSortTest extends PruebasAlgoritmoOrdenamiento {

    @Override
    protected AlgoritmoOrdenamiento crearAlgoritmo() {
        return new BucketSort();
    }
}