package ece448.iot_hub;

import org.junit.*;

import static org.junit.Assert.*;

public class PlugTests {

  @Test
  public void testPlugGetters() {
    Plug plug = new Plug("testPlug", "off", 0);

    assertEquals("testPlug", plug.getName());
    assertEquals("off", plug.getState());
    assertEquals(0, plug.getPower());
  }
}