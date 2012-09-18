package edu.uconn.guarddogs.guardthebridge;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class GTBLocationManager extends Service {
	private static final String TAG = "GTBLocationManager";
	private LocationManager mGPSManager = null;
	private Location aLastLocation = null;
	private double nLat =0, nLong = 0, nSpeed = 0, nHeading = 0, nAccur = 0, nAlt = 0;
	private GTBLocationManager self = null;
	private CarsGtBDbAdapter mCDbHelper = null;
	private ArrayList<List<NameValuePair>> queue = null;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		String sLocServ = "";
		if (intent != null && intent.getExtras() != null)
		{
			sLocServ = intent.getExtras().getString("LocServ");
		}
		if (sLocServ == null || sLocServ.compareTo("") == 0)
			sLocServ = LocationManager.GPS_PROVIDER;
		mGPSManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		LocationListener aLocationListener = new LocationListener()
			{
				public void onLocationChanged(Location location) {
					self.makeUseOfNewLocation(location);
				}

				@Override
				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// TODO Auto-generated method stub
					
				}
			};
			mGPSManager.requestLocationUpdates(sLocServ, 0, 0, aLocationListener);
			backgroundIt();
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		self = this;
		mCDbHelper = new CarsGtBDbAdapter(this);
		queue = new ArrayList<List<NameValuePair>>();
	}
	
	private void backgroundIt()
	{
		(new Thread (new Runnable() 
		  {	
		    public void run()
	        {
		    	Looper.prepare();
	          new Handler().postDelayed( new Runnable()
	            {
		          public void run()
		          {
	                for(;;)
		            {
	                	
	                  mCDbHelper.open();
		              postLocation(mCDbHelper.getCar());
		              mCDbHelper.close();
	                	
		              try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						break;
					}
		            }
		          }
	            }, 10000); // Execute background update every 30 seconds
	          Looper.loop();
	        }
	  })).start();
		
	}
	
	private void makeUseOfNewLocation(Location aLoc)
	{
		if (aLastLocation == null  // If we don't have a last location
				|| isBetterLocation(aLoc))  // If better location
		{
			aLastLocation = aLoc;  // use this one
			nLat = aLoc.getLatitude();
			nLong = aLoc.getLongitude();
			nSpeed = aLoc.getSpeed();
			nHeading = aLoc.getBearing();
			nAccur = aLoc.getAccuracy();
			nAlt = aLoc.getAltitude();
		}
	}
	
	private boolean isBetterLocation(Location newLocation)
	{
		final long INTERVAL = 1000*30;
		long timeDelta = newLocation.getTime() - aLastLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > INTERVAL;
	    boolean isNewer = timeDelta > 0;
	    
	    if (isSignificantlyNewer)
	    	return isAccurateEnough(newLocation);
	    if (isNewer)
	    {
	    	double accurDelta = newLocation.getAccuracy() - aLastLocation.getAccuracy();
	    	if (accurDelta < 0)
	    		return true;
	    }
		return false;
	}
	
	private boolean isAccurateEnough(Location newLocation)
	{
		return true;  // Conditionalize this once we have an idea of the range accuracy lies in
	}

	
	public void postLocation(int inCarNum)
	{
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://guarddogs.uconn.edu/index.php?id=36");
	    
	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("v_id", Integer.toString(inCarNum)));
	        nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(nLat)));
	        nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(nLong)));
	        nameValuePairs.add(new BasicNameValuePair("speed", Double.toString(nSpeed)));
	        nameValuePairs.add(new BasicNameValuePair("heading", Double.toString(nHeading)));
	        nameValuePairs.add(new BasicNameValuePair("accuracy", Double.toString(nAccur)));
	        nameValuePairs.add(new BasicNameValuePair("altitude", Double.toString(nAlt)));
	        
	        GtBSSLSocketFactoryWrapper sslConn = new GtBSSLSocketFactoryWrapper();
	        sslConn.setContext(this);
	        if(!sslConn.haveDataConnection())
	        	queue.add(nameValuePairs);
	        else
	        {
	        	if(queue.size() > 0)
	        	{
	        		while(!queue.isEmpty())
	        		{
		        		httppost.setEntity(new UrlEncodedFormEntity(queue.get(0)));
		        		
				        // Execute HTTP Post Request
				        HttpResponse aRespGPS = httpclient.execute(httppost);
				        //Header aHdrGPS;
				        
				        HttpEntity aEntityGPS = aRespGPS.getEntity();
				        
				        DataInputStream aBufInStr = new DataInputStream(aEntityGPS.getContent());
				        String sContent = "";
				        int nChar = 0;
				        while ((nChar = aBufInStr.read()) != -1)
				        {
				        	sContent += (char)nChar;
				        }
				        
				        Log.v(TAG, "GPS POST response: " + sContent);
	        		}
	        	}
	        
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	
		        // Execute HTTP Post Request
		        HttpResponse aRespGPS = httpclient.execute(httppost);
		        //Header aHdrGPS;
		        
		        HttpEntity aEntityGPS = aRespGPS.getEntity();
		        
		        DataInputStream aBufInStr = new DataInputStream(aEntityGPS.getContent());
		        String sContent = "";
		        int nChar = 0;
		        while ((nChar = aBufInStr.read()) != -1)
		        {
		        	sContent += (char)nChar;
		        }
		        
		        Log.v(TAG, "GPS POST response: " + sContent);
		        /*String sHeaders = "";
		        for(HeaderIterator ahdrIt = aRespGPS.headerIterator();ahdrIt.hasNext(); ahdrIt.next())
		        {
		        	if (ahdrIt.hasNext())
		        	{
		        		aHdrGPS = ahdrIt.nextHeader();
		        		sHeaders += aHdrGPS.getValue() + " :: ";
		        	}
		        }
		        
		        Log.v(TAG, "GPS POST response: " + sHeaders);*/
	        }
	        
	    } catch (ClientProtocolException e) {
	    	Log.e(TAG, "ClientProtocolException caught. Is the server down?");
	    	
	    } catch (IOException e) {
	    	Log.e(TAG, "IOException caught. Is the server down?");
	    }
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
