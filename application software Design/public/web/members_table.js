const btnClassAdd = "btn btn-primary btn-block";
const btnClassDel = "btn btn-danger btn-block";
const btnClassOn = "btn btn-success btn-block";
const btnClassOff = "btn btn-warning btn-block";
const btnClassToggle = "btn btn-secondary btn-block";

/**
 * Bulk-control row: On / Off / Toggle per group
 */
function ControlGroupsRow({ groupNames, onControlGroup }) {
    return (
        <tr>
            <td>Control Group</td>
            {groupNames.map(name => (
                <td key={name}>
                    <button className={btnClassOn}
                        onClick={() => onControlGroup(name, "on")}>On</button>
                    <button className={btnClassOff}
                        onClick={() => onControlGroup(name, "off")}>Off</button>
                    <button className={btnClassToggle}
                        onClick={() => onControlGroup(name, "toggle")}>Toggle</button>
                </td>
            ))}
            <td></td>
        </tr>
    );
}

/**
 * Table header (unchanged from lecture)
 */
function Header({ groupNames }) {
    return (
        <thead>
            <tr>
                <th rowSpan="2" width="10%">Members</th>
                <th colSpan={groupNames.length}>Groups</th>
                <th rowSpan="2" width="10%">Remove from All Groups</th>
            </tr>
            <tr>
                {groupNames.map(name => (
                    <th key={name}><button className={btnClassAdd}>{name}</button></th>
                ))}
            </tr>
        </thead>
    );
}

/**
 * Single-member row (unchanged)
 */
function Row(props) {
    const { memberName, members, onMemberChange, onAddMemberToAllGroups } = props;
    const groupNames = members.get_group_names();
    return (
        <tr>
            <td>
                <button className={btnClassAdd}
                    onClick={() => onAddMemberToAllGroups(memberName)}>
                    {memberName}
                </button>
            </td>
            {groupNames.map(grp => {
                const checked = members.is_member_in_group(memberName, grp);
                return (
                    <td key={grp}>
                        <input type="checkbox"
                            checked={checked}
                            onChange={() => onMemberChange(memberName, grp)} />
                    </td>
                );
            })}
            <td>
                <button className={btnClassDel}
                    onClick={() => props.onDeleteGroup(props.memberName)}>
                    X
                </button>
            </td>
        </tr>
    );
}

/**
 * Delete-groups row (unchanged)
 */
function DeleteGroupsRow({ groupNames, onDeleteGroup }) {
    return (
        <tr>
            <td></td>
            {groupNames.map(name => (
                <td key={name}>
                    <button className={btnClassDel}
                        onClick={() => onDeleteGroup(name)}>
                        X
                    </button>
                </td>
            ))}
            <td></td>
        </tr>
    );
}

/**
 * Add-group form row (unchanged)
 */
function AddGroup(props) {
    return (
        <div>
            <label>Group Name</label>
            <input type="text"
                onChange={e => props.onInputNameChange(e.target.value)}
                value={props.inputName} />
            <label>Members</label>
            <input type="text" size="60"
                placeholder="e.g. a,b,c"
                onChange={e => props.onInputMembersChange(e.target.value)}
                value={props.inputMembers} />
            <button className="btn btn-primary"
                onClick={props.onAddGroup}>
                Add/Replace
            </button>
        </div>
    );
}

/**
 * Table body + overall table
 */
function MembersTable(props) {
    const groups = props.members.get_group_names();
    if (groups.length === 0) {
        return (
            <div>
                <div>There are no groups.</div>
                <AddGroup
                    inputName={props.inputName}
                    inputMembers={props.inputMembers}
                    onInputNameChange={props.onInputNameChange}
                    onInputMembersChange={props.onInputMembersChange}
                    onAddGroup={props.onAddGroup}
                />
            </div>
        );
    }

    return (
        <table className="table table-striped table-bordered">
            <Header groupNames={groups} />
            <tbody>
                <ControlGroupsRow
                    groupNames={groups}
                    onControlGroup={props.onControlGroup}
                />
                {props.members.get_member_names().map(memberName => (
                    <Row key={memberName}
                        memberName={memberName}
                        members={props.members}
                        onMemberChange={props.onMemberChange}
                        onDeleteGroup={props.onDeleteGroup}
                        onAddMemberToAllGroups={props.onAddMemberToAllGroups}
                    />
                ))}
                <DeleteGroupsRow
                    groupNames={groups}
                    onDeleteGroup={props.onDeleteGroup}
                />
                <tr>
                    <td colSpan={groups.length + 2}>
                        <AddGroup
                            inputName={props.inputName}
                            inputMembers={props.inputMembers}
                            onInputNameChange={props.onInputNameChange}
                            onInputMembersChange={props.onInputMembersChange}
                            onAddGroup={props.onAddGroup}
                        />
                    </td>
                </tr>
            </tbody>
        </table>
    );
}

window.MembersTable = MembersTable;