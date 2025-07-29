# import sys

# def encrypt(n, e, m):
#     # m is the plaintext presented as an integer
#     c = pow(m, e, n)
#     return c

# def main():
#     if len(sys.argv) != 4:
#         print("Usage: python encrypt.py <n> <e> <m>")
#         sys.exit(1)

#     n = int(sys.argv[1])
#     e = int(sys.argv[2])
#     m = int(sys.argv[3])
#     ciphertext = encrypt(n, e, m)
#     print(ciphertext)

# if __name__ == "__main__":
#     main()

import sys

def encrypt(n, e, m):
    # Perform RSA encryption
    # m is the plaintext presented as an integer
    # n is the modulus component of the public key
    # e is the exponent component of the public key
    c = pow(m, e, n)  # Compute m^e mod n
    return c

def main():
    if len(sys.argv) != 4:
        print("Usage: python encrypt.py <n> <e> <m>")
        sys.exit(1)
    
    n = int(sys.argv[1])
    e = int(sys.argv[2])
    m = int(sys.argv[3])
    
    ciphertext = encrypt(n, e, m)
    print(ciphertext)

if __name__ == "__main__":
    main()

