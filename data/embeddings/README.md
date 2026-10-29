# Vectores de palabras

`fasttext-corpus.vec` contiene solo las palabras de los 20 abstracts del corpus,
tomadas del modelo preentrenado de FastText:

- Modelo: wiki-news-300d-1M (Wikipedia 2017, UMBC webbase y noticias de statmt.org; 1 millón de palabras, 300 dimensiones)
- Autores: Mikolov, T., Grave, E., Bojanowski, P., Puhrsch, C. y Joulin, A. (2018). Advances in Pre-Training Distributed Word Representations. LREC 2018.
- Fuente: https://fasttext.cc/docs/en/english-vectors.html
- Licencia: Creative Commons Attribution-Share-Alike 3.0 (CC BY-SA 3.0)

El archivo se generó con `FiltrarFastText` (java/src/test/.../embeddings).