package ece448.iot_sim;

import static org.junit.Assert.*;
import org.junit.Test;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttUpdatesTest {

  @Test
  public void testGetTopic() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    assertEquals("testTopic/update/device1/state", updates.getTopic("device1", "state"));
    assertEquals("testTopic/update/device2/power", updates.getTopic("device2", "power"));
    assertEquals("testTopic/update/device3/status", updates.getTopic("device3", "status"));
  }

  @Test
  public void testGetTopicWithSpecialCharacters() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    assertEquals("testTopic/update/dev.ice/state", updates.getTopic("dev.ice", "state"));
    assertEquals("testTopic/update/sensor_123/power", updates.getTopic("sensor_123", "power"));
    assertEquals("testTopic/update/plug#1/status", updates.getTopic("plug#1", "status"));
  }

  @Test
  public void testGetTopicWithEmptyStrings() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    assertEquals("testTopic/update//state", updates.getTopic("", "state"));
    assertEquals("testTopic/update/device2/", updates.getTopic("device2", ""));
    assertEquals("testTopic/update//", updates.getTopic("", ""));
  }

  @Test
  public void testGetMessage() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    MqttMessage message = updates.getMessage("ON");
    assertNotNull(message);
    assertEquals("ON", new String(message.getPayload()));
    assertTrue(message.isRetained());

    message = updates.getMessage("OFF");
    assertNotNull(message);
    assertEquals("OFF", new String(message.getPayload()));
    assertTrue(message.isRetained());
  }

  @Test
  public void testGetMessageWithSpecialCharacters() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    MqttMessage message = updates.getMessage("!@#$%^&*()");
    assertNotNull(message);
    assertEquals("!@#$%^&*()", new String(message.getPayload()));
    assertTrue(message.isRetained());
  }

  @Test
  public void testGetMessageWithEmptyString() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    MqttMessage message = updates.getMessage("");
    assertNotNull(message);
    assertEquals("", new String(message.getPayload()));
    assertTrue(message.isRetained());
  }

  @Test
  public void testGetMessageWithLargePayload() {
    MqttUpdates updates = new MqttUpdates("testTopic");

    String longPayload = "A".repeat(1024);  // 1 KB payload
    MqttMessage message = updates.getMessage(longPayload);

    assertNotNull(message);
    assertEquals(longPayload, new String(message.getPayload()));
    assertTrue(message.isRetained());
  }
}
