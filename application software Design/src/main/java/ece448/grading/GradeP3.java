package ece448.grading;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import java.util.stream.Collectors;
import org.apache.http.client.fluent.Request;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ece448.iot_sim.SimConfig;
import ece448.iot_sim.Main;

public class GradeP3 implements AutoCloseable {

	public static class MqttController {
		private final String broker;
		private final String clientId;
		private final String topicPrefix;
		
		private final MqttClient client;
	
		private final HashMap<String, String> states = new HashMap<>();
		private final HashMap<String, String> powers = new HashMap<>();
		private final Map<String, Deque<TimeStampedReading>> powerHistory = new TreeMap<>();
		private final long maxHistoryMillis = 15 * 60_000; // keep 15 min of data
		public static class TimeStampedReading {
			public final long timestamp;
			public final double power;
			public TimeStampedReading(long ts, double p) {
				this.timestamp = ts;
				this.power = p;
			}
		}

		public MqttController(String broker, String clientId,
			String topicPrefix) throws Exception {
			this.broker = broker;
			this.clientId = clientId;
			this.topicPrefix = topicPrefix;
			this.client = new MqttClient(broker, clientId, new MemoryPersistence());
		}

		synchronized public List<TimeStampedReading> getPowerHistory(String name, long minutes) {
			Deque<TimeStampedReading> q = powerHistory.getOrDefault(name, new ArrayDeque<>());
			long cutoff = System.currentTimeMillis() - minutes*60_000;
			return q.stream()
					.filter(r -> r.timestamp >= cutoff)
					.collect(Collectors.toList());
		}

		public void start() throws Exception {
			MqttConnectOptions opt = new MqttConnectOptions();
			opt.setCleanSession(true);
			client.connect(opt);
			
			client.subscribe(topicPrefix+"/update/#", this::handleUpdate);
	
			logger.info("MqttCtl {}: {} connected", clientId, broker);
		}
	
		public void close() throws Exception {
			client.disconnect();
			logger.info("MqttCtl {}: disconnected", clientId);
		}
	
		synchronized public void publishAction(String plugName, String action) {
			String topic = topicPrefix+"/action/"+plugName+"/"+action;
			try
			{
				client.publish(topic, new MqttMessage());
			}
			catch (Exception e)
			{
				logger.error("MqttCtl {}: {} fail to publish", clientId, topic);
			}
		}
	
		synchronized public String getState(String plugName) {
			return states.get(plugName);
		}
	
		synchronized public String getPower(String plugName) {
			return powers.get(plugName);
		}
	
		synchronized public Map<String, String> getStates() {
			return new TreeMap<>(states);
		}
	
		synchronized public Map<String, String> getPowers() {
			return new TreeMap<>(powers);
		}

		synchronized protected void handleUpdate(String topic, MqttMessage msg) {
			// strip off prefix + “/”
			String[] parts = topic.substring(topicPrefix.length()+1).split("/");
			if (parts.length != 3 || !parts[0].equals("update")) return;
			String plug = parts[1], type = parts[2], payload = msg.toString();

			switch (type) {
				case "state":
					states.put(plug, payload);
					break;

				case "power":
					powers.put(plug, payload);
					try {
						double p = Double.parseDouble(payload);
						long now = System.currentTimeMillis();
						Deque<TimeStampedReading> q = powerHistory
								.computeIfAbsent(plug, k -> new ArrayDeque<>());
						q.addLast(new TimeStampedReading(now, p));
						while (!q.isEmpty() && q.peekFirst().timestamp < now - maxHistoryMillis) {
							q.removeFirst();
						}
					} catch (NumberFormatException ignored) {}
					break;

				default:
					// ignore
			}
		}
	
		private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
	}
	
	private static final String broker = "tcp://127.0.0.1";
	private static final String topicPrefix = System.currentTimeMillis()+"/grade_p3/iot_ece448";

	private final MqttController mqtt;

	private GradeP3() throws Exception {
		this.mqtt = new MqttController(broker, "grader/iot_sim", topicPrefix);
		this.mqtt.start();
	}

	@Override
	public void close() throws Exception {
		mqtt.close();
	}

	public static void main(String[] args) throws Exception {
		SimConfig config = new SimConfig(
			8080, Arrays.asList("xx", "yy", "zz.666"),
			broker, "testee/iot_sim", topicPrefix);

		try (GradeP3 p3 = new GradeP3(); Main m = new Main(config))
		{
			Grading.run(p3, 10);
		}
	}

	private String get(String pathParams) throws Exception {
		return Request.Get("http://127.0.0.1:8080"+pathParams)
			.userAgent("Mozilla/5.0")
			.connectTimeout(1000)
			.socketTimeout(1000)
			.execute().returnContent().asString();
	}

	public boolean testCase00() throws Exception {
		return "off".equals(mqtt.getState("xx"));
	}

	public boolean testCase01() throws Exception {
		mqtt.publishAction("xx", "on");
		Thread.sleep(1000);
		return "on".equals(mqtt.getState("xx"));
	}

	public boolean testCase02() throws Exception {
		mqtt.publishAction("xx", "off");
		Thread.sleep(1000);
		return "off".equals(mqtt.getState("xx"));
	}

	public boolean testCase03() throws Exception {
		mqtt.publishAction("xx", "toggle");
		Thread.sleep(1000);
		return "on".equals(mqtt.getState("xx"));
	}

	public boolean testCase04() throws Exception {
		Thread.sleep(1500);
		if (!"0.000".equals(mqtt.getPower("zz.666")))
			return false;
		mqtt.publishAction("zz.666", "on");	
		Thread.sleep(1500);
		return "666.000".equals(mqtt.getPower("zz.666"));
	}

	public boolean testCase05() throws Exception {
		return (mqtt.getPower("yyyy") == null)
			&& (mqtt.getState("yyyy") == null)
			&& "on".equals(mqtt.getState("zz.666"));
	}

	public boolean testCase06() throws Exception {
		get("/yy?action=on");
		Thread.sleep(1000);
		return "on".equals(mqtt.getState("yy"));				
	}

	public boolean testCase07() throws Exception {
		get("/yy?action=off");
		Thread.sleep(1000);
		return "off".equals(mqtt.getState("yy"));				
	}

	public boolean testCase08() throws Exception {
		mqtt.publishAction("zz.666", "toggle");
		String ret = get("/zz.666");
		Thread.sleep(1000);
		return (ret.indexOf("zz.666 is off") != -1)
			&& (ret.indexOf("zz.666 is on") == -1)
			&& "off".equals(mqtt.getState("zz.666"));
	}

	public boolean testCase09() throws Exception {
		mqtt.publishAction("zz.666", "toggle");
		String ret = get("/zz.666");
		Thread.sleep(1000);
		return (ret.indexOf("zz.666 is on") != -1)
			&& (ret.indexOf("zz.666 is off") == -1)
			&& "on".equals(mqtt.getState("zz.666"));
	}
}
