package uk.ac.aber.dcs.phone_tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigActivity extends Activity implements OnCheckedChangeListener {

	String sendersEmail;
	String sendersPass;
	String receiversEmail;
	int Progress;
	EditText etreceiversEmail;
	EditText etSendersPass;
	EditText etSendersEmail;
	TextView snd;
	EditText sendingFrequency;
	Button btn;
	RadioButton rbEmail;
	RadioButton rbHTTP;
	Intent mainActivity;
	int sndFreq;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config);

		etreceiversEmail = (EditText) findViewById(R.id.receiversEmail);
		etSendersPass = (EditText) findViewById(R.id.sendersPassword);
		etSendersEmail = (EditText) findViewById(R.id.sendersEmail);
		btn = (Button) findViewById(R.id.btn);
		sendingFrequency = (EditText) findViewById(R.id.sndFreq);
		rbEmail = (RadioButton) findViewById(R.id.useEmail);
		rbHTTP = (RadioButton) findViewById(R.id.useHTTP);

		btn.setOnClickListener(onClick);
		rbEmail.setOnCheckedChangeListener(this);

		sendersEmail = etSendersEmail.getText().toString();
		sendersPass = etSendersPass.getText().toString();
		receiversEmail = etreceiversEmail.getText().toString();

		final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			gpsDisabled();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		sendersEmail = etSendersEmail.getText().toString();
		sendersPass = etSendersPass.getText().toString();
		receiversEmail = etreceiversEmail.getText().toString();
	}

	View.OnClickListener onClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			sendersEmail = etSendersEmail.getText().toString();
			sendersPass = etSendersPass.getText().toString();
			receiversEmail = etreceiversEmail.getText().toString();

			sndFreq = Integer.parseInt(sendingFrequency.getText().toString());
			MainActivity.setSendFrequency(sndFreq);

			if (rbEmail.isChecked() == false && rbHTTP.isChecked() == false) {
				Context context = getApplicationContext();
				CharSequence text = "Enter the way you wish to send the data";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} else if (rbEmail.isChecked() == true
					&& (sendersEmail.matches("") || sendersPass.matches("") || receiversEmail
							.matches(""))) {
				Context context = getApplicationContext();
				CharSequence text = "Enter the required email information";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} else {
				mainActivity = new Intent(getApplicationContext(),
						MainActivity.class);
				mainActivity.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				sendersEmail = etSendersEmail.getText().toString();
				sendersPass = etSendersPass.getText().toString();
				receiversEmail = etreceiversEmail.getText().toString();
				startActivity(mainActivity);
			}
		}
	};

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			MainActivity.setEmail(true);
			etSendersEmail.setEnabled(true);
			etSendersPass.setEnabled(true);
			etreceiversEmail.setEnabled(true);
		} else if (!isChecked) {
			MainActivity.setEmail(false);
			etSendersEmail.setEnabled(false);
			etSendersEmail.setText("");
			etSendersPass.setEnabled(false);
			etSendersPass.setText("");
			etreceiversEmail.setEnabled(false);
			etreceiversEmail.setText("");
		}
	}

	private void gpsDisabled() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS is disabled, do you want to enable it?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								startActivity(new Intent(
										Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						finish();
						System.exit(0);
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	public String getSendersEmail() {
		return sendersEmail;
	}

	public void setSendersEmail(String sendersEmail) {
		this.sendersEmail = sendersEmail;
	}

	public String getSendersPass() {
		return sendersPass;
	}

	public void setSendersPass(String sendersPass) {
		this.sendersPass = sendersPass;
	}

	public String getReceiversEmail() {
		return receiversEmail;
	}

	public void setReceiversEmail(String receiversEmail) {
		this.receiversEmail = receiversEmail;
	}
}
