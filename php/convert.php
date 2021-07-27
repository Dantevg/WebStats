<?php

$converters = array();

$converters["json"] = function($row, $config){
	$new_row = array();
	$new_row[$config["key"]] = $row[$config["key"]];
	$key = $config["columns"][0];
	$json_data = json_decode($row[$key], true);
	if(!$json_data){
		echo "could not decode json data<br>";
		return $new_row;
	}
	return array_merge($new_row, $json_data);
};

$converters["keyvalue"] = function($row, $config){
	$key = $config["columns"][0];
	$value = $config["columns"][1];
	$row[$row[$key]] = $row[$value];
	unset($row[$key]);
	unset($row[$value]);
	return $row;
};

$converters["uuid"] = function($row, $config){
	return $row;
};

?>
