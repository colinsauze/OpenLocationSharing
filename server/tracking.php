<?php
require_once($_SERVER['DOCUMENT_ROOT']."/tracking/includes/db_common.php");

function seconds_to_nice_time($seconds)
{
    $periods = array(
    array("day",24*60*60),
    array("hour",60*60),
    array("minute",60));

    $seconds_tmp=$seconds;
    $text="";
    
    for ($i=0;$i<count($periods);$i++)
    {
    $unit = floor($seconds_tmp / $periods[$i][1]);
    
    if ( $unit > 0 || $seconds-$seconds_tmp>0)
    {
        $text=$text.$unit." ".$periods[$i][0];
        if($unit>1)
        {
        $text=$text."s";
        }
        if($i<count($periods)-1)
        {
        $text=$text.", ";
        }
    }
    
    $seconds_tmp = $seconds_tmp - ($unit * $periods[$i][1]);
    } 
    return $text;
}
?>
<!DOCTYPE html>
<html>
	<head>
		<meta name="Author" content="Colin Sauze" />
		<meta name="Keywords" content="microtransat,aberystwyth" />
		<meta http-equiv="refresh" content="1800" />
		<title>The Microtransat Challenge</title>
		<link href="../css/new.css" rel="stylesheet" type="text/css" />

    <link rel="stylesheet" href="https://openlayers.org/en/v4.6.5/css/ol.css" type="text/css">
    <!-- The line below is only needed for old environments like Internet Explorer and Android 4.x -->
    <script src="https://cdn.polyfill.io/v2/polyfill.min.js?features=requestAnimationFrame,Element.prototype.classList,URL"></script>
    <script src="https://openlayers.org/en/v4.6.5/build/ol.js"></script>

	</head>
	<body style="margin:0px;">

    <div class="title_box">
        <div class="title_top"><div></div></div>
        <div class="rounded_content">
            <!--start header-->
            <div id="title_banner">
                <div id="title">
                    <h1>The Microtransat Challege</h1>
                </div>
                <p><br /></p>
            </div> <!--end of title banner-->
        </div> <!--end of first round corner div-->
    <div class="title_bottom"><div></div></div>
    </div>  <!--end of round corners-->


<?php
if ( !isset($_GET['fullscreen'])) {
?>

<!--start container-->

    <div style="height: 20px;"></div>

    <!--start mainbody-->
    <div class="content_box">
        <div class="content_top"><div></div></div>
        <div class="rounded_content">
   <div id="mainpane" style="min-height: 1000px;">
       <div id="mainbody">

            <h2>Live Maps</h2>

             <p>Each coloured circle on the map represents a position report. Click a coloured circle on the map to see time and date information for that position report. All data is delayed by 24 hours and times shown are in UTC. The thin blue lines are the start lines, the black lines are the finish lines.</p>
	     <p>If you are not seeing any tracks on the map try reloading the page, sometimes they don't appear. Alternatively you can download the map for viewing in <a href="get_latest_map.php">google earth</a>.</p>

<?php
}
else {

}
?>

<?php
	include $_SERVER['DOCUMENT_ROOT'].'/tracking/colours.php';
        include $_SERVER['DOCUMENT_ROOT'].'/tracking/includes/config.php';

	$conn = db_init();
	//$db = mysqli_select($conn,$db) or die("Could not select database");

        $sql = "SELECT BoatID,BoatName FROM Boat where startTime>\"2018-06-01\" order by startTime asc;";
	$result = mysqli_query($conn,$sql);
	$count=0;
	while($row = mysqli_fetch_assoc($result))
	{
		$boats[$count]["id"]=$row["BoatID"];
		$boats[$count]["name"]=$row["BoatName"];
		$count++;
	}

	
	$newest_time = date("Y-m-d H:i:s",time()-(3600*24));

	if(count($boats)>0)
	{
        echo "<table border=\"1\" style=\"border-style: thin;\">\n";
        echo "<tr>\n";
        echo "  <th>Boat</th><th>Team</th><th>Direction</th><th>Status</th><th>Latitude</th><th>Longitude</th><th>Last Update Time</th><th>Time Sailing</th><th>End Time/Status</th>\n";
        echo "</tr>\n";


        foreach ($boats as $boat_id)
        {
            $sql = "SELECT TeamID,startTime,BoatURL,endTime,endReason,Direction FROM Boat where BoatID=".$boat_id["id"].";";
            $result = mysqli_query($conn,$sql);
            $row = mysqli_fetch_assoc($result);
                    $direction = $row['Direction'];
            $teamid=$row['TeamID'];
            $boaturl=$row['BoatURL'];
            $start_time=$row['startTime'];
            $endReason=$row['endReason'];
            $endTime=$row['endTime'];

                    if ($start_time==NULL)
                    {
                            continue;
                    }



                $sql = "SELECT TeamName,TeamImage,TeamNationality FROM Teams where TeamID=".$teamid.";";
                    $result = mysqli_query($conn,$sql);
                    $row = mysqli_fetch_assoc($result);              
                    $team_name=$row['TeamName'];
            $team_image=$row['TeamImage'];
            $team_nationality=$row['TeamNationality'];

            if($start_time!=NULL)
            {
                if($endTime==NULL)
                {
                    $sql = "SELECT BoatID,GPSLat,GPSLong,PositionDate FROM  Position where BoatID=".$boat_id["id"]." and PositionDate<'".$newest_time."' and PositionDate>'".$start_time."' order by PositionDate desc limit 1;";
                }
                else
                {
                    $sql = "SELECT BoatID,GPSLat,GPSLong,PositionDate FROM  Position where BoatID=".$boat_id["id"]." and PositionDate<'".$newest_time."' and PositionDate>'".$start_time."' and PositionDate<'".$endTime."' order by PositionDate desc limit 1;";
                }
                $result = mysqli_query($conn,$sql);
                $row = mysqli_fetch_assoc($result);
                echo "<tr  style=\"background-color: #".$colours_html[$boat_id["id"]]."\">\n";
                echo "	<td><a href=\"/".$boaturl."\">".$boat_id['name']."</a></td>\n";
                echo " <!--".$boat_id['name']." colour = ".$colours_html[$boat_id["id"]]." id = ".$boat_id["id"]."-->";
                echo "  <td>".$team_name."</td>\n";
                    echo "  <td>".$direction."</td>\n";	
                            echo "  <td>Started: ".$start_time."</td>\n";
                echo "	<td>".substr($row['GPSLat'],0,6)."</td>\n";
                echo "	<td>".substr($row['GPSLong'],0,7)."</td>\n";
                echo "	<td>".$row['PositionDate']."</td>\n";
                echo "  <td>".seconds_to_nice_time(strtotime($row['PositionDate'])-strtotime($start_time))."</td>\n";
                echo "	<td>".$endReason." ".$endTime."</td>\n";
                echo "</tr>\n";
            }
            else
            {
                echo "<tr  style=\"background-color: #".$colours_html[$boat_id["id"]]."\">\n";
                            echo "  <td><a href=\"tracking.php?boat_id=".$boat_id['id']."\">".$boat_id['name']."</a></td>\n";
                            echo "  <td>".$team_name."</td>\n";
                            echo "  <td>Did not start</td>\n";
                            echo "</tr>\n";
            }
        }
        
        echo "</table>\n";
    }//end of if statement to check if we have any boats
    else
    {
        echo "<h2>No competitors to show</h2>";
        echo "<p>No teams have started the competition yet!</p>";
    }
?>
<br/>

<?php
if (isset($_GET['fullscreen'])) {
?>  <div id="map" class="map" style="height: 80%;" tabindex="0"></div>
<?php
}
else
{
?>
    <p style="text-align: right"><a href="/tracking/index.php?fullscreen">Fullscreen</a></p>
   <div id="map" class="map" style="width: 700px;" tabindex="0"></div>
<?php
}
?>
    <script>

      var vector = new ol.layer.Vector({
        source: new ol.source.Vector({
          url: 'get_latest_map.php',
          format: new ol.format.KML()
        })
      });


      var map = new ol.Map({
        layers: [
          new ol.layer.Tile({
            source: new ol.source.OSM()
          }), vector
        ],
        target: 'map',
        controls: ol.control.defaults({
          attributionOptions: {
            collapsible: false
          }
        }),
        view: new ol.View({
          center: ol.proj.fromLonLat([-25, 50]),
<?php
if (isset($_GET['fullscreen'])) {
  echo "zoom: 4";
}
else
{
   echo "zoom: 3";
}
?>
        })
      });

    </script>
</div>
