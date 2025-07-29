package ece448.iot_hub;

import ece448.grading.GradeP3;
import ece448.grading.GradeP3.MqttController;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

public class PlugsModelTests {

  private StubMqttController mqtt;
  private PlugsModel model;

  @Before
  public void setup() throws Exception {
    mqtt = new StubMqttController();
    model = new PlugsModel(mqtt);
  }

  @Test
  public void testGetPlug_withValidData() {
    mqtt.setState("plug1", "on");
    mqtt.setPower("plug1", "150.0");

    Plug plug = model.getPlug("plug1");

    assertEquals("plug1", plug.getName());
    assertEquals("on", plug.getState());
    assertEquals(150, plug.getPower());
  }

  @Test
  public void testGetPlug_withMissingPlug() {
    Plug plug = model.getPlug("unknown");

    assertEquals("unknown", plug.getName());
    assertEquals("unknown", plug.getState());
    assertEquals(0, plug.getPower());
  }

  @Test
  public void testGetAllPlugs_returnsCorrectList() {
    mqtt.setState("a", "on");
    mqtt.setPower("a", "88.5");

    mqtt.setState("b", "off");
    mqtt.setPower("b", "0.0");

    List<Plug> plugs = model.getAllPlugs();

    assertEquals(2, plugs.size());

    Plug a = getByName(plugs, "a");
    assertNotNull(a);
    assertEquals("on", a.getState());
    assertEquals(88, a.getPower());

    Plug b = getByName(plugs, "b");
    assertNotNull(b);
    assertEquals("off", b.getState());
    assertEquals(0, b.getPower());
  }

  @Test
  public void testPerformAction_tracksLastCalled() {
    model.performAction("plug2", "toggle");

    assertEquals("plug2", mqtt.lastPlug);
    assertEquals("toggle", mqtt.lastAction);
  }

  private Plug getByName(List<Plug> plugs, String name) {
    for (Plug plug : plugs) {
      if (plug.getName().equals(name)) {
        return plug;
      }
    }
    return null;
  }

  /**
   * A stub implementation of MqttController for unit testing without Mockito.
   */
  static class StubMqttController extends GradeP3.MqttController {

    private final Map<String, String> states = new HashMap<>();
    private final Map<String, String> powers = new HashMap<>();
    public String lastPlug;
    public String lastAction;

    public StubMqttController() throws Exception {
      super("tcp://localhost:1883", "dummy-client-id", "dummy/topic");
    }

    public void setState(String name, String state) {
      states.put(name, state);
    }

    public void setPower(String name, String power) {
      powers.put(name, power);
    }

    @Override
    public String getState(String name) {
      return states.get(name);
    }

    @Override
    public String getPower(String name) {
      return powers.get(name);
    }

    @Override
    public Map<String, String> getStates() {
      return new TreeMap<>(states);
    }

    @Override
    public Map<String, String> getPowers() {
      return new TreeMap<>(powers);
    }

    @Override
    public void publishAction(String plugName, String action) {
      this.lastPlug = plugName;
      this.lastAction = action;
    }
  }

  @Test
  public void testPowerRounding() {
    mqtt.setState("plug3", "on");
    mqtt.setPower("plug3", "99.9");

    Plug plug = model.getPlug("plug3");

    assertEquals(99, plug.getPower());
  }

  @Test
  public void testInvalidPowerFormat() {
    mqtt.setState("plugX", "on");
    mqtt.setPower("plugX", "abc");  // Invalid float

    Plug plug = model.getPlug("plugX");

    assertEquals("plugX", plug.getName());
    assertEquals("on", plug.getState());
    assertEquals(0, plug.getPower()); // Fallback to 0
  }

  @Test
  public void testPlugWithNullStateAndPower() {
    mqtt.setState("plugY", null);
    mqtt.setPower("plugY", null);

    Plug plug = model.getPlug("plugY");

    assertEquals("plugY", plug.getName());
    assertEquals("unknown", plug.getState());
    assertEquals(0, plug.getPower());
  }

  @Test
  public void testCaseSensitivePlugNames() {
    mqtt.setState("PlugA", "on");
    mqtt.setPower("PlugA", "100");

    Plug lower = model.getPlug("pluga");
    assertEquals("pluga", lower.getName());
    assertEquals("unknown", lower.getState());
    assertEquals(0, lower.getPower());
  }
}