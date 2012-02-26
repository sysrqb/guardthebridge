/*
 * GUARD the Bridge
 * Copyright (C) 2012  Matthew Finkel <Matthew.Finkel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package edu.uconn.guarddogs.guardthebridge;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;


public class EditPatron extends Activity {
	private final String TAG = "EP-GTBLOG";
	private GtBDbAdapter mGDbHelper;
	private Long mpid;
	private EditPatron self;
	private PatronInfo m_aPI;
	private String mStatus;
	
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		self = this;
		mGDbHelper = new GtBDbAdapter(this);
		mGDbHelper.open();
		setContentView(R.layout.editpatron);
		mpid = (savedInstanceState == null) ? null :
			(Long)savedInstanceState.getSerializable(GtBDbAdapter.KEY_ROWID);
		if (mpid == null)
		{
			Bundle bundle = getIntent().getExtras();
			mpid = (bundle != null) ? bundle.getLong(GtBDbAdapter.KEY_ROWID) : null;
		}
		fillPatronInfo();
		mGDbHelper.close();
		
		Button bCancel = (Button) findViewById(R.id.editpatron_cancel);
		bCancel.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				setResult(RESULT_OK);
				finish();
			}
		});
		
		Button bSave = (Button) findViewById(R.id.editpatron_save);
		bSave.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				self.savePatronInfo();
				setResult(RESULT_OK);
				finish();
			}
		});
		
		Spinner spStatus = (Spinner) findViewById(R.id.editpatron_setstatus);
		ArrayAdapter<CharSequence> aAdapter = ArrayAdapter.createFromResource
				(this, R.array.statusarray, android.R.layout.simple_spinner_item);
		aAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spStatus.setAdapter(aAdapter);
		
		Button bDone = (Button) findViewById(R.id.editpatron_done);
		bDone.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				self.donePatron();
				setResult(RESULT_OK);
				finish();
			}
		});
		
		Button bCanceled = (Button) findViewById(R.id.editpatron_canceled);
		bCanceled.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				self.cancelPatron();
				setResult(RESULT_OK);
				finish();
			}
		});
	}
	
	private void fillPatronInfo()
	{
		if (mpid != null)
		{
			m_aPI = mGDbHelper.fetchPatron(mpid);
			if (m_aPI == null)
				return;
			EditText evName = null;
			if(m_aPI.getName() != null){
				evName = (EditText)findViewById(R.id.editpatron_nameVal);
				evName.setText(" " + m_aPI.getName());
			}
			if(m_aPI.getPhone() != null){
				evName = (EditText)findViewById(R.id.editpatron_phoneVal);
				evName.setText(" " + m_aPI.getPhone());
			}
			if(m_aPI.getPassangers() > 0){
				evName = (EditText)findViewById(R.id.editpatron_passVal);
				evName.setText(" " + Integer.toString(m_aPI.getPassangers()));
			}
			if(m_aPI.getPickup() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_puVal);
				evName.setText(" " + m_aPI.getPickup());
			}
			if(m_aPI.getDropoff() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_doVal);
				evName.setText(" " + m_aPI.getDropoff());
			}
			if(m_aPI.getTimetaken() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_ttVal);
				evName.setText(" " + m_aPI.getTimetaken());
			}
			if(m_aPI.getStatus() != null)
			{
				mStatus = m_aPI.getStatus();
			}
			else
			{
				mStatus = "waiting";
			}
		}
	}
	
	private void savePatronInfo()
	{
		if (mpid != null)
		{
			mGDbHelper.open();
			if (m_aPI == null)
				m_aPI = mGDbHelper.fetchPatron(mpid);
			
			int npass = 0;
			try {
				npass = Integer.parseInt(((EditText)findViewById(R.id.editpatron_passVal)).getText().toString().trim()); //Puts space before number
			} catch (NumberFormatException e)
			{
				Log.w(TAG, "Passangers is not an int: " + ((EditText)findViewById(R.id.editpatron_passVal)).getText().toString() + " : " + npass);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("The Number of passagers you entered is invalid");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           	    return;
		           }
		        });
				builder.show();
			}
			PatronInfo aPI = PatronInfo.newBuilder(m_aPI).
					setName(((EditText)findViewById(R.id.editpatron_nameVal)).getText().toString()).
					setPhone(((EditText)findViewById(R.id.editpatron_phoneVal)).getText().toString()).
					setPassangers(npass).
					setPickup(((EditText)findViewById(R.id.editpatron_puVal)).getText().toString()).
					setDropoff(((EditText)findViewById(R.id.editpatron_doVal)).getText().toString()).
					setTimeassigned(((EditText)findViewById(R.id.editpatron_ttVal)).getText().toString()).
					setStatus(mStatus).
					build();
			
			mGDbHelper.setStatus(0, aPI.toByteArray(), aPI.getPid(), mStatus);
			mGDbHelper.close();
		}
	}
	
	private void donePatron()
	{

		if (mpid != null)
		{
			mGDbHelper.open();
			if (m_aPI == null)
				m_aPI = mGDbHelper.fetchPatron(mpid);
			int npass = 0;
			try {
				npass = Integer.parseInt(((EditText)findViewById(R.id.editpatron_passVal)).getText().toString().trim()); //Puts space before number
			} catch (NumberFormatException e)
			{
				Log.w(TAG, "Passangers is not an int: " + ((EditText)findViewById(R.id.editpatron_passVal)).getText().toString() + " : " + npass);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("The Number of passagers you entered is invalid");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           	    return;
		           }
		        });
				builder.show();
			}
			PatronInfo aPI = PatronInfo.newBuilder(m_aPI).
					setName(((EditText)findViewById(R.id.editpatron_nameVal)).getText().toString()).
					setPhone(((EditText)findViewById(R.id.editpatron_phoneVal)).getText().toString()).
					setPassangers(npass).
					setPickup(((EditText)findViewById(R.id.editpatron_puVal)).getText().toString()).
					setDropoff(((EditText)findViewById(R.id.editpatron_doVal)).getText().toString()).
					setTimeassigned(((EditText)findViewById(R.id.editpatron_ttVal)).getText().toString()).
					setStatus("done").
					build();
			
			Log.v(TAG, "Updating Patron as DONE: " + mpid);
			mGDbHelper.setDone(mpid + 1, aPI.toByteArray(), aPI.getPid());
			mGDbHelper.close();
		}
			
	}
	
	private void cancelPatron()
	{

		if (mpid != null)
		{
			mGDbHelper.open();
			if (m_aPI == null)
				m_aPI = mGDbHelper.fetchPatron(mpid);
			int npass = 0;
			try {
				npass = Integer.parseInt(((EditText)findViewById(R.id.editpatron_passVal)).getText().toString().trim()); //Puts space before number
			} catch (NumberFormatException e)
			{
				Log.w(TAG, "Passangers is not an int: " + ((EditText)findViewById(R.id.editpatron_passVal)).getText().toString() + " : " + npass);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("The Number of passagers you entered is invalid");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           	    return;
		           }
		        });
				builder.show();
			}
			PatronInfo aPI = PatronInfo.newBuilder(m_aPI).
					setName(((EditText)findViewById(R.id.editpatron_nameVal)).getText().toString()).
					setPhone(((EditText)findViewById(R.id.editpatron_phoneVal)).getText().toString()).
					setPassangers(npass).
					setPickup(((EditText)findViewById(R.id.editpatron_puVal)).getText().toString()).
					setDropoff(((EditText)findViewById(R.id.editpatron_doVal)).getText().toString()).
					setTimeassigned(((EditText)findViewById(R.id.editpatron_ttVal)).getText().toString()).
					setStatus("cancelled").
					build();
			
			Log.v(TAG, "Updating Patron as CANCELED: " + mpid);
			mGDbHelper.setCanceled(mpid + 1, aPI.toByteArray(), aPI.getPid());
			mGDbHelper.close();
		}
			
	}
	
	public class StatusOnItemSelectedListener 
			implements OnItemSelectedListener 
	{
		public void onItemSelected(
				AdapterView<?> parent, View view, int pos, long id)
		{
			PatronInfo aPI = null;
			mGDbHelper.open();
			switch (pos)
			{
			case 0:
				mStatus = "waiting";
				aPI = PatronInfo.newBuilder(m_aPI).
				setStatus("waiting").
				build();
				mGDbHelper.setStatus(0, aPI.toByteArray(), m_aPI.getPid(), "waiting");
				mGDbHelper.close();
				break;
			case 1:
				mStatus = "riding";
				aPI = PatronInfo.newBuilder(m_aPI).
				setStatus("riding").
				build();
				mGDbHelper.setStatus(0, m_aPI.toByteArray(), m_aPI.getPid(), "riding");
				mGDbHelper.close();
				break;
			case 2:
				mStatus = "done";
				aPI = PatronInfo.newBuilder(m_aPI).
				setStatus("done").
				build();
				mGDbHelper.setStatus(0, m_aPI.toByteArray(), m_aPI.getPid(), "done");
				mGDbHelper.close();
				break;
			case 3:
				mStatus = "cancelled";
				aPI = PatronInfo.newBuilder(m_aPI).
				setStatus("cancelled").
				build();
				mGDbHelper.setStatus(0, m_aPI.toByteArray(), m_aPI.getPid(), "cancelled");
				mGDbHelper.close();
				break;
			default:
				mGDbHelper.close();
			}
		}
		
		public void onNothingSelected(AdapterView<?> parent)
		{
			
		}
	}
}
