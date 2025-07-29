package ece448.iot_sim;

import java.io.File;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ece448.iot_sim.http_server.JHTTP;

public class Main implements AutoCloseable {
	private final MqttClient mqtt;
	public static void main(String[] args) throws Exception {
		// load configuration file
		String configFile = args.length > 0 ? args[0] : "simConfig.json";
		SimConfig config = mapper.readValue(new File(configFile), SimConfig.class);
		logger.info("{}: {}", configFile, mapper.writeValueAsString(config));

		try (Main m = new Main(config))
		{
			// loop forever
			for (;;)
			{
				Thread.sleep(60000);
			}
		}
	}

	public Main(SimConfig config) throws Exception {
		// create plugs
		ArrayList<PlugSim> plugs = new ArrayList<>();
		for (String plugName: config.getPlugNames()) {
			plugs.add(new PlugSim(plugName));
		}

		// start power measurements
		MeasurePower measurePower = new MeasurePower(plugs);
		measurePower.start();

		// start HTTP commands
		this.http = new JHTTP(config.getHttpPort(), new HTTPCommands(plugs));
		this.http.start();
		// Connect to MQTT broker
		this.mqtt = new MqttClient(
				config.getMqttBroker(),
				config.getMqttClientId(),
				new MemoryPersistence()
		);

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		mqtt.connect(options);

		// Setup MQTT updates
		MqttUpdates mqttUpd = new MqttUpdates(config.getMqttTopicPrefix());

		// Add observers to publish state and power updates
		for (PlugSim plug : plugs) {
			plug.addObserver((name, key, value) -> {
				try {
					mqtt.publish(mqttUpd.getTopic(name, key), mqttUpd.getMessage(value));
					logger.debug("Published MQTT: {} -> {}", mqttUpd.getTopic(name, key), value);
				} catch (Exception e) {
					logger.error("Failed to publish {} {} {}", name, key, value, e);
				}
			});

			// Publish initial state
			plug.publishState();
		}

		// Setup MQTT commands
		MqttCommands mqttCmd = new MqttCommands(plugs, config.getMqttTopicPrefix());
		logger.info("MQTT subscribe to {}", mqttCmd.getTopic());
		mqtt.subscribe(mqttCmd.getTopic(), (topic, msg) -> mqttCmd.handleMessage(topic, msg));

		logger.info("MQTT connected to broker: {}", config.getMqttBroker());
	}

	@Override
	public void close() throws Exception {
		http.close();
	}

	private final JHTTP http;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}
