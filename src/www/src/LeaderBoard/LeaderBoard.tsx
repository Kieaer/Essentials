import React from "react";

import "./LeaderBoard.scss"

import LeaderBoardTable from "./LeaderBoardTable";

function LeaderBoard() { 
	return (
		<div className="LeaderBoard">
			<div className="Caption">
				Leaderboard
			</div>
			<LeaderBoardTable/>
		</div>
	);
}

export default LeaderBoard;