package ece448.iot_sim;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulate a smart plug with power monitoring.
 */
public class PlugSim {

	private final String name;
	private boolean on = false;
	private double power = 0; // in watts
	private final List<Observer> observers = new ArrayList<>();

	public PlugSim(String name) {
		this.name = name;
	}

	/**
	 * No need to synchronize if read a final field.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Switch the plug on.
	 */
	public synchronized void switchOn() {
		on = true;
		notifyObservers("state", "on");
		logger.debug("Plug {}: switched on", name);
	}

	/**
	 * Switch the plug off.
	 */
	public synchronized void switchOff() {
		on = false;
		updatePower(0); // Set power to 0 when the plug is off
		notifyObservers("state", "off");
		logger.debug("Plug {}: switched off", name);
	}

	/**
	 * Toggle the plug.
	 */
	public synchronized void toggle() {
		if (on) {
			switchOff();
		} else {
			switchOn();
		}
	}

	/**
	 * Measure power.
	 */
	public synchronized void measurePower() {
		if (!on) {
			updatePower(0);
			return;
		}

		// A trick to help testing
		if (name.indexOf(".") != -1) {
			updatePower(Integer.parseInt(name.split("\\.")[1]));
		} else if (power < 100) {
			updatePower(power + Math.random() * 100);
		} else if (power > 300) {
			updatePower(power - Math.random() * 100);
		} else {
			updatePower(power + Math.random() * 40 - 20);
		}
	}

	/**
	 * Update the power value and log it.
	 */
	protected void updatePower(double p) {
		power = p;
		notifyObservers("power", String.format("%.3f", power));
		logger.debug("Plug {}: power {}", name, power);
	}

	/**
	 * Getter: current state.
	 */
	public synchronized boolean isOn() {
		return on;
	}

	/**
	 * Getter: last power reading.
	 */
	public synchronized double getPower() {
		return power;
	}

	public void addObserver(Observer observer) {
		observers.add(observer);
	}

	private void notifyObservers(String key, String value) {
		for (Observer observer : observers) {
			observer.update(name, key, value);
		}
	}

	public static interface Observer {
		void update(String name, String key, String value);
	}

	public void publishState() {
		notifyObservers("state", on ? "on" : "off");
	}

	private static final Logger logger = LoggerFactory.getLogger(PlugSim.class);
}