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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;

public class CarNumList extends ListActivity
{
	private static final String TAG = "CNL-GTBLOG";
	private CarsGtBDbAdapter m_aCDbHelper;
	private CarNumList self = this;
	private ProgressDialog mProgBar = null;
	private String exceptionalMessage = "";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		m_aCDbHelper = new CarsGtBDbAdapter(this);
        m_aCDbHelper.open();
		new CarsTask().execute();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if(mProgBar != null && mProgBar.isShowing())
			mProgBar.cancel();
		
		if(m_aCDbHelper != null && !m_aCDbHelper.isClosed())
			m_aCDbHelper.close();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		//Position starts at 0, so add 1
		int nNum = -1;
		String sCarNum = (String)l.getItemAtPosition(position);
		try
		{
			String aNum = sCarNum.substring(4);
			nNum = Integer.parseInt(aNum);
			Log.v(TAG, "Index: " + m_aCDbHelper.setCar(nNum));
			Log.v(TAG, "Car Number: " + nNum);
		} catch (NumberFormatException e)
		{
			Log.w(TAG, "Could not parse car number. Assuming we didn't" +
					"receive an valid range from the server.");
			m_aCDbHelper.setCar(-1);
		}
		m_aCDbHelper.close();
		setResult(RESULT_OK);
		finish();
	}
	
	private class CarsTask extends AsyncTask<Void, Integer, Integer>
	{
		private SSLSocket aSock = null;
		
		protected void onPreExecute()
		{
			mProgBar = new ProgressDialog(self);
			mProgBar.setCancelable(true);
			mProgBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgBar.setMessage("Establishing Connection with server...");
			mProgBar.show();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			int numOfCars = numberOfCars();
			if(isCancelled())
				Log.v(TAG, "doInBackground found request was cancelled");
			return numOfCars;
		}
		
		public int numberOfCars()
		{
			Request aPBReq = null;
			Response aPBRes = null;
			GtBSSLSocketFactoryWrapper aSSLSF = null;
			try
			{
				aSSLSF = new GtBSSLSocketFactoryWrapper(self);
			} catch (UnrecoverableKeyException e1)
			{
				exceptionalMessage = "We ran into an unrecoverable key" +
						" exception. Please notify the IT Officer. Sorry.";
				cancel(true);
			} catch (KeyStoreException e1)
			{
				exceptionalMessage = "We couldn't find or open the KeyStore." +
						"This is manditory to use this app so please notify " +
						"the IT Officer. Sorry.";
				cancel(true);
			} catch (NoSuchAlgorithmException e1)
			{
				exceptionalMessage = "This tablet doesn't support an " +
						"algorithm we need to use. Please notify the " +
						"IT Officer so it can be updated. Sorry.";
				cancel(true);
			} catch (SignalException e1)
			{
				exceptionalMessage = "We appear to have low signal strength." +
						" We can't connect right now, sorry.";
				cancel(true);
			} catch (GTBSSLSocketException e1)
			{
				exceptionalMessage = e1.getMessage();
				cancel(true);
			}
			/* Wait until we cancel */
			while(isCancelled());
			
			final int INCREMENT_PROGRESS = 20;
			Log.v(TAG, "Getting Car");

			publishProgress(INCREMENT_PROGRESS);
			try
			{
				aSock = aSSLSF.getSSLSocket();
			} catch (GTBSSLSocketException e)
			{
				exceptionalMessage = "We could not connect to the server! :(" +
						" Do we currently have 3G service?";
				cancel(true);
			}
			while(isCancelled());

			if(aSock.isClosed())
			{
				Log.w(TAG, "Socket IS closed!");
				try
				{
					aSock = aSSLSF.createSSLSocket(self);
				} catch (UnrecoverableKeyException e1)
				{
					exceptionalMessage =
							"We ran into an unrecoverable key exception." +
							" Please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (KeyStoreException e1)
				{
					exceptionalMessage =
							"We couldn't find or open the KeyStore." +
							"This is manditory to use this app so" +
							" please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (NoSuchAlgorithmException e1)
				{
					exceptionalMessage =
							"This tablet doesn't support an algorithm we" +
							" need to use. Please notify the " +
							"IT Officer so it can be updated. Sorry.";
					cancel(true);
				} catch (SignalException e1)
				{
					exceptionalMessage =
							"We appear to have low signal strength. " +
							"We can't connect right now, sorry.";
					cancel(true);
				} catch (GTBSSLSocketException e1)
				{
					exceptionalMessage = e1.getMessage();
					cancel(true);
				}
			}

			/* Wait until we cancel */
			while(isCancelled());

			if (aSock.isOutputShutdown())
			{
				Log.w(TAG, "We just opened the socket but Output Stream" +
						" is Shutdown!");
				try
				{
					aSock.close();
					aSock = aSSLSF.createSSLSocket(self);
				} catch (UnrecoverableKeyException e1)
				{
					exceptionalMessage =
							"We ran into an unrecoverable key exception." +
							" Please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (KeyStoreException e1)
				{
					exceptionalMessage =
							"We couldn't find or open the KeyStore." +
							"This is manditory to use this app so" +
							" please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (NoSuchAlgorithmException e1)
				{
					exceptionalMessage =
							"This tablet doesn't support an algorithm we" +
							" need to use. Please notify the " +
							"IT Officer so it can be updated. Sorry.";
					cancel(true);
				} catch (SignalException e1)
				{
					exceptionalMessage =
							"We appear to have low signal strength. " +
							"We can't connect right now, sorry.";
					cancel(true);
				} catch (GTBSSLSocketException e)
				{
					exceptionalMessage =
							"We could not connect to the server! :(" +
							" Do we currently have 3G service?";
					cancel(true);
				} catch (IOException e)
				{
					exceptionalMessage =
							"We could not connect to the server! :(" +
							" Do we currently have 3G service?";
					cancel(true);
				}
				/* Wait until we cancel */
				while(isCancelled());
			}
			
			if (aSSLSF.getSession() != null)
				Log.v(TAG, "Session is still valid");
			else
			{
				Log.w(TAG, "Session is NO LONGER VALID");
				try
				{
					aSSLSF = new GtBSSLSocketFactoryWrapper(self);
				} catch (UnrecoverableKeyException e1)
				{
					exceptionalMessage =
							"We ran into an unrecoverable key" +
						" exception. Please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (KeyStoreException e1)
				{
					exceptionalMessage =
							"We couldn't find or open the KeyStore." +
							"This is manditory to use this app so" +
							" please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (NoSuchAlgorithmException e1)
				{
					exceptionalMessage = "This tablet doesn't support an " +
							"algorithm we need to use. Please notify the " +
							"IT Officer so it can be updated. Sorry.";
					cancel(true);
				} catch (SignalException e1)
				{
					exceptionalMessage = "We appear to have low signal" +
							" strength. We can't connect right now, sorry.";
					cancel(true);
				} catch (GTBSSLSocketException e1)
				{
					exceptionalMessage = e1.getMessage();
					cancel(true);
				}

				/* Wait until we cancel */
				while(isCancelled());

				try
				{
					aSock = aSSLSF.getSSLSocket();
				} catch (GTBSSLSocketException e)
				{
					exceptionalMessage =
							"We could not connect to the server! :(" +
							" Do we currently have 3G service?";
					cancel(true);
				}
				/* Wait until we cancel */
				while(isCancelled());
			}

			publishProgress(INCREMENT_PROGRESS);
			try
			{
				OutputStream aOS = null;
				try
				{
					aOS = aSock.getOutputStream();
				} catch (IOException e)
				{
					try
					{
						aSock.close();
						aSSLSF.forceReHandshake(self);
						aSock = aSSLSF.getSSLSocket();
						aOS = aSock.getOutputStream();
					} catch (UnrecoverableKeyException e1)
					{
						exceptionalMessage =
								"We ran into an unrecoverable key" +
							" exception. Please notify the IT Officer. Sorry.";
						cancel(true);
					} catch (KeyStoreException e1)
					{
						exceptionalMessage =
								"We couldn't find or open the KeyStore." +
								"This is manditory to use this " +
								"app so please notify the IT Officer. Sorry.";
						cancel(true);
					} catch (NoSuchAlgorithmException e1)
					{
						exceptionalMessage =
								"This tablet doesn't support an algorithm " +
								"we need to use. Please notify the " +
								"IT Officer so it can be updated. Sorry.";
						cancel(true);
					} catch (SignalException e1)
					{
						exceptionalMessage =
								"We appear to have low signal strength. " +
								"We can't connect right now, sorry.";
						cancel(true);
					} catch (GTBSSLSocketException e1)
					{
						exceptionalMessage = e1.getMessage();
						cancel(true);
					}

					/* Wait until we cancel */
					while(isCancelled());
				}
				
				aPBReq = Request.newBuilder().
						setNReqId(3).
						setSReqType("CARS").
						build();
				Log.v(TAG, "Request type: " + aPBReq.getSReqType());
				Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
				Log.v(TAG, "SReqType = " + aPBReq.getSReqType() +
						" " + aPBReq.getSerializedSize());
				if(aSock.isConnected())
				{
					try
					{
						aOS.write(aPBReq.getSerializedSize());
					}catch (SSLProtocolException e)
					{
						Log.e(TAG, "SSLProtoclException Caught. On-write to" +
								" Output Stream");
						try
						{
							aSSLSF.forceReHandshake(self);
						} catch (UnrecoverableKeyException e1)
						{
							exceptionalMessage =
									"We ran into an unrecoverable key " +
									"exception. " +
									"Please notify the IT Officer. Sorry.";
							cancel(true);
						} catch (KeyStoreException e1)
						{
							exceptionalMessage =
									"We couldn't find or open the KeyStore." +
									"This is manditory to use this app so" +
									" please notify the IT Officer. Sorry.";
							cancel(true);
						} catch (NoSuchAlgorithmException e1)
						{
							exceptionalMessage =
									"This tablet doesn't support an " +
									"algorithm we need to use. Please " +
									"notify the IT Officer so it can " +
									"be updated. Sorry.";
							cancel(true);
						} catch (SignalException e1)
						{
							exceptionalMessage =
									"We appear to have low signal strength. " +
									"We can't connect right now, sorry.";
							cancel(true);
						} catch (GTBSSLSocketException e1)
						{
							exceptionalMessage = e1.getMessage();
							cancel(true);
						}
	
						/* Wait until we cancel */
						while(isCancelled());
	
						try
						{
							aSock = aSSLSF.getSSLSocket();
						} catch (GTBSSLSocketException ex)
						{
							exceptionalMessage =
									"We could not connect to the server! :(" +
									" Do we currently have 3G service?";
							cancel(true);							
						}
						aOS = aSock.getOutputStream();
						try
						{
							aOS.write(aPBReq.getSerializedSize());
						} catch (SSLProtocolException ex)
						{
							try
							{
								aSSLSF.loadStores();
								aSSLSF.createConnection();

								aSock = aSSLSF.getSSLSocket();
								aOS = aSock.getOutputStream();
								aOS.write(aPBReq.getSerializedSize());
							} catch (UnrecoverableKeyException e1)
							{
								exceptionalMessage =
										"We ran into an unrecoverable key" +
									" exception. " +
									"Please notify the IT Officer. Sorry.";
								cancel(true);
							} catch (KeyStoreException e1)
							{
								exceptionalMessage =
										"We couldn't find or open the" +
										" KeyStore. This is manditory to" +
										" use this app so please notify" +
										" the IT Officer. Sorry.";
								cancel(true);
							} catch (NoSuchAlgorithmException e1)
							{
								exceptionalMessage =
										"This tablet doesn't support an " +
										"algorithm we need to use. " +
										"Please notify the IT Officer" +
										" so it can be updated. Sorry.";
								cancel(true);
							} catch (SignalException e1)
							{
								exceptionalMessage =
										"We appear to have low signal" +
										" strength. We can't connect" +
										" right now, sorry.";
								cancel(true);
							} catch (GTBSSLSocketException e1)
							{
								exceptionalMessage = e1.getMessage();
								cancel(true);
							}
	
							/* Wait until we cancel */
							while(isCancelled());
						}
					}
				}
				else
				{
					exceptionalMessage =
							"We could not connect to the server! :(" +
							" Do we currently have 3G service?";
					cancel(true);
				}			
				while(isCancelled());
					
				publishProgress(INCREMENT_PROGRESS);
				if(aSock.isConnected())
					aPBReq.writeTo(aOS);
				else
				{
					Log.v(TAG, "Server-side closed early. Watchdog effect?");
					exceptionalMessage = "Our connection to the server was" +
							"broken! :(" +	" Do we still have 3G service?";
					cancel(true);
					while(isCancelled());
				}
				aOS.close();
				InputStream aIS = aSock.getInputStream();
				byte[] vbuf = new byte[11];
				aIS.read(vbuf);
				//Server Side is already closed
				aSock.close();
				try
				{
					aPBRes = Response.parseFrom(vbuf);
				} catch (InvalidProtocolBufferException e)
				{
					Log.w(TAG, "We received an invalid buf! Failing.");
					exceptionalMessage = "We asked for the number of cars but"
							+ " we got garbage as a reply. Try again soon.";
					cancel(true);
					while(isCancelled());
				}

				publishProgress(INCREMENT_PROGRESS);
				if (aPBRes.getNRespId() != 0)
				{
					publishProgress(-mProgBar.getProgress());
					Log.w(TAG, "Reponse ID: " + aPBRes.getNRespId());
					Log.w(TAG, "Reponse Type: " + aPBRes.getSResValue());
					return -1;
				}
				else
				{
					int numofcars;
					if (aPBRes.getNResAddCount()==1)
					{

						publishProgress(INCREMENT_PROGRESS);
						numofcars = aPBRes.getNResAdd(0);
					    Log.v(TAG, "Number of Cars: " + numofcars);
					    return numofcars;
					}
					else
					{
						publishProgress(-mProgBar.getProgress());

						Log.w(TAG, "We received a buf with the " +
								"correct ID but it doesn't tell us the " +
								"number of cars. Failing.");
						exceptionalMessage = "We asked for the number " +
								"of cars but we got garbage as a reply." +
								" Try again soon.";
						cancel(true);
						while(isCancelled());
					}
				}
			} catch (InvalidProtocolBufferException e)
			{
				e.printStackTrace();
				return -2;
			} catch (IOException e)
			{
				try
				{
					aSSLSF.forceReHandshake(self);
				} catch (UnrecoverableKeyException e1)
				{
					exceptionalMessage = "We ran into an unrecoverable key" +
						" exception. Please notify the IT Officer. Sorry.";
					cancel(true);
				} catch (KeyStoreException e1)
				{
					exceptionalMessage = "We couldn't find or open the KeyStore." +
							"This is manditory to use this app so please notify " +
							"the IT Officer. Sorry.";
					cancel(true);
				} catch (NoSuchAlgorithmException e1)
				{
					exceptionalMessage = "This tablet doesn't support an " +
							"algorithm we need to use. Please notify the " +
							"IT Officer so it can be updated. Sorry.";
					cancel(true);
				} catch (SignalException e1)
				{
					exceptionalMessage = "We appear to have low signal strength. " +
							"We can't connect right now, sorry.";
					cancel(true);
				} catch (GTBSSLSocketException e1)
				{
					exceptionalMessage = e1.getMessage();
					cancel(true);
				}
				/* Wait until we cancel */
				while(isCancelled());

				/* Now that we successfully completed a handshake with the
				 * server, we can call numberOfCars again and we should be
				 * successful. What are the chances of creating a loop?
				 */
				Log.w(TAG, "Are we looping?");
				return numberOfCars();
			}
			/* This should be unreachable, but the compiler doesn't realize
			 * the cancel(true) blocks don't need a return or throw associated
			 * with them.
			 */
			return -1;
		}
		
		protected void onPostExecute(Integer res)
		{
			try
			{
				if(aSock != null)
					aSock.close();
			} catch (IOException e)
			{
			}
			if(isCancelled() && res == null)
			{
				onCancelled();
				return;
			}
			int num = res;
			String cars[] = null;
			
			mProgBar.dismiss();
			if(num < 1)
			{
				Log.w(TAG, "Failed to retrieve number of cars! Sorry!");
				cars = new String[1];
				cars[0] = "Unknown number of cars";
				setListAdapter(new ArrayAdapter<String>(self,
						R.layout.carnums, cars));
				Log.w(TAG, "No cars received. Unknown number.");
			}
			else
			{
				cars = new String[num];
				for(int i = 0; i<num; i++)
				{
					cars[i] = "Car " + (i+1);
				}
				setListAdapter(new ArrayAdapter<String>(self,
						R.layout.carnums, cars));
				Log.v(TAG, "Num of Cars: " + cars.length);
			}
		}
		
		protected void onProgressUpdate(Integer... progress)
		{
			int nTotalProgress = mProgBar.getProgress() + progress[0];
			switch (nTotalProgress)
			{
				case 0:
				case 20:
					mProgBar.setMessage(
							"Establishing Connection with server...");
					break;
				case 40:
					mProgBar.setMessage(
							"Connection Established, Sending request...");
					break;
				case 60:
					mProgBar.setMessage("Receiving response...");
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
		
		protected void onCancelled()
		{
			try
			{
				if(aSock != null)
					aSock.close();
			} catch (IOException e)
			{
			}
			mProgBar.dismiss();
			if(exceptionalMessage.compareTo("") != 0)
			{
				AlertDialog.Builder msgBox = new AlertDialog.Builder(self);
				msgBox.setMessage(exceptionalMessage + "\n\n Would you" +
						" like to continue without a connection to the" +
						" server? This will be much more annoying " +
						"because you will be asked this question every time" +
						" we need to connect to the server.");
				msgBox.setPositiveButton("Yes",
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						/* We're setting this to 8 for no actual reason.
						 * Thus far, GUARD Dogs has not exceeded 8 vans
						 * so this works for us.
						 */
						int num = 8;
						String[] cars = new String[num];
						for(int i = 0; i<num; i++){
							cars[i] = "Car " + (i+1);
						}
						setListAdapter(new ArrayAdapter<String>(self,
								R.layout.carnums, cars));
						Log.v(TAG, "Failed connection Num of Cars: "
								+ cars.length);
					}
				}	);
				msgBox.setNegativeButton("No",
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						new AlertDialog.Builder(self).
							setMessage("Sorry for the inconvenience. " +
									"GUARD the Bridge is now exiting.");
						try
						{
							Thread.sleep(5000);
						} catch (InterruptedException e)
						{
							finish();
						}
						finish();
					}
				}	);
				msgBox.show();
			}
			exceptionalMessage = "";
		}
	}
}