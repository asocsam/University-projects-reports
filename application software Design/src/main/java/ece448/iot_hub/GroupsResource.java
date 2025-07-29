package ece448.iot_hub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupsResource {

    private final GroupsModel groupsModel;

    // The GroupsModel is injected via the constructor.
    @Autowired
    public GroupsResource(GroupsModel groupsModel) {
        this.groupsModel = groupsModel;
    }

    // Create or update a group.
    // Endpoint: POST /api/groups/{groupName}
    // Request body: JSON array of plug names.
    @PostMapping("/{groupName}")
    public void createOrUpdateGroup(@PathVariable String groupName, @RequestBody List<String> plugNames) {
        groupsModel.createOrUpdateGroup(groupName, plugNames);
    }

    // Remove a group.
    // Endpoint: DELETE /api/groups/{groupName}
    @DeleteMapping("/{groupName}")
    public void deleteGroup(@PathVariable String groupName) {
        groupsModel.removeGroup(groupName);
    }

    // Get the state of a group or, if an action is provided, control the group.
    // Endpoint: GET /api/groups/{groupName}?action={action}
    @GetMapping("/{groupName}")
    public Object getOrControlGroup(@PathVariable String groupName,
                                    @RequestParam(value = "action", required = false) String action) {
        if (action != null) {
            groupsModel.controlGroup(groupName, action);
            return null;
        } else {
            // Return the group even if it has no members.
            return groupsModel.getGroup(groupName);
        }
    }

    // Retrieve the states of all groups.
    // Endpoint: GET /api/groups
    @GetMapping
    public List<GroupsModel.Group> getAllGroups() {
        return groupsModel.getAllGroups();
    }
}
