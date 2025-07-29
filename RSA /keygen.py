import sys
from primecheck import is_prime

def egcd(a, b):
    if a == 0:
        return (b, 0, 1)
    else:
        g, y, x = egcd(b % a, a)
        return (g, x - (b // a) * y, y)

def modinv(a, m):
    g, x, _ = egcd(a, m)
    if g != 1:
        raise Exception('Modular inverse does not exist')
    else:
        return x % m

def keygen(p, q):
    if not (is_prime(p) and is_prime(q)):
        raise ValueError("Both numbers must be prime.")
    elif p == q:
        raise ValueError("p and q cannot be the same")
    
    n = p * q
    phi = (p - 1) * (q - 1)

    # Choose e
    e = 3
    while egcd(e, phi)[0] != 1:
        e += 2

    # Compute d
    d = modinv(e, phi)

    return ((n, e), (n, d))

def main():
    if len(sys.argv) != 3:
        print("Usage: python keygen.py <prime1> <prime2>")
        sys.exit(1)

    p = int(sys.argv[1])
    q = int(sys.argv[2])
    public_key, private_key = keygen(p, q)
    print("Public Key:", public_key)
    print("Private Key:", private_key)

if __name__ == "__main__":
    main()

