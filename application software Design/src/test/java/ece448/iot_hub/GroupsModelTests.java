package ece448.iot_hub;

import ece448.grading.GradeP3.MqttController;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class GroupsModelTests {

    // A stub for MqttController similar to PlugsModelTests.StubMqttController.
    static class StubMqttController extends MqttController {
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
        public void publishAction(String plugName, String action) {
            this.lastPlug = plugName;
            this.lastAction = action;
        }
    }
    
    // For simplicity we pass null for Environment since GroupsModel does not use it.
    private GroupsModel groupsModel;
    private StubMqttController mqtt;
    
    @Before
    public void setup() throws Exception {
        mqtt = new StubMqttController();
        groupsModel = new GroupsModel(null, mqtt);
    }
    
    // 1. Test that a non-existent group returns a Group with empty members.
    @Test
    public void testGetNonexistentGroupReturnsEmptyMembers() {
        GroupsModel.Group group = groupsModel.getGroup("nonexistent");
        assertEquals("nonexistent", group.getName());
        assertTrue(group.getMembers().isEmpty());
    }
    
    // 2. Test creating a group and then retrieving it with proper plug states and power.
    @Test
    public void testCreateAndRetrieveGroup() {
        List<String> plugs = Arrays.asList("plug1", "plug2");
        groupsModel.createOrUpdateGroup("groupA", plugs);
        
        // Set up responses for each plug.
        mqtt.setState("plug1", "on");
        mqtt.setPower("plug1", "150.0");
        
        mqtt.setState("plug2", "off");
        mqtt.setPower("plug2", "0.0");
        
        GroupsModel.Group group = groupsModel.getGroup("groupA");
        assertEquals("groupA", group.getName());
        assertEquals(2, group.getMembers().size());
        
        List<String> plugNames = new ArrayList<>();
        for (Plug p : group.getMembers()) {
            plugNames.add(p.getName());
        }
        assertTrue(plugNames.contains("plug1"));
        assertTrue(plugNames.contains("plug2"));
    }
    
    // 3. Test updating a group (replacing previous plugs).
    @Test
    public void testUpdateGroup() {
        List<String> plugs1 = Collections.singletonList("plug1");
        groupsModel.createOrUpdateGroup("groupA", plugs1);
        List<String> plugs2 = Arrays.asList("plug2", "plug3");
        groupsModel.createOrUpdateGroup("groupA", plugs2);
        
        GroupsModel.Group group = groupsModel.getGroup("groupA");
        List<String> plugNames = new ArrayList<>();
        for (Plug p : group.getMembers()) {
            plugNames.add(p.getName());
        }
        assertEquals(2, plugNames.size());
        assertTrue(plugNames.contains("plug2"));
        assertTrue(plugNames.contains("plug3"));
    }
    
    // 4. Test deleting a group.
    @Test
    public void testDeleteGroup() {
        List<String> plugs = Arrays.asList("plug1", "plug2");
        groupsModel.createOrUpdateGroup("groupA", plugs);
        groupsModel.removeGroup("groupA");
        
        GroupsModel.Group group = groupsModel.getGroup("groupA");
        // After deletion, our implementation returns a Group with no members.
        assertEquals("groupA", group.getName());
        assertTrue(group.getMembers().isEmpty());
    }
    
    // 5. Test controlling a group sends the action to every plug.
    @Test
    public void testControlGroup() {
        List<String> plugs = Arrays.asList("plug1", "plug2", "plug3");
        groupsModel.createOrUpdateGroup("groupA", plugs);
        
        groupsModel.controlGroup("groupA", "toggle");
        for (String plug : plugs) {
            // Each plug should have triggered publishAction.
            assertEquals("toggle", mqtt.lastAction); // Since our stub only records the last call.
            // (In a more advanced stub, you might record per-plug calls.)
        }
    }
    
    // 6. Test getAllGroups returns all groups created.
    @Test
    public void testGetAllGroups() {
        groupsModel.createOrUpdateGroup("groupA", Collections.singletonList("plug1"));
        groupsModel.createOrUpdateGroup("groupB", Arrays.asList("plug2", "plug3"));
        
        List<GroupsModel.Group> allGroups = groupsModel.getAllGroups();
        Set<String> groupNames = new HashSet<>();
        for (GroupsModel.Group g : allGroups) {
            groupNames.add(g.getName());
        }
        assertEquals(2, groupNames.size());
        assertTrue(groupNames.contains("groupA"));
        assertTrue(groupNames.contains("groupB"));
    }
    
    // 7. Test default state and invalid power format: if a plug’s state is null and power is invalid.
    @Test
    public void testDefaultStateAndInvalidPowerFormat() {
        // Create a group with two plugs.
        groupsModel.createOrUpdateGroup("groupA", Arrays.asList("plugX", "plugY"));
        // For plugX, leave state and power as null.
        // For plugY, set a valid state but an invalid power string.
        mqtt.setState("plugY", "on");
        mqtt.setPower("plugY", "abc"); // invalid
        
        GroupsModel.Group group = groupsModel.getGroup("groupA");
        for (Plug p : group.getMembers()) {
            if (p.getName().equals("plugX")) {
                assertEquals("off", p.getState()); // defaults to "off"
                assertEquals(0, p.getPower());
            }
            if (p.getName().equals("plugY")) {
                assertEquals("on", p.getState());
                assertEquals(0, p.getPower()); // fallback to 0 when parsing fails
            }
        }
    }
    
    // 8. Test power rounding: ensure that a float power value rounds down.
    @Test
    public void testPowerRounding() {
        groupsModel.createOrUpdateGroup("groupA", Collections.singletonList("plugZ"));
        mqtt.setState("plugZ", "on");
        mqtt.setPower("plugZ", "99.9");
        
        GroupsModel.Group group = groupsModel.getGroup("groupA");
        assertFalse(group.getMembers().isEmpty());
        Plug plug = group.getMembers().get(0);
        assertEquals("plugZ", plug.getName());
        assertEquals("on", plug.getState());
        assertEquals(99, plug.getPower());
    }
    @Test
    public void testControlEmptyGroup() {
    // Create a group with an empty list
    groupsModel.createOrUpdateGroup("emptyGroup", new ArrayList<>());

    // Attempt to control it
    groupsModel.controlGroup("emptyGroup", "toggle");

    // Because the group has zero plugs, we expect no action to be published.
    // That means mqtt.lastPlug and mqtt.lastAction (in your stub) should remain null
    // or still have their old values from previous tests.
    assertNull("No plug should have been acted upon", mqtt.lastPlug);
    assertNull("No action should have been recorded", mqtt.lastAction);
}
@Test
  public void testControlNonExistentGroup() {
    mqtt.lastPlug = null;
    mqtt.lastAction = null;
    groupsModel.controlGroup("nonExistentGroup", "toggle");
    assertNull("No plug should have been acted upon", mqtt.lastPlug);
    assertNull("No action should have been recorded", mqtt.lastAction);
  }
  @Test
  public void testControlGroupWithNullPlugNames() {
    // Do not create this group — it will not exist in the map
    groupsModel.controlGroup("nonexistentGroup", "toggle");

    // plugNames == null → skip loop → condition evaluates to false
    assertNull(mqtt.lastPlug);
    assertNull(mqtt.lastAction);
  }
}
