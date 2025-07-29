import random
import sys
from primecheck import is_prime

def generate_large_prime(bits):
    # Reuse the is_prime function from above, ensure it's defined in this script or imported
    def is_probable_prime(n, k=5):
        return is_prime(n, k)

    p = random.getrandbits(bits)
    p |= (1 << bits - 1) | 1  # Ensure it's odd and has the exact number of bits

    while not is_probable_prime(p, 5):
        p += 2
    return p

def main():
    if len(sys.argv) != 2:
        print("Usage: python primegen.py <bits>")
        sys.exit(1)

    bits = int(sys.argv[1])
    prime = generate_large_prime(bits)
    print(prime)

if __name__ == "__main__":
    main()
