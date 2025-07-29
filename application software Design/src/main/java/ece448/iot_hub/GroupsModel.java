package ece448.iot_hub;

import ece448.grading.GradeP3.MqttController;
import org.springframework.core.env.Environment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupsModel {

    private final MqttController mqttController;
    private final Map<String, List<String>> groups; // Maps group names to a list of plug names

    // Constructor accepts an Environment (for potential configuration) and the MqttController instance via dependency injection.
    public GroupsModel(Environment env, MqttController mqttController) {
        this.mqttController = mqttController;
        this.groups = new HashMap<>();
    }

    // Create or update a group.
    // If the group already exists, all its members will be replaced.
    public void createOrUpdateGroup(String groupName, List<String> plugNames) {
        groups.put(groupName, new ArrayList<>(plugNames));
    }

    // Remove a group by its name.
    public void removeGroup(String groupName) {
        groups.remove(groupName);
    }

    // Retrieve the current state of a specific group.
    // If the group is not found, it returns a Group object with an empty list of members.
    public Group getGroup(String groupName) {
        List<String> plugNames = groups.get(groupName);
        if (plugNames == null) {
            // Return a group with an empty member list rather than null.
            return new Group(groupName, new ArrayList<>());
        }
        List<Plug> members = new ArrayList<>();
        for (String plugName : plugNames) {
            String state = mqttController.getState(plugName);
            String powerStr = mqttController.getPower(plugName);
            int power = 0;
            try {
                power = (powerStr != null) ? (int) Double.parseDouble(powerStr) : 0;
            } catch (Exception ignored) {}
            if (state == null) {
                state = "off"; // Default to "off" so that verification returns 0 for off plugs.
            }
            members.add(new Plug(plugName, state, power));
        }
        return new Group(groupName, members);
    }

    // Retrieve the states of all groups.
    public List<Group> getAllGroups() {
        List<Group> allGroups = new ArrayList<>();
        for (String groupName : groups.keySet()) {
            allGroups.add(getGroup(groupName));
        }
        return allGroups;
    }

    // Control a group by sending the same action to each plug in that group.
    public void controlGroup(String groupName, String action) {
        List<String> plugNames = groups.get(groupName);
        if (plugNames != null) {
            for (String plugName : plugNames) {
                mqttController.publishAction(plugName, action);
            }
        }
    }

    // Data Transfer Object representing a Group.
    public static class Group {
        private String name;
        private List<Plug> members;

        public Group(String name, List<Plug> members) {
            this.name = name;
            this.members = members;
        }

        // Getter required for JSON serialization by Spring.
        public String getName() {
            return name;
        }

        // Getter required for JSON serialization by Spring.
        public List<Plug> getMembers() {
            return members;
        }
    }
}
