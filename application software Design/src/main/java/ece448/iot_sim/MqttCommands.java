package ece448.iot_sim;

import java.util.List;
import java.util.TreeMap;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttCommands {
  private final TreeMap<String, PlugSim> plugs = new TreeMap<>();
  private final String topicPrefix;

  public MqttCommands(List<PlugSim> plugs, String topicPrefix) {
    for (PlugSim plug : plugs) {
      this.plugs.put(plug.getName(), plug);
    }
    this.topicPrefix = topicPrefix;
  }

  public String getTopic() {
    return topicPrefix + "/action/#";
  }

  public void handleMessage(String topic, MqttMessage msg) {
    try {
      logger.info("MqttCmd {}: {}", topic, new String(msg.getPayload()));

      // Parse the topic to extract plug name and action
      String actionPath = topic.substring(topicPrefix.length() + 1);
      String[] parts = actionPath.split("/");

      if (parts.length != 3 || !parts[0].equals("action")) {
        logger.warn("Invalid topic format: {}", topic);
        return;
      }

      String plugName = parts[1];
      String action = parts[2];

      PlugSim plug = plugs.get(plugName);
      if (plug == null) {
        logger.warn("Unknown plug: {}", plugName);
        return;
      }

      switch (action) {
        case "on":
          plug.switchOn();
          break;
        case "off":
          plug.switchOff();
          break;
        case "toggle":
          plug.toggle();
          break;
        default:
          logger.warn("Unknown action: {}", action);
      }

    } catch (Throwable th) {
      logger.error("MqttCmd {}: {}", topic, th.getMessage(), th);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(MqttCommands.class);
}
