/**
 * A model for managing members in groups.
 */
function create_members_model(groups) {
    // create the data structure
    var all_members = new Set();
    var group_names = [];
    var group_members = new Map();
    for (var group of groups) {
        group_names.push(group.name);
        var members = new Set(group.members.map(p => p.name || p));
        group_members.set(group.name, members);
        members.forEach(member => all_members.add(member));
    }
    var member_names = Array.from(all_members);
    group_names.sort();
    member_names.sort();

    return {
        get_group_names: () => group_names,
        get_member_names: () => member_names,
        is_member_in_group: (member, grp) => {
            const m = group_members.get(grp);
            return m ? m.has(member) : false;
        },
        get_group_members: grp => group_members.get(grp) || new Set()
    };
}

/**
 * The Members controller holds the state of groups.
 */
class Members extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            members: create_members_model([]),
            inputName: "",
            inputMembers: ""
        };
    }

    componentDidMount() {
        this.getGroups();
        // poll every second for multi-user sync
        this.interval = setInterval(this.getGroups, 1000);
    }

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    getGroups = () => {
        console.debug("RESTful: get groups");
        fetch("/api/groups")
            .then(r => r.json())
            .then(groups => this.showGroups(groups))
            .catch(err => console.error("Members: getGroups", err));
    }

    showGroups = groups => {
        this.setState({ members: create_members_model(groups) });
    }

    createGroup = (groupName, groupMembers) => {
        console.info(`RESTful: create group ${groupName}`, groupMembers);
        fetch(`/api/groups/${groupName}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(groupMembers)
        })
            .then(() => this.getGroups())
            .catch(err => console.error("Members: createGroup", err));
    }

    deleteGroup = groupName => {
        console.info(`RESTful: delete group ${groupName}`);
        fetch(`/api/groups/${groupName}`, { method: "DELETE" })
            .then(() => this.getGroups())
            .catch(err => console.error("Members: deleteGroup", err));
    }

    // Control a whole group: on/off/toggle
    controlGroup = (groupName, action) => {
        console.info(`RESTful: control group ${groupName} ${action}`);
        fetch(`/api/groups/${groupName}?action=${action}`)
            .then(() => this.getGroups())
            .catch(err => console.error("Members: controlGroup", err));
    }

    onMemberChange = (memberName, groupName) => {
        const current = new Set(this.state.members.get_group_members(groupName));
        const updated = current.has(memberName)
            ? Array.from([...current].filter(m => m !== memberName))
            : Array.from(current).concat(memberName);
        this.createGroup(groupName, updated);
    }

    onDeleteGroup = groupName => this.deleteGroup(groupName);

    onInputNameChange = inputName => this.setState({ inputName });
    onInputMembersChange = inputMembers => this.setState({ inputMembers });

    onAddGroup = () => {
        const members = this.state.inputMembers.split(",").map(s => s.trim()).filter(s => s);
        this.createGroup(this.state.inputName, members);
    }

    onAddMemberToAllGroups = memberName => {
        const updates = this.state.members.get_group_names().map(grp => {
            const mems = new Set(this.state.members.get_group_members(grp));
            mems.add(memberName);
            return { name: grp, members: Array.from(mems) };
        });
        Promise.all(updates.map(u =>
            fetch(`/api/groups/${u.name}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(u.members)
            })
        ))
            .then(() => this.getGroups())
            .catch(err => console.error("Members: bulk update", err));
    }

    render() {
        return (
            <MembersTable
                members={this.state.members}
                inputName={this.state.inputName}
                inputMembers={this.state.inputMembers}
                onMemberChange={this.onMemberChange}
                onDeleteGroup={this.onDeleteGroup}
                onControlGroup={this.controlGroup}
                onInputNameChange={this.onInputNameChange}
                onInputMembersChange={this.onInputMembersChange}
                onAddGroup={this.onAddGroup}
                onAddMemberToAllGroups={this.onAddMemberToAllGroups}
            />
        );
    }
}

window.Members = Members;