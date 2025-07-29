import numpy as np
import matplotlib.pyplot as plt

# Load data from file
time, cwnd = np.loadtxt('cwnd.out', usecols=(0, 1), unpack=True)

# Plotting
plt.plot(time, cwnd)
plt.xlabel('Time (s)')
plt.ylabel('Congestion Window Size')
plt.title('Congestion Window over Time')
plt.grid(True)
plt.show()
