import React from "react";
import {
	Routes,
	Route,
	RouterProvider,
	createBrowserRouter,
	createRoutesFromElements,
	BrowserRouter
} from "react-router-dom";

import "./App.scss";

import LeaderBoard from "./LeaderBoard/LeaderBoard";

function App() { 
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/" element={<LeaderBoard/>}>
				</Route>
			</Routes>
		</BrowserRouter>
	);
}

export default App;