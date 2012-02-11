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
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;

public class GuardtheBridge extends Activity {
	private static final int PATRON_READ = 100;
	private static final int PATRON_EDIT = 101;
	private static final String TAG = "GTB";
	private CarsGtBDbAdapter mDbHelper;
	private GtBDbAdapter mGDbHelper;
    private TLSGtBDbAdapter nGDbHelper;
    private GtBSSLSocketFactoryWrapper m_sslSF;
    private GuardtheBridge self;
    
    private Animation slideLeftIn;
    private Animation slideLeftOut;
    private Animation slideRightIn;
    private Animation slideRightOut;
    
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector mGestureDet = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        self = this;
        m_sslSF = new GtBSSLSocketFactoryWrapper(this);
        setContentView(R.layout.rideslist);
        initializeDb();
        retrieveRides();
        populateRides();
        mGDbHelper.close();
        mDbHelper.close();
        nGDbHelper.close();
    }
    
    public boolean onOptionItemSelected(MenuItem menu){
		return super.onOptionsItemSelected(menu);
    }
    
    public void initializeDb()
     {
    	mGDbHelper = new GtBDbAdapter(this);
    	mGDbHelper.open();
    	mDbHelper = new CarsGtBDbAdapter(this);
	    mDbHelper.open();
	    nGDbHelper = new TLSGtBDbAdapter (this);
        nGDbHelper.open();
    }
    
   public void retrieveRides() {
	   ArrayList<Integer> vRides = mGDbHelper.fetchAllPid();
	   Request aPBReq = Request.newBuilder().
			   setNReqId(1).
			   setSReqType("CURR").
			   setNCarId(mDbHelper.getCar()).
	   		   addAllNParams(vRides).
	   		   build();
	   
	   Log.v(TAG, "Request type: " + aPBReq.getSReqType());
	   Log.v(TAG, "Request ID: " + aPBReq.getNReqId());
	   Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
	   Log.v(TAG, "SReqType = " + aPBReq.getSReqType() + " " + 
			   aPBReq.getSerializedSize());
	   SSLSocket aSock = m_sslSF.getSSLSocket();
	   if (aSock.isClosed())
		   aSock = m_sslSF.createSSLSocket(this);
	   if (m_sslSF.getSession() == null)
	   {
		   m_sslSF = m_sslSF.getNewSSLSFW(this);
		   aSock = m_sslSF.getSSLSocket();
	   }
	   try {
		   OutputStream aOS = aSock.getOutputStream();
		   aOS.write(aPBReq.getSerializedSize());
		   byte[] vbuf = aPBReq.toByteArray();
		   //aPBReq.writeTo(aOS);
		   aOS.write(vbuf);
		   InputStream aIS = aSock.getInputStream();
		   vbuf = new byte[1];
		   aIS.read(vbuf);
		   vbuf = new byte[vbuf[0]];
		   aIS.read(vbuf);
		   Response apbRes;
		   try {
			   apbRes = Response.parseFrom(vbuf);
		
			   Log.v(TAG, "Response Buffer:");
			   Log.v(TAG, TextFormat.shortDebugString(apbRes));
			   Log.v(TAG, "PatronList Buffer: ");
			   Log.v(TAG, TextFormat.shortDebugString(
					   apbRes.getPlPatronList()));
			   addToDb(apbRes.getPlPatronList());
			   Log.v(TAG, "Added to DB");
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   }catch (IOException e)
	   {
		   e.printStackTrace();
	   }
   	}
   
   public void addToDb(PatronList list){
	   for (PatronInfo patron : list.getPatronList())
		   mGDbHelper.createPatron(patron.toByteArray(), patron.getPid());
   }
   
   public void populateRides(){
	   PatronInfo[] vPI = mGDbHelper.fetchAllPatrons();
	   if (vPI.length == 0)
	   {
		   String[] msg = new String[1];
		   msg[0] = "No pending rides! Just chill";
		   //ListView aLV = (ListView) findViewById(R.id.list);
		   //aLV.setAdapter(new ArrayAdapter<String>(this, R.layout.activelist, R.id.nameVal, msg));
		   new ArrayAdapter<String>(this, R.layout.rideslist, msg);
		   Log.w(TAG, "No rides received.");
	   }
	   else
	   {
		   /*int[] to = new int[]{R.string.nameVal, R.string.ttVal};
		   String[] from = new String[]{vPI[i].getName(), vPI[i].getTimetaken()};
		   ArrayList<Map<String, String>> listmap = new ArrayList<Map<String, String>>(vPI.length);
		   TreeMap<String, String> map = new TreeMap<String, String>();
		   for (int i = 0; i < vPI.length; i++){
			   
			   map.put(vPI[i].getTimetaken(), Integer.toString(i));
			   listmap.add(map);
		   }
		   new SimpleAdapter(this, listmap, R.layout.activelist, from, to));*/
		   String[] msg = new String[vPI.length];
		   ListView aLV = (ListView) findViewById(R.id.activelist_list);
		   for(int i = 0; i<vPI.length; i++)
		   {
			   msg[i] = vPI[i].getPid() + " " + vPI[i].getTimeassigned() + 
					   ": " + vPI[i].getName() + " - " + vPI[i].getPickup();
		   }
		   
		   aLV.setAdapter(new ArrayAdapter<String>(this, R.layout.rides, msg));
		   Log.v(TAG, "Finished compiling list of assigned rides");
		   
		   setActions(aLV, (Button)findViewById(R.id.dispatch), 
				   (Button)findViewById(R.id.emergency));
	   }
   }
   
   private void setActions(ListView lv, Button dispatch, Button emerg){
	   lv.setOnItemClickListener(new OnItemClickListener() { 
		   @Override
		   public void onItemClick(AdapterView<?> av, 
				   View v, 
				   int position, 
				   long id){
				Log.v(TAG, "Displaying Ride: " + id);
				Log.v(TAG, "Car Number: " + position);
				TextView tv = (TextView) v;
				long row = 0;
				try
				{
					row = Long.parseLong(tv.getText().
							toString().
							substring(0, 1));
				} catch (NumberFormatException e)
				{
					return;
				}
				Intent intent = new Intent(self, ShowPatron.class);
				intent.putExtra(GtBDbAdapter.KEY_ROWID, row);
				startActivityForResult(intent, PATRON_READ);
		   }
	   });
	   lv.setOnItemLongClickListener(new OnItemLongClickListener() { 
		   @Override
		   public boolean onItemLongClick(AdapterView<?> av, 
				   View v, 
				   int position, 
				   long id){
				Log.v(TAG, "Editing Ride: " + id);
				Log.v(TAG, "Car Number: " + position);
				TextView tv = (TextView) v;
				long row = 0;
				try
				{
					row = Long.parseLong(tv.getText().
							toString().
							substring(0, 1));
				} catch (NumberFormatException e)
				{
					return false;
				}
				Intent intent = new Intent(self, EditPatron.class);
				intent.putExtra(GtBDbAdapter.KEY_ROWID, row);
				startActivityForResult(intent, PATRON_EDIT);
				return true;
		   }
	   });
	   
   }
   
   protected void onActivityResult(int requestCode, 
		   int resultCode, 
		   Intent intent) {
       super.onActivityResult(requestCode, resultCode, intent);
       Log.v(TAG, "On Return");
       m_sslSF = m_sslSF.getNewSSLSFW(this);
       setContentView(R.layout.rideslist);
       initializeDb();
       retrieveRides();
       populateRides();
       mGDbHelper.close();
       mDbHelper.close();
       nGDbHelper.close();
   }
}
