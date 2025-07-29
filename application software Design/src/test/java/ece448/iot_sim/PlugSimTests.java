package ece448.iot_sim;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlugSimTests {

	@Test
	public void testInit() {
		PlugSim plug = new PlugSim("a");

		assertFalse(plug.isOn());
	}

	@Test
	public void testSwitchOn() {
		PlugSim plug = new PlugSim("a");

		plug.switchOn();

		assertTrue(plug.isOn());
	}
	@Test
	public void testGetNameSuccess() {
		PlugSim plug = new PlugSim("a");

		assertFalse(plug.isOn());
		assertEquals(0.0, plug.getPower(), 0.0001);
		assertEquals("a", plug.getName());
	}

	@Test
	public void testGetNameNull() {
		PlugSim plug = new PlugSim(null);

		assertFalse(plug.isOn());
		assertEquals(0.0, plug.getPower(), 0.0001);
		assertNull(plug.getName());
	}

	@Test
	public void testSwitchOff() {
		PlugSim plug = new PlugSim("a");

		plug.switchOff();

		assertFalse(plug.isOn());
		assertEquals(0.0, plug.getPower(), 0.0001);
    assertEquals("a", plug.getName());
	}

	@Test
	public void testToggleOn() {
		PlugSim plug = new PlugSim("a");
		plug.toggle();
		assertTrue(plug.isOn());
	}

	@Test
	public void testToggleOff() {
		PlugSim plug = new PlugSim("a");
		plug.switchOn();
		plug.toggle();
		assertFalse(plug.isOn());
	}

	@Test
	public void testMeasurePowerOnPowerZero() {
		PlugSim plug = new PlugSim("a");
		plug.measurePower();
		assertEquals(0.0, plug.getPower(), 0.0001);
		assertEquals("a", plug.getName());
	}

	@Test
	public void testMeasurePowerOffNameDotNumber() {
		PlugSim plug = new PlugSim("a.10");
		plug.switchOn();
		plug.measurePower();
		assertEquals(10.0, plug.getPower(), 0.0001);
		assertEquals("a.10", plug.getName());
	}

	@Test
	public void testMeasurePowerOffNameDotNumberParseError() {
		PlugSim plug = new PlugSim("a.xyz");
		plug.switchOn();
		assertThrows(NumberFormatException.class, plug::measurePower);
	}

	@Test
	public void testMeasurePowerOffPowerGreateThan100Increase() {
		PlugSim plug = new PlugSim("a");
		plug.switchOn();
		while (plug.getPower() >= 100) {
			plug.measurePower();
		}
		double pervPower = plug.getPower();
		plug.measurePower();
		double currPower = plug.getPower();
		assertTrue(currPower >= pervPower && currPower <= pervPower + 100);
	}

	@Test
	public void testMeasurePowerOffPowerGreaterThan300Decrease() {
		PlugSim plug = new PlugSim("a");
		plug.switchOn();
		while (plug.getPower() <= 300) {
			plug.measurePower();
		}
		double pervPower = plug.getPower();
		plug.measurePower();
		double currPower = plug.getPower();
		assertTrue(currPower <= pervPower && currPower >= pervPower - 100);
	}

	@Test
	public void testMeasurePowerOffPowerLessThan100OrGreaterThan300() {
		PlugSim plug = new PlugSim("a");
		plug.switchOn();
		while (plug.getPower() < 100 || plug.getPower() > 300) {
			plug.measurePower();
		}
		double pervPower = plug.getPower();
		plug.measurePower();
		double currPower = plug.getPower();
		assertTrue(currPower >= pervPower - 20 && currPower <= pervPower + 20);
	}

	class MockObserver implements PlugSim.Observer {
		String lastName;
		String lastKey;
		String lastValue;

		@Override
		public void update(String name, String key, String value) {
			this.lastName = name;
			this.lastKey = key;
			this.lastValue = value;
		}
	}

	@Test
	public void testAddObserver() {
		PlugSim plug = new PlugSim("testPlug");
		MockObserver observer = new MockObserver();

		plug.addObserver(observer);
		plug.switchOn();

		assertEquals("state", observer.lastKey);
		assertEquals("on", observer.lastValue);
	}

	@Test
	public void testNotifyMultipleObservers() {
		PlugSim plug = new PlugSim("testPlug");
		MockObserver observer1 = new MockObserver();
		MockObserver observer2 = new MockObserver();

		plug.addObserver(observer1);
		plug.addObserver(observer2);

		plug.switchOn();

		assertEquals("on", observer1.lastValue);
		assertEquals("on", observer2.lastValue);
	}

	@Test
	public void testUpdatePower() {
		PlugSim plug = new PlugSim("testPlug");
		MockObserver observer = new MockObserver();

		plug.addObserver(observer);
		plug.switchOn();
		plug.updatePower(250.5);

		assertEquals(250.5, plug.getPower(), 0.001);
		assertEquals("power", observer.lastKey);
		assertEquals("250.500", observer.lastValue);
	}

	@Test
	public void testPublishState() {
		PlugSim plug = new PlugSim("testPlug");
		MockObserver observer = new MockObserver();

		plug.addObserver(observer);
		plug.switchOn();
		plug.publishState();

		assertEquals("state", observer.lastKey);
		assertEquals("on", observer.lastValue);
	}

	@Test
	public void testNoObservers() {
		PlugSim plug = new PlugSim("testPlug");
		plug.switchOn(); // No observers, should not throw an exception
		assertTrue(plug.isOn());
	}

	@Test
	public void testExtremePowerValues() {
		PlugSim plug = new PlugSim("testPlug");
		plug.switchOn();

		plug.updatePower(Double.MAX_VALUE);
		assertEquals(Double.MAX_VALUE, plug.getPower(), 0.001);

		plug.updatePower(Double.MIN_VALUE);
		assertEquals(Double.MIN_VALUE, plug.getPower(), 0.001);
	}

	@Test
	public void testPublishState_OnAndOff() {
		PlugSim plug = new PlugSim("testPlug");
		MockObserver observer = new MockObserver();
		plug.addObserver(observer);

		// When plug is turned ON
		plug.switchOn();
		plug.publishState();
		assertEquals("state", observer.lastKey);
		assertEquals("on", observer.lastValue);

		// When plug is turned OFF
		plug.switchOff();
		plug.publishState();
		assertEquals("state", observer.lastKey);
		assertEquals("off", observer.lastValue);
	}

}
