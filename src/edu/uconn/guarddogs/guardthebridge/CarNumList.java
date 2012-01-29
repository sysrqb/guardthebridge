package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;


public class CarNumList extends ListActivity {
	private static final String TAG = "GTBLOG";
    private CarsGtBDbAdapter mDbHelper;
    private TLSGtBDbAdapter nGDbHelper; //Ngin DB Helper
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new CarsGtBDbAdapter(this);
		nGDbHelper = new TLSGtBDbAdapter(this);
        mDbHelper.open();
		listCars();

	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		System.out.println("Index: " + mDbHelper.setCar(position+1));//Position starts at 0, so add 1
		System.out.println("Car Number: " + position);
		Log.v(TAG, "Car Number: " + position);
		setResult(RESULT_OK);
		finish();
	}
	
	public void listCars()
	{
		String cars[];
		int num = numberOfCars();
		if(num < 1)
		{
			System.out.println("Failed to retrieve number of cars!");
			num = 1;
		}
		cars = new String[num];
		for(int i = 0; i<num; i++){
			cars[i] = "Car " + (i+1);
		}
		setListAdapter(new ArrayAdapter<String>(this, R.layout.carnums, cars));
		System.out.println("Num of Cars: " + cars.length);
		Log.v(TAG, "Num of Cars: " + cars.length);
	}
	
	private void getConnFailedDialog(String msg){
		Log.v(TAG, "Failed to Connect to server");
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
		Response aPBRes;
		GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper(this);
		System.out.println("Getting Car");
		Log.v(TAG, "Getting Car");
		
		SSLSocket aSock = aSSLSF.getSSLSocket();
		if(aSock.isBound())
		{
			if(aSock.isConnected())
			{
				System.out.println("Socket is connected!");		
			}
			else
			{
				System.out.println("Socket is NOT connected!");
			}
		}
		else
			System.out.println("Socket is NOT bound!");
		System.out.println("Connected to: " + aSock.getInetAddress().getCanonicalHostName() + " on Port: " + aSock.getPort());
		System.out.println("Local Binding is on: " + aSock.getLocalAddress().getCanonicalHostName() + " on Port: " + aSock.getLocalPort());
		
		try
		{
			OutputStream aOS = aSock.getOutputStream();
			aPBReq = Request.newBuilder().
					setNReqId(2).
					setSReqType("CARS").
					build();
			System.out.println("Request type: " + aPBReq.getSReqType());
			System.out.println("Request Size: " + aPBReq.isInitialized());
			Request aTemp = Request.parseFrom(aPBReq.toByteArray());
			System.out.println("SReqType = " + aTemp.getSReqType() + " " + aTemp.getSerializedSize());
			aPBReq.writeTo(aOS);
			aOS.close();
			InputStream aIS = aSock.getInputStream();
			byte[] vresp = new byte[aIS.available()];
			aIS.read(vresp);
			aPBRes = Response.parseFrom(vresp);
			aIS.close();
			if (!aPBRes.hasNRespId())
			{
				aIS = aSock.getInputStream();
				aPBRes = Response.parseFrom(aIS);
				if (!aPBRes.hasNRespId())
				{
					getConnFailedDialog("Connection to server could not be established." + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				if (aPBRes.getNRespId() != 0)
				{
					System.out.println("Error: " + aPBRes.getSResValue());
					getConnFailedDialog("Connection to server could not be established." + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				int numofcars;
				if (aPBRes.getNResAddCount()==1)
				{
					numofcars = aPBRes.getNResAdd(1);
					System.out.println("Number of Cars: " + numofcars);
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
					System.out.println("Reponse ID: " + aPBRes.getNRespId());
					System.out.println("Reponse Type: " + aPBRes.getSResValue());
					getConnFailedDialog("Connection to server could not be established." + 
							"Please try again in a minute or call Dispatch.");
					return -1;
				}
				else {	
					int numofcars;
					if (aPBRes.getNResAddCount()==1)
					{
						numofcars = aPBRes.getNResAdd(1);
						System.out.println("Number of Cars: " + numofcars);
					    Log.v(TAG, "Number of Cars: " + numofcars);
					    return numofcars;
					}
					else
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
			return -2;
		}
	}
}