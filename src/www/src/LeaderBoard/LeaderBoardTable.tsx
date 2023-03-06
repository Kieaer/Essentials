import React from "react";

import Box from '@mui/material/Box';
import Collapse from '@mui/material/Collapse';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';

import axios from "axios";

import "./LeaderBoardTable.scss"

interface Column {
  	id: 'rank' | 'username' | 'level' | 'exp' | 'playtime';
  	label: string;
  	minWidth?: number;
  	align?: 'right';
  	format?: (value: number) => string;
}

const columns: readonly Column[] = [
  	{ id: 'rank', label: '', minWidth: 20 },
  	{ id: 'username', label: 'Username', minWidth: 100 },
  	{
  	  	id: 'level',
  	  	label: 'Level',
  	  	minWidth: 170,
  	  	align: 'right',
  	  	format: (value: number) => value.toLocaleString('en-US'),
  	},
  	{
  	  	id: 'exp',
  	  	label: 'Exp',
  	  	minWidth: 170,
  	  	align: 'right',
  	  	format: (value: number) => value.toLocaleString('en-US'),
  	},
  	{
  	  	id: 'playtime',
  	  	label: 'playtime',
  	  	minWidth: 20,
  	  	align: 'right',
  	  	format: (value: number) => {
			const date = new Date(0);
			date.setSeconds(value);
			return date.toISOString().substring(11, 19);
		},
  	},
];

interface Data {
  	rank: number;
  	username: string;
  	level: number;
  	exp: number;
  	playtime: number;
	stat: {
		attackclear: number,
		pvpwin: number,
		pvplose: number
	}
}

function createData(
  	rank: number,
  	username: string,
  	level: number,
	exp: number,
	playtime: number,
	stat: {
		attackclear: number,
		pvpwin: number,
		pvplose: number
	}
): Data {
  	return { rank, username, level, exp, playtime, stat };
}

function Row(props: { row: ReturnType<typeof createData> }) {
	const { row } = props;
	const [ open, setOpen ] = React.useState(false);

	return (
		<React.Fragment>
			<TableRow hover role="checkbox" tabIndex={-1} key={row.rank}>
				{columns.map((column) => {
					const value = row[column.id];
					return (
						<TableCell key={column.id} align={column.align}>
							{column.format && typeof value === 'number'
							? column.format(value)
							: value}
						</TableCell>

					);
				})}
				<TableCell>
        	  		<IconButton
        	    		aria-label="expand row"
        	    		size="small"
        	    		onClick={() => setOpen(!open)}
        	  		>
        	    		{open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
        	  		</IconButton>
        		</TableCell>
			</TableRow>
			<TableRow>
				<TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
				  	<Collapse in={open} timeout="auto" unmountOnExit>
						<Box sx={{ margin: 1 }}>
					  		<Typography variant="h6" gutterBottom component="div">
								Status
					  		</Typography>
					  		<Table size="small" aria-label="purchases">
					   			<TableHead>
						 	 		<TableRow>
										<TableCell>AttackClear</TableCell>
										<TableCell>PVPWin</TableCell>
										<TableCell >PVPWinRate</TableCell>
						  			</TableRow>
								</TableHead>
								<TableBody>
									<TableRow key={0}>
							  			<TableCell component="th" scope="row">
											{row.stat.attackclear}
							  			</TableCell>
							  			<TableCell>
											{row.stat.pvpwin}
										</TableCell>
							  			<TableCell>
											{(Number(row.stat.pvpwin) + Number(row.stat.pvplose)) == 0 ? "Unrated" : ((Number(row.stat.pvpwin) / (Number(row.stat.pvpwin) + Number(row.stat.pvplose))) * 100).toFixed(2) + "%"}
							  			</TableCell>
									</TableRow>
								</TableBody>
					  		</Table>
						</Box>
				  	</Collapse>
				</TableCell>
			</TableRow>
		</React.Fragment>
	);
}

export default function LeaderBoardTable() {
	const [page, setPage] = React.useState(0);
	const [rowsPerPage, setRowsPerPage] = React.useState(10);

	const handleChangePage = (event: unknown, newPage: number) => {
		setPage(newPage);
	};

	const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
		setRowsPerPage(+event.target.value);
		setPage(0);
	};

	const [ loading, setLoading ] = React.useState(true);
	const [ error429, setError429 ] = React.useState(false);
	const [ unknownerror, setUnknownError ] = React.useState(false);

	const [ rows, setRows ] = React.useState([{ rank: 1, username: "", level: 0, exp: 0, playtime: 0, stat: {
		attackclear: 0,
		pvpwin: 0,
		pvplose: 0
	}}]);

	const fetchData = async () => {
		setLoading(true);
		try {
			setRows((await axios.get("/api/ranking")).data["data"]
				.sort((a: any, b: any) => a.exp - b.exp));
		} catch (e: any) {
			if (axios.isAxiosError(e)) {
				if (e.response) {
					if (e.response.status == 429) {
						setLoading(false);
						setError429(true);
					} else {
						setLoading(false);
						setUnknownError(true);
					}
				} else {
					console.log(e.message);
				}
			} else {
				console.log(e.message);
			}
		}
		setLoading(false);
	};

	React.useEffect(() => {
		fetchData();
	}, []);

	if (loading) return <div>loading...</div>;
	if (error429) return <div>too many requests! please try after few seconds.</div>;
	if (unknownerror) return <div>unknown error occured. please tell the error code poped up on developer console to administarator</div>

	return (
		<div className="LeaderBoardTable">
			<Paper sx={{ width: '100%', overflow: 'hidden' }}>
				<TableContainer sx={{ maxHeight: 440 }}>
					<Table stickyHeader aria-label="sticky table">
						<TableHead>
							<TableRow>
				  				{columns.map((column) => (
									<TableCell
					  				key={column.id}
					  				align={column.align}
					  				style={{ minWidth: column.minWidth }}
									>
					 		 			{column.label}
									</TableCell>
				  				))}
								<TableCell style={{ minWidth: 20 }}>
									Status
								</TableCell>
							</TableRow>
			  			</TableHead>
			  			<TableBody>
							{rows
				  			.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
				  			.map((row) => {
								return (
					  				<Row key={row.rank} row={row} />
								);
				  			})}
			  			</TableBody>
					</Table>
	  			</TableContainer>
	  			<TablePagination
					rowsPerPageOptions={[10, 25, 50]}
					component="div"
					count={rows.length}
					rowsPerPage={rowsPerPage}
					page={page}
					onPageChange={handleChangePage}
					onRowsPerPageChange={handleChangeRowsPerPage}
	  			/>
			</Paper>
		</div>
  	);
}
