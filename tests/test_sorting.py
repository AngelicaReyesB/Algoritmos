import random
import unittest

from src.sorting import ALGORITHMS, bucket_sort, counting_sort, radix_sort


def reference(pairs):
    # Solo para VERIFICAR: orden descendente por frecuencia, estable (empates en orden de entrada)
    return sorted(pairs, key=lambda p: -p[1])


class TestSorting(unittest.TestCase):
    def test_empty_and_single(self):
        for fn in ALGORITHMS.values():
            self.assertEqual(fn([]), [])
            self.assertEqual(fn([("a", 5)]), [("a", 5)])

    def test_all_equal_keeps_order(self):
        data = [(f"w{i}", 3) for i in range(10)]
        for fn in ALGORITHMS.values():
            self.assertEqual(fn(data), data)

    def test_random_matches_reference_and_is_stable(self):
        rng = random.Random(42)
        for _ in range(200):
            n = rng.randint(1, 300)
            hi = rng.choice([3, 10, 100, 5000])
            data = [(f"w{i}", rng.randint(1, hi)) for i in range(n)]
            expected = reference(data)
            for name, fn in ALGORITHMS.items():
                self.assertEqual(fn(data), expected, msg=name)

    def test_input_not_mutated(self):
        data = [("a", 2), ("b", 9), ("c", 2)]
        copy = list(data)
        for fn in (counting_sort, radix_sort, bucket_sort):
            fn(data)
            self.assertEqual(data, copy)


if __name__ == "__main__":
    unittest.main()
