<?php

function convert($table, $config_item, $converters){
	if(!array_key_exists("convert", $config_item)) return $table;
	foreach($config_item["convert"] as $command){
		if(!array_key_exists($command[0], $converters)){
			die("no such converter: " . $command[0]);
		}
		foreach($table as &$row) $row = $converters[$command[0]]($row, $config_item, $command);
	}
	return $table;
}

$converters = array();

// ["rename", from, to]
$converters["rename"] = function($row, $config, $command){
	$from = $command[1];
	$to = $command[2];
	$row[$to] = $row[$from];
	unset($row[$from]);
	return $row;
};

// ["key-value", key-column, value-column]
$converters["key-value"] = function($row, $config, $command){
	$key = $command[1];
	$value = $command[2];
	$row[$row[$key]] = $row[$value];
	unset($row[$key]);
	unset($row[$value]);
	return $row;
};

// ["json", column]
$converters["json"] = function($row, $config, $command){
	$column = $command[1];
	$json_data = json_decode($row[$column], true);
	if(!$json_data) die("could not decode json data");
	$row = array_merge($row, $json_data);
	unset($row[$column]);
	return $row;
};

// ["uuid", column]
$converters["uuid"] = function($row, $config, $command){
	return $row;
};

?>
