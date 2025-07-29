import random
import sys

def is_prime(n, k=5):  # Number of iterations, higher k means more accuracy
    if n <= 1:
        return False
    elif n <= 3:
        return True
    elif n % 2 == 0 or n % 3 == 0:
        return False

    # Write (n - 1) as 2^r * d
    r, d = 0, n - 1
    while d % 2 == 0:
        d //= 2
        r += 1

    def miller_test(d, n):
        a = random.randint(2, n - 2)
        x = pow(a, d, n)  # Compute a^d % n
        if x == 1 or x == n - 1:
            return True
        while d != n - 1:
            x = (x * x) % n
            d *= 2
            if x == 1:
                return False
            if x == n - 1:
                return True
        return False

    # Perform the test k times
    for _ in range(k):
        if not miller_test(d, n):
            return False
    return True

def main():
    if len(sys.argv) != 2:
        print("Usage: python primecheck.py <number>")
        sys.exit(1)

    number = int(sys.argv[1])
    result = is_prime(number)
    print("True" if result else "False")

if __name__ == "__main__":
    main()
