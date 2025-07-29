package main

import (
	"crypto/aes"
	"fmt"
)

// Bob the evaluator

func evaluateGarbledCircuit(inputs [][]byte, gates []Gate) []byte {
	n := len(inputs) // number of inputs
	m := len(gates)  // number of gates

	// array of signals have a size of n+m
	signals := make([][]byte, m+n)

	// setup inputs signals
	for i := 0; i < n; i++ {
		signals[i] = inputs[i]
		fmt.Printf("input %d=%x\n", i, signals[i][:2])
	}

	// Evaluate each gate in order
	for _, gate := range gates {
		// Get input signals for this gate
		a := signals[gate.in0]
		b := signals[gate.in1]

		// Extract selection bits from input signals
		// The first bit (MSB) of each signal determines which row to select
		sa := (a[0] & 0x80) >> 7
		sb := (b[0] & 0x80) >> 7

		// Calculate table index using selection bits
		tableIndex := sa*2 + sb

		// Create AES cipher using concatenated input signals as key
		cipher, _ := aes.NewCipher(append(a, b...))

		// Decrypt the selected row from the truth table
		output := make([]byte, 16)
		cipher.Decrypt(output, gate.table[tableIndex])

		// Store the decrypted output signal
		signals[gate.out] = output

		fmt.Printf("gate %d: in0=%d[%x] in1=%d[%x] sel=%d out=%x\n",
			gate.out, gate.in0, a[:2], gate.in1, b[:2], tableIndex, output[:2])
	}
	// the last signal is the output

	return signals[n+m-1]
}
