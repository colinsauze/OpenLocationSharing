<?php
session_start();

include 'includes/db_common.php';
include 'includes/config.php';

$conn = db_init();


// Creates an array of strings to hold the lines of the KML file.
$gpx = array('<?xml version="1.0" encoding="UTF-8"?>
<gpx
  version="1.0"
  creator="GPSBabel - http://www.gpsbabel.org"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://www.topografix.com/GPX/1/0"
  xsi:schemaLocation="http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd">');


 $sql = "SELECT BoatID,BoatName,startTime FROM Boat  where startTime is not null order by BoatId";
// where startTime is not null;";
  $result_boat = mysqli_query($conn,$sql);
  while($row2 = mysqli_fetch_assoc($result_boat))
  {
    /*$boats[$count]["id"]=$row["BoatID"];
    $boats[$count]["name"]=$row["BoatName"];
    $boats[$count]["startTime"]=$row["startTime"];
    $count++;*/

	//print_r($row2);
	echo "boat name ".$row2['BoatName']."\n";
/*	if($boat_id['startTime']==NULL)
	{
		continue;
	}*/
	$sql =  "SELECT BoatID,TeamID,GPSLat,GPSLong,PositionDate,Notes FROM  Position where BoatID=".$row2['BoatID']." order by PositionDate desc;"; 
	$result = mysqli_query($conn,$sql);

	if (!$result) {
	    die("Could not successfully run query ($sql) from DB: " . mysqli_error());
	}

	if (mysqli_num_rows($result) == 0) {
		continue;
	}


	while ($row = @mysqli_fetch_assoc($result))
	{
		$gpx[] = '<wpt lat="'.$row['GPSLat'].'" lon="'.$row['GPSLong'].'">';
		$gpx[] = '    <name>'.$row2['BoatName'].' '.$row['PositionDate'].'</name>';
		$gpx[] = '</wpt>';
	}

	//draw the points
	$result = mysqli_query($conn,$sql);

	if (!$result) {
		die("Could not successfully run query ($sql) from DB: " . mysqli_error());
	}

	if (mysqli_num_rows($result) == 0) {
		die("No rows found, nothing to print so am exiting");
	}

	$gpx[] = '<trk>
		      <name>'.$row2["BoatName"].'\'s Path</name>
		      <trkseg>';


	// Iterates through the rows, printing a node for each row.
	while ($row = @mysqli_fetch_assoc($result))
	{
		$gpx[] = ' <trkpt lat="'.$row['GPSLat'].'" lon="'.$row['GPSLong'].'"/>';
	}

	$gpx[] = '</trkseg>';
	$gpx[] = '</trk>';

    }

// End XML file
$gpx[] = ' </Document>';
$gpx[] = '</gpx>';
$gpxOutput = join("\n", $gpx);
//header('Content-type: application/vnd.google-earth.kml+xml');
header('Content-type: text/plain');
//header('Content-disposition: attachment; filename='.$_SESSION['teamname'].'-microtransat.kml');
echo $gpxOutput;

mysqli_close($conn);

?>




