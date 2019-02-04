<?php

/*script to store number plate data:
we expect the following data to be posted to us:
tag_id - the unique ID of an RFID tag
date - the current date
time - the current time
lat - the latitude
lon - the longitude
distance - the distance from the centre of campus

the tag_id will be looked up in an associative array which will reveal the owner and registration plate

*/

$filename="tag_data";

//read number plate/tag association data into an array
$number_plates=array();

$fptr=fopen("tags","r");
while ( ($buf=fgets( $fptr, 8192 )) != '' ) {
	$pieces=explode(" ",$buf);
        //element 0 = RFID tag, 1 registration plate, 2 owner
	$number_plates[$pieces[0]]=array($pieces[1],trim($pieces[2]));
        //array($pieces[1],$pieces[2]);
}
fclose($fptr);

header("Content-Type: text/plain");

//check for file size overflows
if(is_file($filename))
{
    if(filesize($filename)<10000)
    {
        $proceed=true;
    }
    else
    {
        echo "Error: Datafile too big";
        return;
    }
}
//store the data in the tag_data file
if($proceed)
{
    $fptr=fopen($filename,'a');
    
    //look up registration plate
    $registration=$number_plates[$_POST['tag_id']][0];
    $owner=$number_plates[$_POST['tag_id']][1];
    
    if ($registration=="")
    {
	echo "Tag ".$_POST['tag_id']." not found in list";
	return;
    }
    
        
    $text=$registration.' '.$owner.' '.$_POST['time'].' '.$_POST['date'].' '.$_POST['lat'].' '.$_POST['lon'].' '.$_POST['distance'];
    fputs($fptr," ".$text."\n");
    fclose($fptr);
    echo "Data Saved";
}

?>
