package main

import (
	"bytes"
	"fmt"
)

func encryptInputs(inputs []int, wires []Wire) [][]byte {
	signals := make([][]byte, len(inputs))
	for i, input := range inputs {
		signals[i] = wires[i].v[input]
	}
	return signals
}

func decryptOutput(signal []byte, last Wire) int {
	if bytes.Compare(signal, last.v[0]) == 0 {
		return 0
	} else if bytes.Compare(signal, last.v[1]) == 0 {
		return 1
	}
	panic("invalid output signal")
}

func doTest(t string, gates []Gate, inputs []int, expected int) {
	wires := garbleCircuit(gates)
	signals := encryptInputs(inputs, wires)
	signal := evaluateGarbledCircuit(signals, gates)
	output := decryptOutput(signal, wires[len(wires)-1])
	if output != expected {
		panic("incorrect output for test " + t)
	} else {
		fmt.Printf(">>>>>>>>>>>>>>>> test %s pass!\n", t)
	}
}

func test123() {
	gates := []Gate{
		makeGate("NAND", 0, 1, 2),
	}
	doTest("1", gates, []int{0, 0}, 1)
	doTest("2", gates, []int{0, 1}, 1)
	doTest("3", gates, []int{1, 0}, 1)
}

func test4() {
	gates := []Gate{
		makeGate("NAND", 0, 1, 2),
		makeGate("NAND", 2, 2, 3),
	}
	doTest("4", gates, []int{1, 1}, 1)
}

func test567() {
	gates := []Gate{
		makeGate("NAND", 0, 0, 2),
		makeGate("NAND", 1, 1, 3),
		makeGate("NAND", 2, 3, 4),
	}
	doTest("5", gates, []int{0, 0}, 0)
	doTest("6", gates, []int{0, 1}, 1)
	doTest("7", gates, []int{1, 1}, 1)
}

func test890() {
	gates := []Gate{
		makeGate("NAND", 0, 1, 4),
		makeGate("NAND", 2, 3, 5),
		makeGate("NAND", 4, 5, 6),
	}
	doTest("8", gates, []int{0, 0, 0, 0}, 0)
	doTest("9", gates, []int{0, 1, 0, 0}, 0)
	doTest("10", gates, []int{1, 1, 1, 0}, 1)
}

func testNOR() {
	gates := []Gate{
		makeGate("NOR", 0, 1, 2),
	}
	doTest("11", gates, []int{0, 0}, 1) // NOR(0,0) = 1
	doTest("12", gates, []int{0, 1}, 0) // NOR(0,1) = 0
	doTest("13", gates, []int{1, 0}, 0) // NOR(1,0) = 0
	doTest("14", gates, []int{1, 1}, 0) // NOR(1,1) = 0
}

func testXOR() {
	gates := []Gate{
		makeGate("XOR", 0, 1, 2),
	}
	doTest("15", gates, []int{0, 0}, 0) // XOR(0,0) = 0
	doTest("16", gates, []int{0, 1}, 1) // XOR(0,1) = 1
	doTest("17", gates, []int{1, 0}, 1) // XOR(1,0) = 1
	doTest("18", gates, []int{1, 1}, 0) // XOR(1,1) = 0
}

func testMixed() {
	// This circuit implements: (a XOR b) NOR (b NAND c)
	gates := []Gate{
		makeGate("XOR", 0, 1, 5),  // XOR of inputs a and b -> wire 5
		makeGate("NAND", 1, 2, 6), // NAND of inputs b and c -> wire 6
		makeGate("NOR", 5, 6, 7),  // NOR of wires 5 and 6 -> wire 7 (output)
	}
	doTest("19", gates, []int{0, 0, 0}, 1) // (0⊕0) ↓ (0↑0) = 0 ↓ 1 = 1
	doTest("20", gates, []int{1, 1, 0}, 0) // (1⊕1) ↓ (1↑0) = 0 ↓ 1 = 0
	doTest("21", gates, []int{1, 0, 1}, 0) // (1⊕0) ↓ (0↑1) = 1 ↓ 1 = 0
}

// Modified main() function:
func main() {
	test123()
	test4()
	test567()
	test890()
	testNOR()
	testXOR()
	testMixed()
}
