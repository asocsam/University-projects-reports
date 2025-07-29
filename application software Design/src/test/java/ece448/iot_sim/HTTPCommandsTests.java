package ece448.iot_sim;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.*;

public class HTTPCommandsTests {

    private HTTPCommands http;
    private PlugSim plugX, plugY, plugZ;

    @Before
    public void setup() {
        plugX = new PlugSim("xxxx");
        plugY = new PlugSim("yyyy");
        plugZ = new PlugSim("zzzz.789"); // Power set to 789 when ON
        http = new HTTPCommands(Arrays.asList(plugX, plugY, plugZ));
    }

    @Test
    public void testInitialPlugStatus() {
        String response = http.handleGet("/xxxx", new HashMap<>());
        assertTrue(response.contains("xxxx is off"));
        assertTrue(response.contains("Power reading is 0.000"));
    }

    @Test
    public void testSwitchOn() {
        http.handleGet("/xxxx", Map.of("action", "on"));
        assertTrue(plugX.isOn());
    }

    @Test
    public void testSwitchOff() {
        plugX.switchOn(); // First turn it ON
        http.handleGet("/xxxx", Map.of("action", "off"));
        assertFalse(plugX.isOn());
    }

    @Test
    public void testRemainOnAfterSwitching() {
        http.handleGet("/xxxx", Map.of("action", "on"));
        String response = http.handleGet("/xxxx", new HashMap<>());
        assertTrue(response.contains("xxxx is on"));
    }

    @Test
    public void testToggleOnToOff() {
        plugX.switchOn();
        http.handleGet("/xxxx", Map.of("action", "toggle"));
        assertFalse(plugX.isOn());
    }

    @Test
    public void testToggleOffToOn() {
        http.handleGet("/xxxx", Map.of("action", "toggle"));
        assertTrue(plugX.isOn());
    }

    @Test
    public void testPlugYInitialState() {
        String response = http.handleGet("/yyyy", new HashMap<>());
        assertTrue(response.contains("yyyy is off"));
    }

    @Test
    public void testVerifyPlugRemainsOn() {
        http.handleGet("/xxxx", Map.of("action", "on"));
        assertTrue(plugX.isOn());
    }

    @Test
    public void testPlugZPowerReadingOff() {
        String response = http.handleGet("/zzzz.789", new HashMap<>());
        assertTrue(response.contains("Power reading is 0.000"));
    }

    @Test
    public void testListPlugs() {
        String response = http.handleGet("/", new HashMap<>());
        assertTrue(response.contains("xxxx"));
        assertTrue(response.contains("yyyy"));
        assertTrue(response.contains("zzzz.789"));
    }

    @Test
    public void testInvalidPlugName() {
        String response = http.handleGet("/unknownPlug", new HashMap<>());
        assertTrue(response.contains("Error: No such plug found!"));
    }

    @Test
    public void testInvalidAction() {
        String response = http.handleGet("/xxxx", Map.of("action", "invalidAction"));
        assertTrue(response.contains("Error: Invalid action!"));
    }

    @Test
    public void testMultipleToggles() {
        http.handleGet("/xxxx", Map.of("action", "toggle"));
        assertTrue(plugX.isOn());

        http.handleGet("/xxxx", Map.of("action", "toggle"));
        assertFalse(plugX.isOn());
    }

    @Test
    public void testEnsureSwitchOffAfterOn() {
        http.handleGet("/xxxx", Map.of("action", "on"));
        http.handleGet("/xxxx", Map.of("action", "off"));
        assertFalse(plugX.isOn());
    }
}
