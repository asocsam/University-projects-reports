/**
 * The App class is a controller holding the global state.
 * It creates its child controller in render().
 */
class MembersApp extends React.Component {
  constructor(props) {
    super(props);
    console.info("MembersApp constructor()");
  }

  render() {
    console.info("MembersApp render()");
    return (
      <div className="container">
        <div className="row">
          <div className="col-sm-12">
            <h2>Manage Groups and Members</h2>
            {/* This will mount the Members controller + table */}
            <Members />
          </div>
        </div>
      </div>
    );
  }
}

// expose to the page
window.MembersApp = MembersApp;