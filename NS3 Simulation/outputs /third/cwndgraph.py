import numpy as np
import matplotlib.pyplot as plt

# Load only the first two columns (time and congestion window size) from cwnd.out file
time, cwnd = np.loadtxt('cwnd.out', usecols=(0, 1), unpack=True)

# Plot congestion window size over time
plt.plot(time, cwnd, label='Congestion Window')
plt.title('Congestion Window Size over Time')
plt.xlabel('Time (s)')
plt.ylabel('Congestion Window Size')
plt.legend()
plt.grid(True)
plt.savefig('cwnd_plot.png')  # Save plot as PNG file
plt.show()
