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

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;


public class CarNumList extends ListActivity {
	private static final String TAG = "CNL-GTBLOG";
    private CarsGtBDbAdapter m_aCDbHelper;
    private CarNumList self = this;
    private ProgressDialog mProgBar = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_aCDbHelper = new CarsGtBDbAdapter(this);
        m_aCDbHelper.open();
		new CarsTask().execute();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.v(TAG, "Index: " + m_aCDbHelper.setCar(position+1));//Position starts at 0, so add 1
		Log.v(TAG, "Car Number: " + position);
		m_aCDbHelper.close();
		setResult(RESULT_OK);
		finish();
	}
	
	private class CarsTask extends AsyncTask<Void, Integer, Integer>
	{
		
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
			return numOfCars;
		}
		
		public int numberOfCars()
		{
			Request aPBReq;
			Response aPBRes = null;
			GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper(self);
			final int INCREMENT_PROGRESS = 20;
			Log.v(TAG, "Getting Car");
			
			publishProgress(INCREMENT_PROGRESS);
			SSLSocket aSock = aSSLSF.getSSLSocket();
			if(aSock.isBound())
			{
				if(!aSock.isClosed())
				{
					Log.v(TAG, "Socket is not closed!");	
				}
				else
				{
					Log.w(TAG, "Socket IS closed!");
					aSock = aSSLSF.createSSLSocket(self);
				}
			}
			else
				Log.w(TAG, "Socket is NOT bound!");
			
			if (aSock.isOutputShutdown())
			{
				Log.w(TAG, "Output Stream is Shutdown!");
				aSock = aSSLSF.getSSLSocket();
			}
			if (aSSLSF.getSession() != null)
				Log.v(TAG, "Session is still valid");
			else
			{
				Log.w(TAG, "Session is NO LONGER VALID");
				aSSLSF = new GtBSSLSocketFactoryWrapper(self);
				aSock = aSSLSF.getSSLSocket();
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
					aSSLSF.forceReHandshake(self);
					aSock = aSSLSF.getSSLSocket();
					aOS = aSock.getOutputStream();
				}
				
				aPBReq = Request.newBuilder().
						setNReqId(3).
						setSReqType("CARS").
						build();
				Log.v(TAG, "Request type: " + aPBReq.getSReqType());
				Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
				Log.v(TAG, "SReqType = " + aPBReq.getSReqType() + " " + aPBReq.getSerializedSize());
				try 
				{
					aOS.write(aPBReq.getSerializedSize());
				}catch (SSLProtocolException e)
				{
					Log.e(TAG, "SSLProtoclException Caught. On-write to Output Stream");
					aSSLSF.forceReHandshake(self);
					aSock = aSSLSF.getSSLSocket();
					aOS = aSock.getOutputStream();
					try
					{
						aOS.write(aPBReq.getSerializedSize());
					} catch (SSLProtocolException ex)
					{
						aSSLSF = aSSLSF.getNewSSLSFW(self);
						aSock = aSSLSF.getSSLSocket();
						aOS = aSock.getOutputStream();
						aOS.write(aPBReq.getSerializedSize());
					}
				}

				publishProgress(INCREMENT_PROGRESS);
				aPBReq.writeTo(aOS);
				aOS.close();
				InputStream aIS = aSock.getInputStream();
				byte[] vbuf = new byte[11];
				aIS.read(vbuf);
				aSock.close(); //Server Side is already closed
				try
				{
					aPBRes = Response.parseFrom(vbuf);
				} catch (InvalidProtocolBufferException e)
				{
					e.printStackTrace();
					aSock = aSSLSF.getSSLSocket();
					aOS = aSock.getOutputStream();
					aOS.write(aPBReq.getSerializedSize());
					aPBReq.writeTo(aOS);
					aOS.close();
					aIS = aSock.getInputStream();
					vbuf = new byte[11];
					aIS.read(vbuf);
					String tmp = "";
					for (int i = 0; i<vbuf.length; i++)
						tmp = tmp + vbuf[i] + " ";
					Log.v(TAG, tmp);
				}

				publishProgress(INCREMENT_PROGRESS);
				if (!aPBRes.hasNRespId())
				{
					aIS = aSock.getInputStream();
					aPBRes = Response.parseFrom(aIS);
					if (!aPBRes.hasNRespId())
					{
						publishProgress(-mProgBar.getProgress());
						return -1;
					}
					if (aPBRes.getNRespId() != 0)
					{
						publishProgress(-mProgBar.getProgress());
						Log.e(TAG, "Error: " + aPBRes.getSResValue());
						return -1;
					}
					int numofcars;
					if (aPBRes.getNResAddCount()==1)
					{
						numofcars = aPBRes.getNResAdd(1);
					    Log.v(TAG, "Number of Cars: " + numofcars);
					    return numofcars;
					}
					else{
						publishProgress(-mProgBar.getProgress());
						return numberOfCars();
					}
				}
				else
				{
					if (aPBRes.getNRespId() != 0)
					{
						publishProgress(-mProgBar.getProgress());
						Log.w(TAG, "Reponse ID: " + aPBRes.getNRespId());
						Log.w(TAG, "Reponse Type: " + aPBRes.getSResValue());
						return -1;
					}
					else {	
						int numofcars;
						if (aPBRes.getNResAddCount()==1)
						{

							publishProgress(INCREMENT_PROGRESS);
							numofcars = aPBRes.getNResAdd(0);
						    Log.v(TAG, "Number of Cars: " + numofcars);
						    return numofcars;
						}
						else{
							publishProgress(-mProgBar.getProgress());
							aSSLSF.forceReHandshake(self);
							return numberOfCars();
						}
					}
				}
			} catch (InvalidProtocolBufferException e)
			{
				e.printStackTrace();
				return -2;
			} catch (IOException e)
			{
				e.printStackTrace();
				aSSLSF.forceReHandshake(self);
				aSock = aSSLSF.getSSLSocket();
				return -2;
			}
		}
		
		protected void onPostExecute(Integer res)
		{
			int num = res;
			String cars[] = null;
			
			mProgBar.dismiss();
			if(num < 1)
			{
				Log.w(TAG, "Failed to retrieve number of cars!");
				cars = new String[1];
				cars[0] = "Unknown number of cars";
				setListAdapter(new ArrayAdapter<String>(self, R.layout.carnums, cars));
				Log.w(TAG, "No cars received. Unknown number.");
			}
			else
			{
				cars = new String[num];
				for(int i = 0; i<num; i++){
					cars[i] = "Car " + (i+1);
				}
				setListAdapter(new ArrayAdapter<String>(self, R.layout.carnums, cars));
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
					mProgBar.setMessage("Establishing Connection with server...");
					break;
				case 40:
					mProgBar.setMessage("Connection Established, Sending request...");
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
	}
}
