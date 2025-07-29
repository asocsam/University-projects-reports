package ece448.iot_sim;

import static org.junit.Assert.*;
import org.junit.Test;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Arrays;
import java.util.List;

public class MqttCommandsTest {

  @Test
  public void testGetTopic() {
    MqttCommands commands = new MqttCommands(Arrays.asList(new PlugSim("plug1")), "testTopic");
    assertEquals("testTopic/action/#", commands.getTopic());
  }

  @Test
  public void testHandleMessage_InvalidTopic() {
    MqttCommands commands = new MqttCommands(Arrays.asList(new PlugSim("plug1")), "testTopic");

    // Invalid topic formats (should not throw exceptions)
    String[] invalidTopics = {
        "testTopic/wrongformat",
        "testTopic/action",
        "testTopic/action/plug1",
        "testTopic/action/plug1/on/extra"
    };

    for (String topic : invalidTopics) {
      commands.handleMessage(topic, new MqttMessage("on".getBytes()));
    }
  }

  @Test
  public void testHandleMessage_UnknownPlug() {
    MqttCommands commands = new MqttCommands(Arrays.asList(new PlugSim("plug1")), "testTopic");

    // Send commands to a non-existent plug
    String[] unknownPlugTopics = {
        "testTopic/action/plugX/on",
        "testTopic/action/plugY/off"
    };

    for (String topic : unknownPlugTopics) {
      commands.handleMessage(topic, new MqttMessage());
    }
  }

  @Test
  public void testHandleMessage_ValidActions() {
    PlugSim plug = new PlugSim("plug1");
    MqttCommands commands = new MqttCommands(Arrays.asList(plug), "testTopic");

    // Switch ON
    commands.handleMessage("testTopic/action/plug1/on", new MqttMessage());
    assertTrue(plug.isOn());

    // Switch OFF
    commands.handleMessage("testTopic/action/plug1/off", new MqttMessage());
    assertFalse(plug.isOn());

    // Toggle ON -> OFF -> ON
    commands.handleMessage("testTopic/action/plug1/toggle", new MqttMessage());
    assertTrue(plug.isOn());
    commands.handleMessage("testTopic/action/plug1/toggle", new MqttMessage());
    assertFalse(plug.isOn());
  }

  @Test
  public void testHandleMessage_UnknownAction() {
    PlugSim plug = new PlugSim("plug1");
    MqttCommands commands = new MqttCommands(Arrays.asList(plug), "testTopic");

    // Unknown actions should not change state
    commands.handleMessage("testTopic/action/plug1/invalid", new MqttMessage());
    assertFalse(plug.isOn());

    commands.handleMessage("testTopic/action/plug1/on", new MqttMessage());
    assertTrue(plug.isOn());

    commands.handleMessage("testTopic/action/plug1/unknownAction", new MqttMessage());
    assertTrue(plug.isOn());
  }

  @Test
  public void testHandleMessage_ExceptionHandling() {
    PlugSim faultyPlug = new PlugSim("faultyPlug") {
      @Override
      public void switchOn() {
        throw new RuntimeException("Simulated Exception");
      }
    };
    MqttCommands commands = new MqttCommands(Arrays.asList(faultyPlug), "testTopic");

    // Should trigger the catch block
    commands.handleMessage("testTopic/action/faultyPlug/on", new MqttMessage());

    // Edge cases: Null topic & short topic (should not crash)
    commands.handleMessage(null, new MqttMessage("on".getBytes()));
    commands.handleMessage("testT", new MqttMessage("on".getBytes()));

    // Null message (should not crash)
    commands.handleMessage("testTopic/action/faultyPlug/on", null);
  }

  @Test
  public void testHandleMessage_InvalidTopic_ShortLength() {
    MqttCommands commands = new MqttCommands(Arrays.asList(new PlugSim("plug1")), "testTopic");
    String[] invalidTopics = {
        "testTopic/",
        "testTopic/action",
        "testTopic/action/plug1"
    };

    for (String topic : invalidTopics) {
      commands.handleMessage(topic, new MqttMessage("on".getBytes()));
    }
  }

  @Test
  public void testHandleMessage_InvalidTopic_WrongPrefix() {
    MqttCommands commands = new MqttCommands(Arrays.asList(new PlugSim("plug1")), "testTopic");

    commands.handleMessage("testTopic/wrongPrefix/plug1/on", new MqttMessage("on".getBytes()));
  }

}
