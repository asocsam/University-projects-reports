package ece448.iot_hub;

import ece448.grading.GradeP3.MqttController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlugsModel {

  private final MqttController mqtt;

  public PlugsModel(MqttController mqtt) {
    this.mqtt = mqtt;
  }

  public Plug getPlug(String name) {
    String state = mqtt.getState(name);
    String powerStr = mqtt.getPower(name);
    int power = 0;
    try {
      power = (powerStr != null) ? (int) Double.parseDouble(powerStr) : 0;
    } catch (Exception ignored) {}

    return new Plug(name, state == null ? "unknown" : state, power);
  }

  public List<Plug> getAllPlugs() {
    Map<String, String> states = mqtt.getStates();
    List<Plug> all = new ArrayList<>();

    for (String name : states.keySet()) {
      all.add(getPlug(name));
    }
    return all;
  }

  public void performAction(String name, String action) {
    mqtt.publishAction(name, action);
  }

  public List<MqttController.TimeStampedReading> getHistory(String name, long minutes) {
    return mqtt.getPowerHistory(name, minutes);
  }
}