import React from "react";
import {
	Routes,
	Route,
	RouterProvider,
	createBrowserRouter,
	createRoutesFromElements
} from "react-router-dom";

import "./App.scss";

import LeaderBoard from "./LeaderBoard/LeaderBoard";

function App() { 
	return (
		<Routes>
			<Route path="/" element={<LeaderBoard/>}/>
			<Route path="/a" element={<div>t</div>}/>
		</Routes>
	);
}

export default App;