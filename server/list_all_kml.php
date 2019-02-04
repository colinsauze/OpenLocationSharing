<?php
//	header('Content-Type: text/plain');
include 'includes/db_common.php';
include 'includes/config.php';
include 'colours.php';

$conn = db_init();

/*
//   black(won't show)        red        green      yellow     orange    white
$colours = array("ff000000","ff0000ff","ff00ff00","ff00ffff","ff0080ff","ffffffff");
$icons = array("http://www.microtransat.org/blackicon.png",
	       "http://www.microtransat.org/redicon.png",
	       "http://www.microtransat.org/greenicon.png",
	       "http://www.microtransat.org/yellowicon.png",
	       "http://www.microtransat.org/orangeicon.png",
	       "http://www.microtransat.org/whiteicon.png");
*/

// Creates an array of strings to hold the lines of the KML file.
$kml = array('<?xml version="1.0" encoding="UTF-8"?>');
$kml[] = '<kml xmlns="http://earth.google.com/kml/2.1">';
$kml[] = ' <Document>';

$handle = @fopen("waypoints_compsci.kml", "r");
if ($handle) {
    while (!feof($handle)) {
        $kml[] = fgets($handle, 4096);
    }
    fclose($handle);
}



$sql = "SELECT BoatID,BoatName,startTime FROM Boat order by BoatId;";
$result = mysqli_query($conn,$sql);
while($row = mysqli_fetch_assoc($result))
{
 	$boats[$count]["id"]=$row["BoatID"];
	$boats[$count]["name"]=$row["BoatName"];
	$boats[$count]["startTime"]=$row["startTime"];
	$count++;
}

foreach ($boats as $boat_id)
{
	$kml[] = '	<Style id="style'.$boat_id["id"].'">';
	$kml[] = '		<IconStyle><scale>1.2</scale></IconStyle>';
	$kml[] = '		<LineStyle><color>'.$colours[$boat_id["id"]].'</color>';
	$kml[] = '			<width>5</width>';
	$kml[] = '		</LineStyle>';
	$kml[] = '		<IconStyle>';
	$kml[] = '			<scale>0.5</scale>';
	$kml[] = '			<Icon>';
	$kml[] = '				<href>'.$icons[$boat_id["id"]].'</href>';
	$kml[] = '			</Icon>';
	$kml[] = '			<hotSpot x="16" y="16" xunits="pixels" yunits="pixels"/>';
	$kml[] = '		</IconStyle>';
	$kml[] = '	</Style>';




	//draw a line between all our points
	$kml[] = '	<Placemark>';
	$kml[] = '		<name>'.$boat_id["name"].'\'s Path</name>';
	$kml[] = '		<styleUrl>#style'.$boat_id["id"].'</styleUrl>';
	$kml[] = '		<LineString>';
	$kml[] = '			<tessellate>1</tessellate>';
	$kml[] = '			<coordinates>';
	if($boat_id["startTime"]!=NULL)
	{
		$newest_time = date("Y-m-d H:i:s",time()-(3600*24));

		$sql = "SELECT BoatID,TeamID,GPSLat,GPSLong,PositionDate,Notes FROM Position where BoatID=".$boat_id["id"]." and PositionDate>'".$boat_id["startTime"]."' order by PositionDate desc;";


		$result = mysqli_query($conn,$sql);

		if (!$result) {
			echo "Could not successfully run query ($sql) from DB: " . mysqli_error();
			exit;
		}
		// While a row of data exists, put that row in $row as an associative array
		// Note: If you're expecting just one row, no need to use a loop
		// Note: If you put extract($row); inside the following loop, you'll
		//       then create $userid, $fullname, and $userstatus
		while($row = mysqli_fetch_assoc($result))
		{
			#if ( $last_time != $row["PositionDate"] && $row["PositionDate"]!="")
			{
				//draw a line around all our points

				//while ($row = @mysql_fetch_assoc($result))
				//{
					$kml[] = $row['GPSLong'].','.$row['GPSLat'].',0 ';
				//}

				//echo "data=".$row["lat"]." ".$row["lon"]." ".$row["logtime"]."\n";
				#$last_time=$row["PositionDate"];
			}
		}
	}
	$kml[] = '			</coordinates>';
	$kml[] = '		</LineString>';
	$kml[] = '	</Placemark>';

	$newest_time = date("Y-m-d H:i:s",time()-(3600*24));

	{
                if($boat_id["startTime"]==NULL)
                {
                        continue;
                }

		$sql = "SELECT BoatID,TeamID,GPSLat,GPSLong,PositionDate,Notes FROM  Position where BoatID=".$boat_id["id"]." and PositionDate>'".$boat_id["startTime"]."' order by PositionDate;";
		$result = mysqli_query($conn,$sql);
	
		// While a row of data exists, put that row in $row as an associative array
		// Note: If you're expecting just one row, no need to use a loop
		// Note: If you put extract($row); inside the following loop, you'll
		//       then create $userid, $fullname, and $userstatus

		$kml[] = '    <Folder>';
		$kml[] = '      <name>'.$boat_id["name"].'\'s Path</name>';
		//draw a line between all our points

		while($row = mysqli_fetch_assoc($result))
		{
			$kml[] = ' <Placemark id="'.$boat_id['name'].' '.$row['PositionDate'].'">';
			$kml[] = ' <name>'.$boat_id['name']." ". htmlentities($row['PositionDate']) . '</name>';
			if ($row['Notes']!="")
			{
				$kml[] = ' <description>' . $row['Notes']. '</description>';
			}
			$kml[] = ' <styleUrl>#style'.$boat_id['id'].'</styleUrl>';
			$kml[] = ' <Point>';
			$kml[] = ' 	<coordinates>' . $row['GPSLong'] . ','  . $row['GPSLat'] . '</coordinates>';
			$kml[] = ' </Point>';
			$kml[] = ' </Placemark>';

			//echo "data=".$row["lat"]." ".$row["lon"]." ".$row["logtime"]."\n";
			$last_time=$row["PositionDate"];
		}

		$kml[] = ' </Folder>';


		if (!$result) {
			echo "Could not successfully run query ($sql) from DB: " . mysqli_error();
			exit;
		}
	}

}
// End XML file
$kml[] = ' </Document>';
$kml[] = '</kml>';
$kmlOutput = join("\n", $kml);
header('Content-type: application/vnd.google-earth.kml+xml');
echo $kmlOutput;

mysqli_close($conn);
?>
