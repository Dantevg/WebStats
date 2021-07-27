<?php

$converters = array();

// ["rename column", from, to]
$converters["rename column"] = function($row, $config, $command){
	$from = $command[1];
	$to = $command[2];
	$row[$to] = $row[$from];
	unset($row[$from]);
	return $row;
};

// ["json", column]
$converters["json"] = function($row, $config, $command){
	$column = $command[1];
	$json_data = json_decode($row[$column], true);
	if(!$json_data){
		echo "could not decode json data<br>";
		return $row;
	}
	$row = array_merge($row, $json_data);
	unset($row[$column]);
	return $row;
};

// ["key value", key-column, value-column]
$converters["key value"] = function($row, $config, $command){
	$key = $command[1];
	$value = $command[2];
	$row[$row[$key]] = $row[$value];
	unset($row[$key]);
	unset($row[$value]);
	return $row;
};

// ["uuid", column]
$converters["uuid"] = function($row, $config, $command){
	return $row;
};

?>
