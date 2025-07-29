import numpy as np
import matplotlib.pyplot as plt

# Load data from file
time, rtt = np.loadtxt('rtt.out', usecols=(0, 1), unpack=True, comments='#')

# Plotting
plt.plot(time, rtt)
plt.xlabel('Time (s)')
plt.ylabel('Round-Trip Time (ms)')
plt.title('Round-Trip Time over Time')
plt.grid(True)
plt.show()
