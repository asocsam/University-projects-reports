function plugAction(name, action) {
	var url = "../api/plugs/" + name + "?action=" + action;
	console.info("PlugDetails: request " + url);
	fetch(url);
  }
  
  /**
   * This is a stateless view showing details of one plug.
   */
  window.PlugDetails = function (props) {
	const plug = props.plugSelected;
	const [minutes, setMinutes] = React.useState(1);
	const chartRef = React.useRef(null);
	const chartInstance = React.useRef(null);
  
	// Initialize chart when plug changes
	React.useEffect(() => {
	  if (!chartRef.current || chartInstance.current) return;
	  const ctx = chartRef.current.getContext('2d');
	  chartInstance.current = new Chart(ctx, {
		type: 'line',
		data: { datasets: [{ label: 'Power (W)', data: [] }] },
		options: {
		  scales: {
			x: { type: 'time', time: { unit: 'second' } },
			y: { beginAtZero: true }
		  }
		}
	  });
	}, [plug]);
  
	// Poll for history and update chart
	React.useEffect(() => {
	  if (!plug || !chartInstance.current) return;
	  const id = setInterval(() => {
		fetch(`/api/plugs/${encodeURIComponent(plug.name)}/history?minutes=${minutes}`)
		  .then(r => r.json())
		  .then(data => {
			chartInstance.current.data.datasets[0].data =
			  data.map(p => ({ x: p.timestamp, y: p.power }));
			chartInstance.current.update();
		  });
	  }, 1000);
	  return () => clearInterval(id);
	}, [plug, minutes]);
  
	if (!plug) return <div>Please select a plug from the left.</div>;
  
	return (
	  <div>
		<p><strong>Plug</strong> {plug.name}</p>
		<p><strong>State</strong> {plug.state}</p>
		<p><strong>Power</strong> {plug.power} W</p>
		<button className="btn btn-primary me-2" onClick={() => plugAction(plug.name, 'on')}>Switch On</button>
		<button className="btn btn-primary" onClick={() => plugAction(plug.name, 'off')}>Switch Off</button>
  
		<div className="mt-4">
		  <label className="me-2">Time window:</label>
		  <select value={minutes} onChange={e => setMinutes(+e.target.value)}>
			<option value={1}>1 min</option>
			<option value={5}>5 min</option>
			<option value={15}>15 min</option>
		  </select>
		</div>
  
		<canvas ref={chartRef} width="600" height="200" className="mt-3" />
	  </div>
	);
  };