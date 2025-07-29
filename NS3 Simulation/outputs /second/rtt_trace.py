import matplotlib.pyplot as plt

enqueue_times = []
dequeue_times = []

with open('1q2.tr', 'r') as file:
    for line in file:
        parts = line.split(' ')
        event_type = parts[0]
        time = float(parts[1])
        if event_type == '+':
            enqueue_times.append(time)
        elif event_type == '-':
            dequeue_times.append(time)

# Plot enqueue and dequeue events
plt.plot(enqueue_times, [1]*len(enqueue_times), 'bo', label='Enqueue')
plt.plot(dequeue_times, [1]*len(dequeue_times), 'ro', label='Dequeue')
plt.xlabel('Time')
plt.ylabel('Event')
plt.title('Enqueue and Dequeue Events Over Time')
plt.legend()
plt.show()
