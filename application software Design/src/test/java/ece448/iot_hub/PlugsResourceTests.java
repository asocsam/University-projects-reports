package ece448.iot_hub;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PlugsResourceTests {

  private PlugsResource resource;
  private PlugsModelTests.StubMqttController mqtt;

  @Before
  public void setup() throws Exception {
    mqtt = new PlugsModelTests.StubMqttController();
    PlugsModel model = new PlugsModel(mqtt);
    resource = new PlugsResource(model);
  }

  @Test
  public void testGetPlugState() {
    mqtt.setState("plug1", "on");
    mqtt.setPower("plug1", "99.9");

    Plug result = resource.getPlugState("plug1");

    assertEquals("plug1", result.getName());
    assertEquals("on", result.getState());
    assertEquals(99, result.getPower());
  }

  @Test
  public void testGetAllPlugStates() {
    mqtt.setState("a", "on");
    mqtt.setPower("a", "88.5");
    mqtt.setState("b", "off");
    mqtt.setPower("b", "0.0");

    List<Plug> plugs = resource.getAllPlugStates();
    assertEquals(2, plugs.size());

    Plug a = plugs.stream().filter(p -> p.getName().equals("a")).findFirst().orElse(null);
    assertNotNull(a);
    assertEquals("on", a.getState());
    assertEquals(88, a.getPower());

    Plug b = plugs.stream().filter(p -> p.getName().equals("b")).findFirst().orElse(null);
    assertNotNull(b);
    assertEquals("off", b.getState());
    assertEquals(0, b.getPower());
  }

  @Test
  public void testControlPlug_on() {
    resource.controlPlug("plugX", "on");

    assertEquals("plugX", mqtt.lastPlug);
    assertEquals("on", mqtt.lastAction);
  }

  @Test
  public void testControlPlug_toggle() {
    resource.controlPlug("plugY", "toggle");

    assertEquals("plugY", mqtt.lastPlug);
    assertEquals("toggle", mqtt.lastAction);
  }
}