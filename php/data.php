<?php

function get_table($db, $table){
	return json_decode(file_get_contents($db.".json"), true);
}

?>