<pre>
<?php

require("data.php");
require("convert.php");

function convert_table($table, $config_item, $config, $converters){
	if(!array_key_exists("convert", $config_item)) return $table;
	foreach($config_item["convert"] as $converter){
		if(!array_key_exists($converter, $converters)){
			echo "no such converter: ".$converter;
			continue;
		}
		foreach($table as &$row) $row = $converters[$converter]($row, $config);
	}
	return $table;
}

function rename_columns($table, $config_item, $config){
	if(!array_key_exists("renameColumns", $config_item)) return $table;
	foreach($config_item["renameColumns"] as $from => $to){
		foreach($table as &$row){
			$row[$to] = $row[$from];
			unset($row[$from]);
		}
	}
	return $table;
}

// Read config file
$config_string = file_get_contents("config.json");
$config = json_decode($config_string, true);

$main_key = "player";

// Get and convert each database/table combination
$data = array();
foreach($config as $config_item){
	$table = get_table($config_item["database"], $config_item["table"]);
	$table = convert_table($table, $config_item, $config, $converters);
	$table = rename_columns($table, $config_item, $config);
	$data = array_merge($data, $table);
}

// Transpose to players
$players = array();
foreach($data as $row){
	$player = $row[$main_key];
	$players[$player] = array_merge($players[$player] ?? array(), $row);
	unset($players[$player][$main_key]); // Remove key from values
}

// Transpose again to stats
$stats = array();
foreach($players as $player_name => $scores){
	foreach($scores as $stat_name => $stat_value){
		if(!array_key_exists($stat_name, $stats)) $stats[$stat_name] = array();
		$stats[$stat_name][$player_name] = $stat_value;
	}
}

// Output
// echo(json_encode($stats));
print_r($stats);

?>
</pre>
