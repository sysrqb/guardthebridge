package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
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
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_aCDbHelper = new CarsGtBDbAdapter(this);
        m_aCDbHelper.open();
		listCars();
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
	
	public void listCars()
	{
		String cars[];
		int num = numberOfCars();
		if(num < 1)
		{
			Log.w(TAG, "Failed to retrieve number of cars!");
			cars = new String[1];
			cars[0] = "Unknown number of cars";
			setListAdapter(new ArrayAdapter<String>(this, R.layout.carnums, cars));
			Log.w(TAG, "No cars received. Unknown number.");
		}
		else
		{
			cars = new String[num];
			for(int i = 0; i<num; i++){
				cars[i] = "Car " + (i+1);
			}
			setListAdapter(new ArrayAdapter<String>(this, R.layout.carnums, cars));
			Log.v(TAG, "Num of Cars: " + cars.length);
		}
	}
	
	private void getConnFailedDialog(String msg){
		Log.w(TAG, "Failed to Connect to server");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
           	    finish();
           }
        });
		builder.show();
	}
		
	public int numberOfCars()
	{
		Request aPBReq;
		Response aPBRes = null;
		GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper(this);
		Log.v(TAG, "Getting Car");
		
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
				aSock = aSSLSF.createSSLSocket(this);
			}
		}
		else
			Log.w(TAG, "Socket is NOT bound!");
		
		if (aSock.isOutputShutdown())
		{
			Log.w(TAG, "Output Stream is Shutdown!");
			aSock = aSSLSF.getSSLSocket();
		}
		if (aSSLSF.getSession() == null)
		{
			Log.v(TAG, "Session is still valid");
			aSSLSF = new GtBSSLSocketFactoryWrapper(this);
			aSock = aSSLSF.getSSLSocket();
		}
		else
			Log.w(TAG, "Session is NO LONGER VALID");
		
		try
		{
			OutputStream aOS = null;
			try
			{
				aOS = aSock.getOutputStream();
			} catch (IOException e)
			{
				aSSLSF.forceReHandshake(this);
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
			aOS.write(aPBReq.getSerializedSize());
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
			aIS.close();
			if (!aPBRes.hasNRespId())
			{
				aIS = aSock.getInputStream();
				aPBRes = Response.parseFrom(aIS);
				if (!aPBRes.hasNRespId())
				{
					getConnFailedDialog("Connection to server could not be established. " + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				if (aPBRes.getNRespId() != 0)
				{
					Log.e(TAG, "Error: " + aPBRes.getSResValue());
					getConnFailedDialog("Connection to server could not be established. " + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				int numofcars;
				if (aPBRes.getNResAddCount()==1)
				{
					numofcars = aPBRes.getNResAdd(1);
				    Log.v(TAG, "Number of Cars: " + numofcars);
				    return numofcars;
				}
				else
					return numberOfCars();
			}
			else
			{
				if (aPBRes.getNRespId() != 0)
				{
					Log.w(TAG, "Reponse ID: " + aPBRes.getNRespId());
					Log.w(TAG, "Reponse Type: " + aPBRes.getSResValue());
					getConnFailedDialog("Connection to server could not be established. " + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				else {	
					int numofcars;
					if (aPBRes.getNResAddCount()==1)
					{
						numofcars = aPBRes.getNResAdd(0);
					    Log.v(TAG, "Number of Cars: " + numofcars);
					    return numofcars;
					}
					else
						aSSLSF.forceReHandshake(this);
						return numberOfCars();
				}
			}
		} catch (InvalidProtocolBufferException e)
		{
			e.printStackTrace();
			return -2;
		} catch (IOException e)
		{
			e.printStackTrace();
			aSSLSF.forceReHandshake(this);
			aSock = aSSLSF.getSSLSocket();
			getConnFailedDialog("Connection to server could not be established. " + 
					"Please try again in a minute or call Dispatch.");
			return -2;
		}
	}
}