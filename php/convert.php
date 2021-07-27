<?php

$converters = array();

$converters["json"] = function($row, $config){
	return $row;
};

$converters["uuid"] = function($row, $config){
	return $row;
};

?>
