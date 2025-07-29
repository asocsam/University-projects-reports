package ece448.iot_hub;

public class Plug {
  private final String name;
  private final String state;
  private final int power;

  public Plug(String name, String state, int power) {
    this.name = name;
    this.state = state;
    this.power = power;
  }

  public String getName() { return name; }
  public String getState() { return state; }
  public int getPower() { return power; }
}