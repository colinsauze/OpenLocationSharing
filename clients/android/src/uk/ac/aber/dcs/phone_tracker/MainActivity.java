package uk.ac.aber.dcs.phone_tracker;

//Imports

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements LocationListener,
		SensorEventListener, GpsStatus.Listener,
		CompoundButton.OnCheckedChangeListener {

	// Variables

	boolean Append = false;
	EditText etlat;
	EditText etlon;
	EditText etbear;
	EditText etAcc;
	EditText tex;
	ToggleButton tb;
	Button optionsBtn;
	LocationManager gps;
	ProgressDialog pd;
	SensorManager m_sensorManager;
	GpsStatus.Listener listener;
	GpsStatus status;
	Lock isLoggingLock;
	File f;
	FileOutputStream fos;
	PrintStream ps;
	List<String> EmailRetryList = new Vector<String>();
	List<String> HTTPRetryList = new Vector<String>();
	List<GpsSatellite> Sat = new Vector<GpsSatellite>();
	ConfigActivity ca = new ConfigActivity();
	Intent configActivity;
	Camera mCamera;
	CameraPreview mPreview;

	double lat;
	double lon;
	float Acc;
	private float[] gravity = new float[3];
	private float[] geomag = new float[3];
	private int heading;
	int Heading;
	int FixCount;
	int satSize;
	boolean isLogging = false;
	String fileName = "GPS_output_" + getCurrentTimeDate() + ".kml";
	String filePath;
	String pictureName = "IMG_" + getCurrentTimeDate() + ".jpg";
	String picturePath;
	int numberOfResults = 0;
	private static boolean email;
	double fileLat;
	double fileLon;
	String fileTime;
	String fileDate;
	static int sendFrequency;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	boolean pictureCreated = false;
	String currentFileName;
	String currentPictureName;
	long lastPictureTime = 0;

	// Getters and Setters

	public int getHeading() {
		return heading;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public float getAcc() {
		return Acc;
	}

	public void setAcc(float acc) {
		Acc = acc;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public static boolean isEmail() {
		return email;
	}

	public static void setEmail(boolean email) {
		MainActivity.email = email;
	}

	public int getSendFrequency() {
		return sendFrequency;
	}

	public static void setSendFrequency(int sendFrequency) {
		MainActivity.sendFrequency = sendFrequency;
	}

	// Overridden

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mCamera = getCameraInstance();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		configActivity = new Intent(getApplicationContext(),
				ConfigActivity.class);

		etlat = (EditText) findViewById(R.id.lat);
		etlon = (EditText) findViewById(R.id.lon);
		etbear = (EditText) findViewById(R.id.Bearing);
		etAcc = (EditText) findViewById(R.id.Acc);
		tex = (EditText) findViewById(R.id.editText1);
		tb = (ToggleButton) findViewById(R.id.Start_Stop);
		optionsBtn = (Button) findViewById(R.id.options);
		tb.setOnCheckedChangeListener(this);
		optionsBtn.setOnClickListener(ocl);

		m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		setUpGPS();

		isLoggingLock = new ReentrantLock();
	}

	View.OnClickListener ocl = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			configActivity.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivity(configActivity);
		}
	};

	@Override
	public void onLocationChanged(Location location) {

		lat = location.getLatitude();
		lon = location.getLongitude();
		Acc = location.getAccuracy();
		etlat.setText("" + lat);
		etlon.setText("" + lon);
		etbear.setText("" + getHeading());
		etAcc.setText("" + Acc + "m");

		isLoggingLock.lock();
		if (true == isLogging) {
			tex.setText("");
			ps.print("<Placemark>");
			ps.println();
			ps.print("<description>Accuracy: " + Acc + "m");
			ps.println();
			ps.print("Time: " + getCurrentTime() + "</description>");
			ps.println();
			ps.print("<styleUrl>#arrow_" + Heading + "</styleUrl>");
			ps.println();
			ps.print("<Point>");
			ps.println();
			ps.print("<coordinates>" + lon + "," + lat + "</coordinates>");
			ps.println();
			ps.print("</Point>");
			ps.println();
			ps.print("</Placemark>");
			numberOfResults++;
			tex.setText(""
					+ ((System.currentTimeMillis() - lastPictureTime) / 1000));

			if (System.currentTimeMillis() > lastPictureTime
					+ ((60000) * sendFrequency)) {
				if (email == true) {
					mCamera.takePicture(null, null, mPicture);
					createNewFile();
					createHeadings();
					if (EmailRetryList.size() > 0) {
						retryResults();
					}
				}
				if (email == false) {
					mCamera.takePicture(null, null, mPicture);
					createNewFile();
					if (HTTPRetryList.size() > 0) {
						retryPostData();
					}
				}
				numberOfResults = 0;
				lastPictureTime = System.currentTimeMillis();
			}
		}
		isLoggingLock.unlock();
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@SuppressWarnings("unused")
	@Override
	public void onSensorChanged(SensorEvent event) {

		float[] inR = new float[16];
		float[] orientVals = new float[3];
		float[] I = new float[16];
		float[] apr = new float[3];

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			geomag = event.values.clone();
		} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			gravity = event.values.clone();
		}

		if (gravity != null && geomag != null) {
			boolean success = SensorManager.getRotationMatrix(inR, I, gravity,
					geomag);
			if (success) {
				SensorManager.getOrientation(inR, orientVals);
				Heading = (((int) Math.toDegrees((double) orientVals[0])));
				if (Heading < 0) {
					Heading = Heading + 360;
				}
				setHeading(Heading);
				etbear.setText("" + getHeading());

			}
		}

	}

	@Override
	public void onGpsStatusChanged(int event) {
		/*
		 * if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) { GpsStatus status
		 * = gps.getGpsStatus(null);
		 * 
		 * Sat = (Vector<GpsSatellite>) status.getSatellites(); for (int i = 0;
		 * i < Sat.size(); i++) { if (Sat.get(i).usedInFix()) { FixCount++; } }
		 * } etSat.setText(FixCount + "/" + Sat.size());
		 */
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked == true) {

			createNewFile();

			ps.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			ps.println();
			ps.print("<kml xmlns=\"http://www.opengis.net/kml/2.2\"");
			ps.println();
			ps.print("xmlns:gx=\"http://www.google.com/kml/ext/2.2\"");
			ps.println();
			ps.print("xmlns:atom=\"http://www.w3.org/2005/Atom\">");
			ps.println();
			ps.print("<Document>");
			ps.println();

			pd = new ProgressDialog(tb.getContext());
			pd.setCancelable(false);
			pd.setMessage("Creating file please wait...");
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setProgress(0);
			pd.setMax(359);
			pd.show();

			Thread background = new Thread(new Runnable() {
				public void run() {

					pd.incrementProgressBy(5);

					for (int i = 0; i < 360; i++) {

						ps.print("<Style id=\"arrow_" + i + "\">");
						ps.println();
						ps.print("<IconStyle>");
						ps.println();
						ps.print("<color>ff00d1ff</color>");
						ps.println();
						ps.print("<scale>1.0</scale>");
						ps.println();
						ps.print("<heading>");
						int heading = i + 180;
						if (heading >= 360) {
							heading = heading - 360;
						}
						ps.print(heading + "</heading>");
						ps.println();
						ps.print("<Icon>");
						ps.println();
						ps.print("<href>http://maps.google.com/mapfiles/kml/shapes/arrow.png</href>");
						ps.println();
						ps.print("</Icon>");
						ps.println();
						ps.print("<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>");
						ps.println();
						ps.print("</IconStyle>");
						ps.println();
						ps.print("</Style>");
						ps.println();

						pd.setProgress(i);

						if (pd.getProgress() == 359) {
							pd.dismiss();
							isLoggingLock.lock();
							isLogging = true;
							isLoggingLock.unlock();
						}

					}
				}
			});
			background.start();
		} else {
			ps.print("</Document>");
			ps.println();
			ps.print("</kml>");
			ps.close();

			if (sendFrequency != 0) {
				if (email == true) {
					sendResults();
				} else {
					postData();
				}
			}
		}
	}

	// Methods

	public void setUpGPS() {

		String gpsProvider;

		m_sensorManager.registerListener(this,
				m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);
		m_sensorManager.registerListener(this,
				m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		m_sensorManager.registerListener(this,
				m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		gps = (LocationManager) this.getBaseContext().getSystemService(
				Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		gpsProvider = gps.getBestProvider(criteria, true);
		gps.requestLocationUpdates(gpsProvider, 10000, 0, this);
		Location location = gps.getLastKnownLocation(gpsProvider);
		gps.addGpsStatusListener(listener);
		if (location != null) {
			lat = location.getLatitude();
			lon = location.getLongitude();
		}
	}

	public String getCurrentTimeDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH,mm,ss");
		Calendar now = Calendar.getInstance();
		String timeDateNow = sdf.format(now.getTime());
		return timeDateNow;
	}

	public String getCurrentDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar now = Calendar.getInstance();
		String dateNow = sdf.format(now.getTime());
		return dateNow;
	}

	public String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH,mm,ss");
		Calendar now = Calendar.getInstance();
		String timeNow = sdf.format(now.getTime());
		return timeNow;
	}

	public void sendCurrentLocation() {
		Mail m = new Mail(ca.getSendersEmail(), ca.getSendersPass());

		try {
			String[] toArr = { ca.getReceiversEmail() };
			m.set_to(toArr);
			m.set_from(ca.getSendersEmail());
			m.set_subject("Current Location");
			m.set_body("Latitude: " + lat + " " + "Longitude: " + lon);
			if (m.send()) {
				tex.setText("Email sent");
			} else {
				tex.setText("Email not sent");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendResults() {
		Mail m = new Mail(ca.getSendersEmail(), ca.getSendersPass());

		String[] toArr = { ca.getReceiversEmail() };
		m.set_to(toArr);
		m.set_from(ca.getSendersEmail());
		m.set_subject("Results");
		m.set_body("The results are attached.");

		try {
			m.addAttachment(filePath + "/" + currentFileName);
			m.addAttachment(picturePath + "/" + currentPictureName);

			if (m.send()) {
				Toast.makeText(MainActivity.this,
						"Email was sent successfully.", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(MainActivity.this, "Email was not sent.",
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			Log.e("MailApp", "Could not send email", e);
		}
	}

	public void retryResults() {
		Mail m = new Mail(ca.getSendersEmail(), ca.getSendersPass());

		try {
			String[] toArr = { ca.getReceiversEmail() };
			m.set_to(toArr);
			m.set_from(ca.getSendersEmail());
			m.set_subject("Delayed Results/Pictures");
			m.set_body("The results and pictures that are attached could not be sent on the time of creation.");
			for (int i = 0; i < EmailRetryList.size(); i++) {
				m.addAttachment(EmailRetryList.get(i));
				if (m.send()) {
					tex.setText("Email has been sent");
					EmailRetryList.remove(i);
				} else {
					tex.setText("Email has not been sent");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createNewFile() {
		File extStorageDirectory = Environment.getExternalStorageDirectory();
		File dir = new File(extStorageDirectory.getAbsolutePath()
				+ "/GPS_Tracker/GPS_Data");
		currentFileName = "GPS_output_" + getCurrentTimeDate() + ".kml";

		try {
			f = new File(dir, currentFileName);
			filePath = dir.getAbsolutePath();
			dir.mkdirs();
			ps = new PrintStream(new FileOutputStream(f, Append));
		} catch (Exception E) {
			E.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public void postData() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(
				"http://users.aber.ac.uk/cos/map/store_data.php");

		String FileName = f.getName();

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("boat_name", fileName));
			nameValuePairs
					.add(new BasicNameValuePair("time", getCurrentTime()));
			nameValuePairs
					.add(new BasicNameValuePair("date", getCurrentDate()));
			nameValuePairs.add(new BasicNameValuePair("lat", "" + lat));
			nameValuePairs.add(new BasicNameValuePair("lon", "" + lon));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);

		} catch (IOException e) {
			e.printStackTrace();
			fileLat = lat;
			fileLon = lon;
			fileTime = getCurrentTime();
			fileDate = getCurrentDate();
			int noRetry = 0;
			HTTPRetryList.add(fileName + "," + fileTime + "," + fileDate + ","
					+ fileLat + "," + fileLon + "," + noRetry);
		}
	}

	@SuppressWarnings("unused")
	public void retryPostData() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(
				"http://users.aber.ac.uk/cos/map/store_data.php");

		for (int i = 0; i < HTTPRetryList.size(); i++) {
			String[] fileRetry = (HTTPRetryList.get(i)).split(",");
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("boat_name",
						fileRetry[0]));
				nameValuePairs
						.add(new BasicNameValuePair("time", fileRetry[1]));
				nameValuePairs
						.add(new BasicNameValuePair("date", fileRetry[2]));
				nameValuePairs.add(new BasicNameValuePair("lat", fileRetry[3]));
				nameValuePairs.add(new BasicNameValuePair("lon", fileRetry[4]));

				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse response = httpclient.execute(httppost);

				HTTPRetryList.remove(i);

			} catch (IOException e) {
				HTTPRetryList.remove(i);
				int noRetry = (Integer.parseInt(fileRetry[5])) + 1;
				HTTPRetryList.add(fileRetry[0] + "," + fileRetry[1] + ","
						+ fileRetry[2] + "," + fileRetry[3] + ","
						+ fileRetry[4] + "," + noRetry);
			}
		}
	}

	public void createHeadings() {
		for (int i = 0; i < 359; i++) {
			ps.print("<Style id=\"arrow_" + i + "\">");
			ps.println();
			ps.print("<IconStyle>");
			ps.println();
			ps.print("<color>ff00d1ff</color>");
			ps.println();
			ps.print("<scale>1.0</scale>");
			ps.println();
			ps.print("<heading>");
			int heading = i + 180;
			if (heading >= 360) {
				heading = heading - 360;
			}
			ps.print(heading + "</heading>");
			ps.println();
			ps.print("<Icon>");
			ps.println();
			ps.print("<href>http://maps.google.com/mapfiles/kml/shapes/arrow.png</href>");
			ps.println();
			ps.print("</Icon>");
			ps.println();
			ps.print("<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>");
			ps.println();
			ps.print("</IconStyle>");
			ps.println();
			ps.print("</Style>");
			ps.println();
		}
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.v("MainActivity", "Taking Picture");
			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if (pictureFile == null) {
				Log.d("MainActivity",
						"Error creating media file, check storage permissions: ");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d("MainActivity", "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d("MainActivity", "Error accessing file: " + e.getMessage());
			}
			pictureCreated = true;
			Log.v("MainActivity", "pictureCreated = true");

			/*
			 * (if (email == true) { sendResults(); } else { postData(); }
			 */
		}
	};

	private File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File extStorageDirectory = Environment.getExternalStorageDirectory();
		File dir = new File(extStorageDirectory.getAbsolutePath()
				+ "/GPS_Tracker/GPS_Images");

		// Create a media file name
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(dir.getAbsolutePath() + File.separator
					+ pictureName);
			currentPictureName = pictureName;
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(dir.getPath() + File.separator + "VID_"
					+ getCurrentTimeDate() + ".mp4");
		} else {
			return null;
		}

		picturePath = dir.getAbsolutePath();

		return mediaFile;
	}
}