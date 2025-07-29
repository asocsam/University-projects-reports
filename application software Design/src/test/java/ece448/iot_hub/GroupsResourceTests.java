package ece448.iot_hub;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class GroupsResourceTests {

    // A stub implementation of GroupsModel for testing the resource layer.
    static class StubGroupsModel extends GroupsModel {
        Map<String, List<String>> groupsStored = new HashMap<>();
        List<String> controlCalls = new ArrayList<>();
        
        public StubGroupsModel() {
            // Pass null for Environment and MqttController as they are not used in the stub.
            super(null, null);
        }
        
        @Override
        public void createOrUpdateGroup(String groupName, List<String> plugNames) {
            groupsStored.put(groupName, new ArrayList<>(plugNames));
        }
        
        @Override
        public void removeGroup(String groupName) {
            groupsStored.remove(groupName);
        }
        
        @Override
        public Group getGroup(String groupName) {
            List<String> plugs = groupsStored.get(groupName);
            if (plugs == null) {
                return new Group(groupName, new ArrayList<>());
            }
            // Create dummy Plug objects with fixed state ("on") and power (100)
            List<Plug> plugList = new ArrayList<>();
            for (String p : plugs) {
                plugList.add(new Plug(p, "on", 100));
            }
            return new Group(groupName, plugList);
        }
        
        @Override
        public List<Group> getAllGroups() {
            List<Group> result = new ArrayList<>();
            for (String groupName : groupsStored.keySet()) {
                result.add(getGroup(groupName));
            }
            return result;
        }
        
        @Override
        public void controlGroup(String groupName, String action) {
            controlCalls.add(groupName + ":" + action);
        }
    }
    
    private StubGroupsModel stubModel;
    private GroupsResource resource;
    
    @Before
    public void setup() {
        stubModel = new StubGroupsModel();
        resource = new GroupsResource(stubModel);
    }
    
    // 1. Test that POST creates or updates a group.
    @Test
    public void testCreateOrUpdateGroup() {
        List<String> plugs = Arrays.asList("a", "b");
        resource.createOrUpdateGroup("groupX", plugs);
        assertTrue(stubModel.groupsStored.containsKey("groupX"));
        assertEquals(2, stubModel.groupsStored.get("groupX").size());
    }
    
    // 2. Test GET without action returns a group with expected members.
    @Test
    public void testGetGroup() {
        List<String> plugs = Arrays.asList("a", "b", "c");
        stubModel.createOrUpdateGroup("groupX", plugs);
        GroupsModel.Group group = (GroupsModel.Group) resource.getOrControlGroup("groupX", null);
        assertEquals("groupX", group.getName());
        assertEquals(3, group.getMembers().size());
    }
    
    // 3. Test GET on a non-existent group returns a group with empty members.
    @Test
    public void testGetNonexistentGroup() {
        GroupsModel.Group group = (GroupsModel.Group) resource.getOrControlGroup("nonexistent", null);
        assertEquals("nonexistent", group.getName());
        assertTrue(group.getMembers().isEmpty());
    }
    
    // 4. Test GET with an action parameter calls controlGroup and returns null.
    @Test
    public void testGetGroupWithAction() {
        List<String> plugs = Arrays.asList("a", "b");
        stubModel.createOrUpdateGroup("groupX", plugs);
        Object result = resource.getOrControlGroup("groupX", "toggle");
        assertNull(result);
        assertTrue(stubModel.controlCalls.contains("groupX:toggle"));
    }
    
    // 5. Test that DELETE removes a group.
    @Test
    public void testDeleteGroup() {
        stubModel.createOrUpdateGroup("groupX", Arrays.asList("a", "b"));
        resource.deleteGroup("groupX");
        assertFalse(stubModel.groupsStored.containsKey("groupX"));
    }
    
    // 6. Test GET all groups returns the correct list.
    @Test
    public void testGetAllGroups() {
        stubModel.createOrUpdateGroup("groupX", Arrays.asList("a"));
        stubModel.createOrUpdateGroup("groupY", Arrays.asList("b", "c"));
        List<GroupsModel.Group> all = resource.getAllGroups();
        Set<String> names = new HashSet<>();
        for (GroupsModel.Group g : all) {
            names.add(g.getName());
        }
        assertEquals(2, names.size());
        assertTrue(names.contains("groupX"));
        assertTrue(names.contains("groupY"));
    }
    
    // 7. Test that GET with action on a non-existent group still calls controlGroup and returns null.
    @Test
    public void testControlNonexistentGroupViaGet() {
        Object result = resource.getOrControlGroup("nonexistent", "on");
        assertNull(result);
        assertTrue(stubModel.controlCalls.contains("nonexistent:on"));
    }
}
