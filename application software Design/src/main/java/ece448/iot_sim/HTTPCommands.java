package ece448.iot_sim;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ece448.iot_sim.http_server.RequestHandler;

public class HTTPCommands implements RequestHandler {

	// Use a map so we can search plugs by name.
	private final TreeMap<String, PlugSim> plugs = new TreeMap<>();

	public HTTPCommands(List<PlugSim> plugs) {
		for (PlugSim plug: plugs)
		{
			this.plugs.put(plug.getName(), plug);
		}
	}

	@Override
	public String handleGet(String path, Map<String, String> params) {
		// list all: /
		// do switch: /plugName?action=on|off|toggle
		// just report: /plugName

		logger.info("HTTPCmd {}: {}", path, params);

		if (path.equals("/"))
		{
			return listPlugs();
		}

		PlugSim plug = plugs.get(path.substring(1));
		if (plug == null) {
			return "<html><body><p>Error: No such plug found!</p></body></html>"; // ✅ Fixes NPE
		}

		String action = params.get("action");
		if (action == null) {
			return report(plug);
		}
		// Handle the plug action
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
				return "<html><body><p>Error: Invalid action!</p></body></html>";
		}

		// Return updated plug status
		return report(plug);
	}

	protected String listPlugs() {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><body>");
		for (String plugName: plugs.keySet())
		{
			sb.append(String.format("<p><a href='/%s'>%s</a></p>",
				plugName, plugName));
		}
		sb.append("</body></html>");

		return sb.toString();
	}

	protected String report(PlugSim plug) {
		String name = plug.getName();
		return String.format("<html><body>"
			+"<p>Plug %s is %s.</p>"
			+"<p>Power reading is %.3f.</p>"
			+"<p><a href='/%s?action=on'>Switch On</a></p>"
			+"<p><a href='/%s?action=off'>Switch Off</a></p>"
			+"<p><a href='/%s?action=toggle'>Toggle</a></p>"
			+"</body></html>",
			name,
			plug.isOn()? "on": "off",
			plug.getPower(), name, name, name);
	}

	private static final Logger logger = LoggerFactory.getLogger(HTTPCommands.class);
}

// git add src/main/java/ece448/iot_sim/HTTPCommands.java src/test/java/ece448/iot_sim/HTTPCommandsTests.java
// gradle test   for running all test cases
//  gradle grade_p2  for grade_p2 test cases
//  screenshos of all the functionality including testcases are taken.