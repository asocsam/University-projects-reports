import sys

def decrypt(n, d, c):
    # Perform RSA decryption
    # c is the ciphertext as an integer
    # n is the modulus component of the private key
    # d is the exponent component of the private key
    m = pow(c, d, n)  # Compute c^d mod n
    return m

def main():
    if len(sys.argv) != 4:
        print("Usage: python decrypt.py <n> <d> <c>")
        sys.exit(1)
    
    n = int(sys.argv[1])
    d = int(sys.argv[2])
    c = int(sys.argv[3])
    
    plaintext = decrypt(n, d, c)
    print(plaintext)

if __name__ == "__main__":
    main()
