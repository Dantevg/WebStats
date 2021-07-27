<?php

function get_table($database, $table){
	$hostname = "localhost";
	$username = "USERNAME";
	$password = "PASSWORD";
	
	$sql = "SELECT * FROM ".$table;
	
	try{
		$conn = new PDO("mysql:host=$hostname;dbname=$database", $username, $password);
	}catch(PDOException $e){
		die("Connection to database failed: " . $e->getMessage());
	}
	
	$query = $conn->query($sql);
	$data = $query->fetchAll(PDO::FETCH_ASSOC);
	$conn = null;
	return $data;
}

?>
