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
import java.util.regex.Pattern;

import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;

public class GuardtheBridge extends FragmentActivity {
	private static final int PATRON_EDIT = 101;
	private static final String TAG = "GTB";
	private static final int OPENRIDES = 0;
	private static final int CLOSEDRIDES = 1;
	private static final int NUM_ITEMS = 2;
    private static GuardtheBridge sself;
    private ProgressDialog mProgBar = null;
    
    private GtBDbAdapter mGDbHelper = null;
	private CarsGtBDbAdapter mCDbHelper = null;
	private GtBSSLSocketFactoryWrapper mSSLSF;
    
    private GTBAdapter m_GFPA = null;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        sself = this;
        setContentView(R.layout.rideslist);
        
        updateList();
        mSSLSF = new GtBSSLSocketFactoryWrapper(this);
        mGDbHelper = new GtBDbAdapter(this);
        mCDbHelper = new CarsGtBDbAdapter(this);
        
        Button aRfrshBtn = (Button)findViewById(R.id.refresh);
        aRfrshBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new CurrTask().execute();
		        ArrayListFragment aALF = (ArrayListFragment) GTBAdapter.getFragment().getActivity().
		        		getSupportFragmentManager().
		        		findFragmentByTag("android:switch:" + R.id.ridelist_pageview + ":0");
		        if (aALF != null && aALF.getView() != null)
		        	aALF.updateView();
		        aALF = (ArrayListFragment) GTBAdapter.getFragment().getActivity().
		        		getSupportFragmentManager().
		        		findFragmentByTag("android:switch:" + R.id.ridelist_pageview + ":1");
		        if (aALF != null && aALF.getView() != null)
		        	aALF.updateView();
			}
		});
    }
    
    public boolean onOptionItemSelected(MenuItem menu){
		return super.onOptionsItemSelected(menu);
    }
    
    private void updateList()
    {
        ViewPager aVp = (ViewPager)findViewById(R.id.ridelist_pageview);
        m_GFPA = new GTBAdapter(getSupportFragmentManager());
        aVp.setAdapter(m_GFPA);
    }
   	   
	public void addToDb(PatronList list)
	{
		mGDbHelper.open();
		for (PatronInfo patron : list.getPatronList())
			   mGDbHelper.createPatron(patron.toByteArray(), patron.getPid());
		mGDbHelper.close();
	}
   
   public static class GTBAdapter extends FragmentPagerAdapter
   {
	   private static ArrayListFragment mFrag = null;
	   public GTBAdapter(FragmentManager fm)
	   {
		   super(fm);
	   }
	   public int getCount()
	   {
		   return NUM_ITEMS;
	   }
	   public Fragment getItem(int position)
	   {
		   mFrag = ArrayListFragment.newInstance(position);
		   return mFrag;
	   }
	   
	   public static ArrayListFragment getFragment()
	   {
		   return mFrag;
	   }
   }
   
   public static class ArrayListFragment extends ListFragment
   {
	   private int mNum;
	   private static GtBDbAdapter m_ALFGDbHelper = null;
	   private CarsGtBDbAdapter mDbHelper = null;
	   private TLSGtBDbAdapter nGDbHelper = null;
	   //private GtBSSLSocketFactoryWrapper m_sslSF;
	    
	   public static ArrayListFragment newInstance(int num)
	   {
		   Log.v(TAG, "ArrayListFragment: newInstance: num=" + num);
		   ArrayListFragment f = new ArrayListFragment();
		   
		   Bundle args = new Bundle();
		   args.putInt("num", num);
		   f.setArguments(args);
		   return f;
	   }
	   
	   public void onCreate(Bundle savedInstanceState)
	   {
		   super.onCreate(savedInstanceState);
		   Log.v(TAG, "ArrayListFragment: onCreate");
	       //m_sslSF = new GtBSSLSocketFactoryWrapper(sself);
		   m_ALFGDbHelper = new GtBDbAdapter(sself);
		   mDbHelper = new CarsGtBDbAdapter(sself);
		   nGDbHelper = new TLSGtBDbAdapter(sself);
		   mNum = getArguments() != null ? getArguments().getInt("num") : 1;
		   //initializeDb();
		   //sself.new CurrTask().execute();
		   //updateView();
		   //retrieveRides();
	   }
	   
	   public View onCreateView(LayoutInflater inflater, ViewGroup container,
			   Bundle savedInstanceState)
	   {
		   Log.v(TAG, "ArrayListFragment: onCreateView");
		   m_ALFGDbHelper.open();
		   TextView aTV = null;
		   View v = null;
		   if (mNum == 0)
		   {
			   Log.v(TAG, "onCreateView: Current: mNum = " + mNum);
			   v = inflater.inflate(R.layout.openrides, container, false);
			   aTV = (TextView)v.findViewById(R.id.openrides_title);
			   aTV.setText(R.string.openrides_title);
		   }
		   else/* if (mNum == 1)*/
		   {
			   Log.v(TAG, "onCreateView: Closed: mNum = " + mNum);
			   v = inflater.inflate(R.layout.closedrides, container, false);
			   aTV = (TextView)v.findViewById(R.id.closedrides_title);
			   aTV.setText(R.string.closedrides_title);
		   }
		   ((RelativeLayout)aTV.getParent()).removeView(aTV);
		   Log.v(TAG, "onCreateView: returning");
		   m_ALFGDbHelper.close();
		   return v;
	   }
	   
	   public void onActivityCreated(Bundle savedInstanceState)
	   {
		   super.onActivityCreated(savedInstanceState);
		   Log.v(TAG, "ArrayListFragment: onActivityCreated");
		   updateView();
	   }
	   
	   @Override
	   public void onListItemClick(ListView l, View v, int position, long id){
			Log.v(TAG, "Displaying Ride: " + id);
			Log.v(TAG, "Car Number: " + position);
			TextView tv = (TextView) v;
			long pid = 0;
			try
			{
				Pattern pPidRegex = Pattern.compile("\\d*:");
				String atmp = "";
				try
				{
					atmp = tv.getText().
							toString().split(pPidRegex.pattern())[0];
					atmp = atmp.substring(0, atmp.length()-1);
				} catch (NullPointerException ex)
				{
					Log.w(TAG, "NULL Pointer on pid read: " + atmp);
					return;
				}
				pid = Long.parseLong(atmp);
			} catch (NumberFormatException e)
			{
				return;
			}
			Intent intent = new Intent(sself, ShowPatron.class);
			intent.putExtra(GtBDbAdapter.KEY_ROWID, pid);
			startActivity(intent);
	   }
	   
	   public boolean onListItemLongClick(AdapterView<?> av, 
			   View v, 
			   int position, 
			   long id)
	   {
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
			Intent intent = new Intent(sself, EditPatron.class);
			intent.putExtra(GtBDbAdapter.KEY_ROWID, row);
			startActivityForResult(intent, PATRON_EDIT);
			return true;
	   }
	   
	@SuppressWarnings("unchecked")
	public void updateView()
	   {
		   Log.v(TAG, "ArrayListFragment: updateView");
		   ListAdapter aVAA = getListAdapter();
		   if (aVAA == null)
		   {
			   if (mNum == 0)
				   aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(OPENRIDES));
			   else /* if (mNum == 1)*/
				   aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(CLOSEDRIDES));
		   }
		   if (aVAA.equals(new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(OPENRIDES))) || 
						   aVAA.equals(new ArrayAdapter<String>(getActivity(),
								   R.layout.rides, populateRides(CLOSEDRIDES))))
			   ((ArrayAdapter<String>)aVAA).notifyDataSetChanged();
		   setListAdapter(aVAA);
	   }
	   
	   public void initializeDb()
	   {
	       mDbHelper.open();
	       nGDbHelper.open();
	   }
   
	   public void addToDb(PatronList list){
		   for (PatronInfo patron : list.getPatronList())
			   m_ALFGDbHelper.createPatron(patron.toByteArray(), patron.getPid());
	   }
	   
	   public String[] populateRides(int ridetype){
		   m_ALFGDbHelper.open();
		   PatronInfo[] vPI = m_ALFGDbHelper.fetchAllPatrons(ridetype);
		   m_ALFGDbHelper.close();
		   if (vPI.length == 0)
		   {
			   String[] msg = new String[1];
			   msg[0] = "No pending rides! Just chill";
			   Log.w(TAG, "No rides received.");
			   return msg;
		   }
		   else
		   {
			   String[] msg = new String[vPI.length + 3];
			   
			   for(int i = 0; i<vPI.length; i++)
			   {
				   msg[i] = vPI[i].getPid() + " " + vPI[i].getTimeassigned() + 
						   ": " + vPI[i].getName() + " - " + vPI[i].getPickup();
			   }
			   
			   /* 
			    * Adds three lines such that the last three rides are 
			    * not covered by the buttons
			    */
			   for(int i = vPI.length; i<msg.length; i++)
				   msg[i] = "";
			   return msg;
		   }
	   }
   }
   
   private class CurrTask extends AsyncTask<Void, Integer, Integer>
   {
	   static final int INCREMENT_PROGRESS = 20;
	   protected void onPreExecute()
	   {
		   mProgBar = new ProgressDialog(sself);
		   mProgBar.setCancelable(true);
		   mProgBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		   mProgBar.setMessage("Establishing Connection with server...");
		   mProgBar.show();
	   }

	   @Override
	   protected Integer doInBackground(Void... params)
	   {
		   return retrieveRides();
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
			   mProgBar.setMessage("Reading new rides...");
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
	   
	   public int retrieveRides()
	   {
		   mGDbHelper.open();
		   mCDbHelper.open();
		   ArrayList<Integer> vRides = mGDbHelper.fetchAllPid();
		   mGDbHelper.close();
		   Request aPBReq = Request.newBuilder().
				   setNReqId(1).
				   setSReqType("CURR").
				   setNCarId(mCDbHelper.getCar()).
		   		   addAllNParams(vRides).
		   		   build();
		   mCDbHelper.close();
		   publishProgress(INCREMENT_PROGRESS);
		   Log.v(TAG, "Request type: " + aPBReq.getSReqType());
		   Log.v(TAG, "Request ID: " + aPBReq.getNReqId());
		   Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
		   Log.v(TAG, "SReqType = " + aPBReq.getSReqType() + " " + 
				   aPBReq.getSerializedSize());
		   SSLSocket aSock = mSSLSF.createSSLSocket(sself);
		   if (mSSLSF.getSession() == null)
		   {
			   mSSLSF = mSSLSF.getNewSSLSFW(sself);
			   aSock = mSSLSF.getSSLSocket();
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
					mSSLSF.forceReHandshake(sself);
					aSock = mSSLSF.getSSLSocket();
					aOS = aSock.getOutputStream();
					try
					{
						aOS.write(aPBReq.getSerializedSize());
					} catch (SSLProtocolException exc)
					{
						mSSLSF = mSSLSF.getNewSSLSFW(sself);
						aSock = mSSLSF.getSSLSocket();
						aOS = aSock.getOutputStream();
						aOS.write(aPBReq.getSerializedSize());
					}
			   }
			   byte[] vbuf = aPBReq.toByteArray();
			   aOS.write(vbuf);
			   publishProgress(INCREMENT_PROGRESS);
			   InputStream aIS = aSock.getInputStream();
			   vbuf = new byte[9];
			   aIS.read(vbuf);
			   /* Handle messages smaller than 9 bytes */
			   int nsize = (vbuf.length - 1);
			   for (; nsize>0; nsize--)
			   {
				   if(vbuf[nsize] == 0)
				   {
					   continue;
				   }
				   break;
			   }
			   byte[] vbuf2 = new byte[nsize + 1];
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
				   apbRes = Response.parseFrom(vbuf);
				   publishProgress(INCREMENT_PROGRESS);
				   
				   Log.v(TAG, "Response Buffer:");
				   Log.v(TAG, TextFormat.shortDebugString(apbRes));
				   Log.v(TAG, "PatronList Buffer: ");
				   Log.v(TAG, TextFormat.shortDebugString(
						   apbRes.getPlPatronList()));
				   addToDb(apbRes.getPlPatronList());
				   Log.v(TAG, "Added to DB");
				   
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
