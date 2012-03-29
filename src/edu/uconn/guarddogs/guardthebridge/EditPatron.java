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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;


public class EditPatron extends Activity {
	private final String TAG = "EP-GTBLOG";
	private GtBDbAdapter mGDbHelper;
	private Long mpid;
	private EditPatron self;
	private PatronInfo m_aPI;
	private String mStatus;
	private String mDialogMsg = "";
	private ProgressDialog mProgBar;
	private CarsGtBDbAdapter mCDbHelper = null;
	
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
			Spinner aStatSpinn = (Spinner)findViewById(R.id.editpatron_setstatus);
			aStatSpinn.setSelection(getStatusPositionID(mStatus));
			aStatSpinn.invalidate();
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
					setStatus(((Spinner)findViewById(R.id.editpatron_setstatus)).getSelectedItem().toString()).
					build();
			
			long nRetval = mGDbHelper.setStatus(0, 
					aPI.toByteArray(), 
					aPI.getPid(), 
					mStatus);
			Log.v(TAG, "setStatus returned " + nRetval);
			Log.v(TAG, "updatePatron returned " 
					+ mGDbHelper.updatePatron(aPI.toByteArray(), 
							aPI.getPid(), 
							getStatusOpenness(mStatus)));
			mGDbHelper.close();
			new UpdtTask().execute();
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
	
	private int getStatusPositionID(String sStat)
	{
		/* Determined by position in/order of in 
		 * stringarray in res/values/status.xml 
		 */
		String sLowerStat = sStat.toLowerCase();
		
		if (sLowerStat.compareTo("waiting") == 0)
			return 0;
		if (sLowerStat.compareTo("riding") == 0)
			return 1;
		if (sLowerStat.compareTo("Done") == 0)
			return 2;
		if (sLowerStat.compareTo("Cancelled") == 0)
			return 3;
		return 0;
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
	
	private int getStatusOpenness(String isStatus)
	{
		String sStatusLower = isStatus.toLowerCase();
		if (sStatusLower.compareTo("waiting") == 0 || sStatusLower.compareTo("riding") == 0)
			return 0;
		else /*if (sStatusLower == "done" || sStatusLower == "cancelled") */
			return 1;
		
	}

   /*
    * Update ride assigned to this van
    */
   private class UpdtTask extends AsyncTask<Void, Integer, Integer>
   {
	   static final int INCREMENT_PROGRESS = 20;
	   protected void onPreExecute()
	   {
		   mProgBar = new ProgressDialog(self);
		   mProgBar.setCancelable(true);
		   mProgBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		   mProgBar.setMessage("Establishing Connection with server...");
		   mProgBar.show();
	   }

	   @Override
	   protected Integer doInBackground(Void... params)
	   {
		   return updateRide();
	   }	 
	   
	   protected void onProgressUpdate(Integer... progress)
	   {
		   int nTotalProgress = mProgBar.getProgress() + progress[0];
		   switch (nTotalProgress)
		   {
		   case 0:
		   case 20:
			   mProgBar.setMessage("Establishing Connection with server...");
			   break;
		   case 40:
			   mProgBar.setMessage("Connection Established, Sending request...");
			   break;
		   case 60:
			   mProgBar.setMessage("Updating ride...");
			   break;
		   case 80:
			   mProgBar.setMessage("Reading response...");
			   break;
		   case 100:
			   mProgBar.setMessage("Done!");
			   break;
		   }		
		   mProgBar.setProgress(nTotalProgress);
	   }
	   
	   protected void onPostExecute(Integer res)
	   {
		   
		  publishProgress(INCREMENT_PROGRESS);
		  mProgBar.dismiss();
		  
	   }
	   
	   public int updateRide()
	   {
		   GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper(self);
		   mGDbHelper.open();
		   mCDbHelper = new CarsGtBDbAdapter(self);
		   mCDbHelper.open();
		   //ArrayList<Integer> vRides = mGDbHelper.fetchAllPid();  // We only want the server to send us new rides, so we send the set of pids we already have
                   PatronInfo pbPat = mGDbHelper.fetchPatron(mpid);  // Get current patron info from DB 
		   mGDbHelper.close();
		   PatronList apbPL = PatronList.newBuilder().
		                    addPatron(pbPat).
				    build();
		   Request aPBReq = Request.newBuilder().
				   setNReqId(4).
				   setSReqType("UPDT").
				   setNCarId(mCDbHelper.getCar()).
		   		   setPlPatronList(apbPL).
		   		   build();
		   mCDbHelper.close();
		   publishProgress(INCREMENT_PROGRESS);  // <---- start from here
		   Log.v(TAG, "Request type: " + aPBReq.getSReqType());
		   Log.v(TAG, "Request ID: " + aPBReq.getNReqId());
		   Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
		   Log.v(TAG, "SReqType = " + aPBReq.getSReqType() + " " + 
				   aPBReq.getSerializedSize());
                   /* Make sure the connection is established and valid */
		   SSLSocket aSock = aSSLSF.createSSLSocket(self);
		   if (aSSLSF.getSession() == null)
		   {
			   aSSLSF = aSSLSF.getNewSSLSFW(self);
			   aSock = aSSLSF.getSSLSocket();
		   }
		   publishProgress(INCREMENT_PROGRESS);
		   try {
			   OutputStream aOS = aSock.getOutputStream();
			   try
			   {
				   aOS.write(aPBReq.getSerializedSize());
			   } catch (SSLProtocolException ex)
			   {
				   Log.e(TAG, "SSLProtoclException Caught. On-write to Output Stream");
					aSSLSF.forceReHandshake(self);
					aSock = aSSLSF.getSSLSocket();
					aOS = aSock.getOutputStream();
					try
					{
						aOS.write(aPBReq.getSerializedSize());
					} catch (SSLProtocolException exc)
					{
						aSSLSF = aSSLSF.getNewSSLSFW(self);
						aSock = aSSLSF.getSSLSocket();
						aOS = aSock.getOutputStream();
						aOS.write(aPBReq.getSerializedSize());
					}
			   }
			   byte[] vbuf = aPBReq.toByteArray();
			   aOS.write(vbuf);  // Send
			   publishProgress(INCREMENT_PROGRESS);
			   InputStream aIS = aSock.getInputStream();
			   vbuf = new byte[9];
			   aIS.read(vbuf);  // Receive
			   /* Handle messages smaller than 9 bytes; Bufs aren't terminated, so removes trailing 0s */
			   int nsize = (vbuf.length - 1);
			   for (; nsize>0; nsize--)
			   {
				   if(vbuf[nsize] == 0)
				   {
					   continue;
				   }
				   break;
			   }
			   byte[] vbuf2 = new byte[nsize + 1];  // Copy the received buf into an array of the correct size so parsing is successful
			   for(int i = 0; i != nsize + 1; i++)
				   vbuf2[i] = vbuf[i];
			   vbuf = vbuf2;
			   Response apbRes = null;
			   try 
			   {
				   Response apbTmpSize = null;
				   apbTmpSize = Response.parseFrom(vbuf);
				   vbuf = new byte[apbTmpSize.getNRespId()];
				   aIS.read(vbuf);
				   apbRes = Response.parseFrom(vbuf);  // Still deciding what will be contained in the response
				   publishProgress(INCREMENT_PROGRESS);
				   
				   Log.v(TAG, "Response Buffer:");
				   Log.v(TAG, TextFormat.shortDebugString(apbRes));
				   Log.v(TAG, "PatronList Buffer: ");
				   Log.v(TAG, TextFormat.shortDebugString(
						   apbRes.getPlPatronList()));
                                   /* I'd rather have the dispatchers handle merge conflicts
				    * because it would be easier, but what should this return
				    * on error/conflict to the client?
				    *
				    * Maybe we'll just have to return a message generated
				    * by the server, on both failure and success. On merge
				    * conflict, flag is set and message asks if he/she would
				    * like to resolve the conflict
				    */
				   if ( apbRes.getNResAddCount() == 1 )
				   {
				     if ( apbRes.getNResAdd(0) == mpid )
				     {
				    	 mDialogMsg = apbRes.getSResAdd(0);
				    	 self.runOnUiThread( new Runnable () {
				    		 public void run() {
				    			 String sErrorMessage = mDialogMsg;
				    			 AlertDialog.Builder builder = new AlertDialog.Builder(self);
				    			 builder.setMessage("Save returned error message:\n" + sErrorMessage + "\nCan/Would you like to resolve this?");
				    			 builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				    				 public void onClick(DialogInterface dialog, int id) {
				    					 dialog.dismiss();
				    					 new AlertDialog.Builder(self).
						    			 setMessage("Sorry, we haven't gotten this far yet.\nHopefully you can resolve the problem :-/ ").
						    			 setPositiveButton("Fiiiiiine", new DialogInterface.OnClickListener() {
						    				 public void onClick(DialogInterface dialog, int id) {
						    					 dialog.dismiss();
						    					 
						    				 }
						    			 }).
						    			 show();
				    				 }
				    			 });
				    			 builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										return;  // Do nothing
									}
								});
				    			 builder.show();
				    			 
				    		 }
				    	 });
				     }
				   }
				   //addToDb(apbRes.getPlPatronList());  
				   //Log.v(TAG, "Added to DB");
				   
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
					String tmp = "";
					for(int i = 0; i<vbuf.length; i++)
						tmp = tmp + vbuf[i] + " ";
					Log.w(TAG, "Buffer Received: " + vbuf.length + " bytes : " 
						+ tmp);
					e.printStackTrace();
				}
		   }catch (IOException e)
		   {
			   e.printStackTrace();
		   }
		   return 0;
	   }
   }
}
