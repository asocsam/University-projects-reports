package ece448.iot_hub;

import ece448.grading.GradeP3.MqttController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class App {

	@Bean
	public MqttController mqttController(Environment env) throws Exception {
		String broker = env.getProperty("mqtt.broker");
		String clientId = env.getProperty("mqtt.clientId");
		String topicPrefix = env.getProperty("mqtt.topicPrefix");
		MqttController controller = new MqttController(broker, clientId, topicPrefix);
		controller.start();
		return controller;
	}

	@Bean
	public PlugsModel plugsModel(MqttController mqttController) {
		return new PlugsModel(mqttController);
	}

	@Bean
	public GroupsModel groupsModel(Environment env, MqttController mqttController) {
		return new GroupsModel(env, mqttController);
	}

	public static void main(String[] args) {
		// Start the Spring Boot application and obtain the context.
		ConfigurableApplicationContext context = SpringApplication.run(App.class, args);

		// Register a shutdown hook that not only closes the context but also forces JVM termination.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutdown hook: Initiating graceful shutdown...");
			try {
				MqttController mqttController = context.getBean(MqttController.class);
				if (mqttController != null) {
					// If your MqttController has a disconnect() or stop() method, call it here.
					// For example: mqttController.disconnect();
					// (Currently, no such method exists, so this line is omitted.)
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// Close the Spring context to shut down the embedded server and associated threads.
				context.close();
				System.out.println("Spring context closed. Exiting gracefully.");
				// Force the JVM to exit.
				System.exit(0);
			}
		}));

		// (The application continues running while tests are executed.)
	}
}
