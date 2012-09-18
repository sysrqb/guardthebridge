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
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;

public class GuardtheBridge extends FragmentActivity
{
	private static final int PATRON_EDIT = 101;
	private static final String TAG = "GTB";
	private static final int OPENRIDES = 0;
	private static final int CLOSEDRIDES = 1;
	/* Number of panels */
	private static final int NUM_ITEMS = 2;
	/* Static Self */
    private static GuardtheBridge sself;
    private ProgressDialog mProgBar = null;

    private GtBDbAdapter mGDbHelper = null;
	private CarsGtBDbAdapter mCDbHelper = null;

	private ViewPager mVp = null;
    private GTBAdapter m_GFPA = null;
	private String exceptionalMessage = "";
	private boolean mUpdatingNow = false;
	private Thread mainThread = null;
	private Handler mainHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        sself = this;
        setContentView(R.layout.rideslist);
        mainThread = Thread.currentThread();
        mainHandler = new Handler();

        updateList();
        createBackgroundThreads();

        Button aRfrshBtn = (Button)findViewById(R.id.refresh);
        aRfrshBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v)
			{
				/* We don't want to make another unnecessary connection
				 * to the server. We're already handling updates.
				 */
				if(mUpdatingNow)
				{
					AlertDialog.Builder msgBox =
							new AlertDialog.Builder(sself);
					msgBox.setMessage("We're actually already updating in" +
							" the background right now. If you have new" +
							" rides, they should show up very soon!");
					msgBox.setPositiveButton("Ok",
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							return;
						}
					}	);
					msgBox.show();
					return;
				}
				mUpdatingNow = true;
				new CurrTask().execute();  // Request the update
			}
		});
		
        /* Launch GPS Location Updater */
        Log.e(TAG, "Make sure this is uncommented!");
		/* Intent i = new Intent(this, GTBLocationManager.class);
		this.startService(i); */
    }

    public void onRestart()
    {
    	super.onRestart();
    	createBackgroundThreads();

        Button aRfrshBtn = (Button)findViewById(R.id.refresh);
        aRfrshBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v)
			{
				/* We don't want to make another unnecessary connection
				 * to the server. We're already handling updates.
				 */
				if(mUpdatingNow)
				{
					AlertDialog.Builder msgBox =
							new AlertDialog.Builder(sself);
					msgBox.setMessage("We're actually already updating in" +
							" the background right now. If you have new" +
							" rides, they should show up very soon!");
					msgBox.setPositiveButton("Ok",
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							return;
						}
					}	);
					msgBox.show();
					return;
				}
				mUpdatingNow = true;
				new CurrTask().execute();  // Request the update
			}
		});
		
        /* Launch GPS Location Updater */
		Intent i = new Intent(this, GTBLocationManager.class);
		this.startService(i);
    }
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if(mProgBar != null && mProgBar.isShowing())
			mProgBar.cancel();
		
		if(mCDbHelper != null && !mCDbHelper.isClosed())
			mCDbHelper.close();
		
		if(mGDbHelper != null && !mGDbHelper.isClosed())
			mGDbHelper.close();
	}
	
	/* Create and launch the background threads that periodically send the
	 * server any pending updates or retrieve newly assigned patrons to the
	 * respective van.
	 */
	private void createBackgroundThreads()
	{
		(new Thread (new Runnable()
        {
		    public void run()
	        {
		    	Log.v(TAG, "Spawning background thread for updates.");
		    	Looper.prepare();
		    	
    			for(;;)
    			{
    				if (!mUpdatingNow)
    				{
    					mUpdatingNow = true;
    					new CurrUpdtTask().execute();
    					/* updatingNow = false is set on onPostExecute
    					 * or onCancelled, depending on what happens.
    					 */
    				}
    				try
    				{
    					Thread.sleep(60000);
					} catch (InterruptedException ex)
					{
						/* If we update slightly more often than
						 * 60 seconds it's ok.
						 */
					}
				}
	        }
	    })).start();

        (new Thread (new Runnable()
        {
        	public void run()
	        {
        		Log.v(TAG,
		    			"Launching background thread for pending updates.");
		    	Looper.prepare();
		    	
				for(;;)
				{
					if(!mUpdatingNow)
					{
						mUpdatingNow = true;
						new SendUpdatesTask().execute();
    					/* updatingNow = false is set on onPostExecute
    					 * or onCancelled, depending on what happens.
    					 */
					}
					try
					{
						Thread.sleep(75000);
					} catch (InterruptedException ex)
					{
						/* If we update slightly more often than
						 * 75 seconds it's ok.
						 */
					}
				}
    		} // Execute background update every 75 seconds
    	})).start();
	}

    public boolean onOptionItemSelected(MenuItem menu){
		return super.onOptionsItemSelected(menu);
    }

    private void updateList()
    {
    	if(Thread.currentThread().equals(mainThread))
    	{
	        mVp = (ViewPager)findViewById(R.id.ridelist_pageview);
	        m_GFPA = new GTBAdapter(getSupportFragmentManager());
	        mVp.setAdapter(m_GFPA);
    	}
    	else
    	{
    		Log.v(TAG, "Passing list update over to MT");
    		mainHandler.post( new Runnable()
    		{
    			
    			public void run()
    			{
    				updateList();
    			}
    		});
    	}
    }

	public void addToDb(PatronList list)
	{
		mGDbHelper.open();
		// For each patron in the patron list, add
		for (PatronInfo patron : list.getPatronList())
			   mGDbHelper.createPatron(patron.toByteArray(),
					   patron.getPid(), patron.getStatus());
		mGDbHelper.close();
	}

   /* Used to populate the fragments */
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

   /* The fragment */
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

	   /* Populating the view */
	   public View onCreateView(LayoutInflater inflater, ViewGroup container,
			   Bundle savedInstanceState)
	   {
		   Log.v(TAG, "ArrayListFragment: onCreateView");
		   //m_ALFGDbHelper.open();
		   //TextView aTV = null;
		   View v = null;
		   if (mNum == 0)
		   {
			   Log.v(TAG, "onCreateView: Current: mNum = " + mNum);
			   v = inflater.inflate(R.layout.openrides, container, false);
			   //aTV = (TextView)v.findViewById(R.id.openrides_title);
			   //aTV.setText(R.string.openrides_title);
		   }
		   else/* if (mNum == 1)*/
		   {
			   Log.v(TAG, "onCreateView: Closed: mNum = " + mNum);
			   v = inflater.inflate(R.layout.closedrides, container, false);
			   //aTV = (TextView)v.findViewById(R.id.closedrides_title);
			   //aTV.setText(R.string.closedrides_title);
		   }
		   getActivity().getSupportFragmentManager().beginTransaction().
		   		replace(R.id.fragrideslist, new ArrayListFragment());
		   //((RelativeLayout)aTV.getParent()).removeView(aTV);
		   Log.v(TAG, "onCreateView: returning");
		   //m_ALFGDbHelper.close();
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
				// PID is stored as the first sequence of numbers on each line
				Pattern pPidRegex = Pattern.compile("\\d* ");
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

	   /* Never used/called, can't long press on fragment */
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

	   public ArrayListFragment setNum(int nNum)
	   {
		   mNum = nNum;
		   return this;
	   }

	/* Works, but does not update */
	public void updateView()
	   {
		   Log.v(TAG, "ArrayListFragment: updateView");
		   //ListAdapter aVAA = getListAdapter();
		   /*if (aVAA == null)
		   {
			   if (mNum == 0)
				   aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(OPENRIDES));
			   else *//* if (mNum == 1)*/
				   /*aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(CLOSEDRIDES));
		   }
		   if (aVAA.equals(new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(OPENRIDES))) ||
						   aVAA.equals(new ArrayAdapter<String>(getActivity(),
								 R.layout.rides, populateRides(CLOSEDRIDES))))
		   */
		   ArrayAdapter<String> aVAA = null;
			   if (mNum == 0)
				   aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(OPENRIDES));
			   else /* if (mNum == 1)*/
				   aVAA = new ArrayAdapter<String>(getActivity(),
						   R.layout.rides, populateRides(CLOSEDRIDES));
			   setListAdapter(aVAA);
			   //((ArrayAdapter<String>)aVAA).notifyDataSetChanged();
			   //sself.mVp.invalidate();
	   }

	   public void initializeDb()
	   {
	       mDbHelper.open();
	       nGDbHelper.open();
	   }

	   public void addToDb(PatronList list){
		   for (PatronInfo patron : list.getPatronList())
			   m_ALFGDbHelper.createPatron(patron.toByteArray(),
					   patron.getPid(), patron.getStatus());
	   }

	   public String[] populateRides(int ridetype){
		   m_ALFGDbHelper.open();
		   PatronInfo[] vPI = m_ALFGDbHelper.fetchAllPatrons(ridetype);
		   m_ALFGDbHelper.close();
		   if (vPI.length == 0)
		   {
			   String[] msg = new String[1];
			   if (ridetype == OPENRIDES)
			   {
				   msg[0] = "No pending rides! Just chill";
			   }
			   else /* ridetype == CLOSEDRIDES */
			   {
				   msg[0] = "No closed rides...yet!";
			   }
			   Log.w(TAG, "No rides populated.");
			   return msg;
		   }
		   else
		   {
			   String[] msg = new String[vPI.length + 3];

			   for(int i = 0; i<vPI.length; i++)
			   {
				   msg[i] = vPI[i].getPid() + ": " + vPI[i].getTimeassigned() +
						   " " + vPI[i].getName() + " - " + vPI[i].getPickup();
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

   /*
    * Retrieves new rides assigned to this van
    */
   private class CurrTask extends AsyncTask<Void, Integer, Integer>
   {
	   static final int INCREMENT_PROGRESS = 20;
	   private SSLSocket aSock = null;
	   protected void onPreExecute()
	   {

		   mProgBar = new ProgressDialog(sself);
		   mProgBar.setCancelable(true);
		   mProgBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		   mProgBar.setMessage("Establishing Connection with server...");
		   mProgBar.show();
		   /* Sleep background thread for 30 seconds because
		    * we are forcing an update
		    */
		   //mBkgdCurrRunnable.sleep(30000);
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
		  try
		  {
				if(aSock != null)
					aSock.close();
			} catch (IOException e)
			{
			}
		  mProgBar.dismiss();

		  /* this, right here, updates the frag */
		  mVp = (ViewPager)findViewById(R.id.ridelist_pageview);
	      m_GFPA = new GTBAdapter(getSupportFragmentManager());
	      mVp.setAdapter(m_GFPA);
	   }

		public int retrieveRides()
		{
			Request aPBReq = null;
			Response aPBRes = null;
			GtBSSLSocketFactoryWrapper aSSLSF = null;
			try {
				aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
			Log.v(TAG, "Getting Patrons");

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
					aSock = aSSLSF.createSSLSocket(sself);
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
					if(aSock != null)
						aSock.close();
					aSock = aSSLSF.createSSLSocket(sself);
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
					aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
						if(aSock != null)
							aSock.close();
						aSSLSF.forceReHandshake(sself);
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

				if(mGDbHelper == null)
					mGDbHelper = new GtBDbAdapter(sself);
				if(mCDbHelper == null)
					mCDbHelper = new CarsGtBDbAdapter(sself);
				mGDbHelper.open();
				mCDbHelper.open();
				/* We only want the server to send us new rides,
				 * so we send the set of pids we already have
				 */
				ArrayList<Integer> vRides = mGDbHelper.fetchAllPid();
				mGDbHelper.close();
				aPBReq = Request.newBuilder().
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
							aSSLSF.forceReHandshake(sself);
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
				publishProgress(INCREMENT_PROGRESS);
				InputStream aIS = aSock.getInputStream();
				byte[] vbuf = new byte[9];
				aIS.read(vbuf);  // Receive
				/* Handle messages smaller than 9 bytes;
				 * Bufs aren't terminated, so removes trailing 0s */
				int nsize = (vbuf.length - 1);
				for (; nsize>0; nsize--)
				{
					if(vbuf[nsize] == 0)
					{
						continue;
					}
					break;
				}
				/* Copy the received buf into an array of the
				 * correct size so parsing is successful
				 */
				byte[] vbuf2 = new byte[nsize + 1];
				for(int i = 0; i != nsize + 1; i++)
					vbuf2[i] = vbuf[i];
				vbuf = vbuf2;
				try
				{
					Response apbTmpSize = null;
					apbTmpSize = Response.parseFrom(vbuf);
					vbuf = new byte[apbTmpSize.getNRespId()];
					if(aSock.isConnected())
					{
						aIS.read(vbuf);
						aPBRes = Response.parseFrom(vbuf);
						publishProgress(INCREMENT_PROGRESS);
						
						Log.v(TAG, "Response Buffer:");
						Log.v(TAG, TextFormat.shortDebugString(aPBRes));
						Log.v(TAG, "PatronList Buffer: ");
						Log.v(TAG, TextFormat.shortDebugString(
								aPBRes.getPlPatronList()));
						addToDb(aPBRes.getPlPatronList());
						Log.v(TAG, "Added to DB");
					}
					else
					{
						exceptionalMessage =
							"Our connection to the server was interrupted." +
							" :( Try again soon.";
						cancel(true);
						while(isCancelled());
					}
				} catch (InvalidProtocolBufferException e)
				{
					e.printStackTrace();
					String tmp = "";
					for(int i = 0; i<vbuf.length; i++)
						tmp = tmp + vbuf[i] + " ";
					Log.w(TAG, "Buffer Received: " + vbuf.length + " bytes : "
						+ tmp);
					e.printStackTrace();
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			return 0;
		}
   }

   /*
    * Retrieves new rides assigned to this van
    */
   private class CurrUpdtTask extends AsyncTask<Void, Integer, Integer>
   {
	   private SSLSocket aSock;
	   @Override
	   protected Integer doInBackground(Void... params)
	   {
		   return retrieveBackgroundRides();
	   }

	   protected void onPostExecute(Integer res)
	   {
		   /* this, right here, updates the frag */
			  mVp = (ViewPager)findViewById(R.id.ridelist_pageview);
		      m_GFPA = new GTBAdapter(getSupportFragmentManager());
		      mVp.setAdapter(m_GFPA);
	   }

	   public int retrieveBackgroundRides()
	   {
			Request aPBReq = null;
			Response aPBRes = null;
			GtBSSLSocketFactoryWrapper aSSLSF = null;
			try {
				aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
			Log.v(TAG, "Getting Patrons");

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
					aSock = aSSLSF.createSSLSocket(sself);
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
					if(aSock != null)
						aSock.close();
					aSock = aSSLSF.createSSLSocket(sself);
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
					aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
						if(aSock != null)
							aSock.close();
						aSSLSF.forceReHandshake(sself);
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

				if(mGDbHelper == null)
					mGDbHelper = new GtBDbAdapter(sself);
				if(mCDbHelper == null)
					mCDbHelper = new CarsGtBDbAdapter(sself);
				mGDbHelper.open();
				mCDbHelper.open();
				/* We only want the server to send us new rides,
				 * so we send the set of pids we already have
				 */
				ArrayList<Integer> vRides = mGDbHelper.fetchAllPid();
				mGDbHelper.close();
				aPBReq = Request.newBuilder().
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
							aSSLSF.forceReHandshake(sself);
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
				publishProgress(INCREMENT_PROGRESS);
				InputStream aIS = aSock.getInputStream();
				byte[] vbuf = new byte[9];
				aIS.read(vbuf);  // Receive
				/* Handle messages smaller than 9 bytes;
				 * Bufs aren't terminated, so removes trailing 0s */
				int nsize = (vbuf.length - 1);
				for (; nsize>0; nsize--)
				{
					if(vbuf[nsize] == 0)
					{
						continue;
					}
					break;
				}
				/* Copy the received buf into an array of
				 * the correct size so parsing is successful
				 */
				byte[] vbuf2 = new byte[nsize + 1];
				for(int i = 0; i != nsize + 1; i++)
					vbuf2[i] = vbuf[i];
				vbuf = vbuf2;
				try
				{
					Response apbTmpSize = null;
					apbTmpSize = Response.parseFrom(vbuf);
					vbuf = new byte[apbTmpSize.getNRespId()];
					if(aSock.isConnected())
					{
						aIS.read(vbuf);
						aPBRes = Response.parseFrom(vbuf);
						publishProgress(INCREMENT_PROGRESS);
						
						Log.v(TAG, "Response Buffer:");
						Log.v(TAG, TextFormat.shortDebugString(aPBRes));
						Log.v(TAG, "PatronList Buffer: ");
						Log.v(TAG, TextFormat.shortDebugString(
								aPBRes.getPlPatronList()));
						addToDb(aPBRes.getPlPatronList());
						Log.v(TAG, "Added to DB");
					}
					else
					{
						exceptionalMessage =
							"Our connection to the server was interrupted." +
							" :( Try again soon.";
						cancel(true);
						while(isCancelled());
					}
				} catch (InvalidProtocolBufferException e)
				{
					e.printStackTrace();
					String tmp = "";
					for(int i = 0; i<vbuf.length; i++)
						tmp = tmp + vbuf[i] + " ";
					Log.w(TAG, "Buffer Received: " + vbuf.length + " bytes : "
						+ tmp);
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			return 0;
		}

		protected void onCancelled()
		{
			mProgBar.dismiss();
			if(exceptionalMessage.compareTo("") != 0)
			{
				AlertDialog.Builder msgBox = new AlertDialog.Builder(sself);
				msgBox.setMessage(exceptionalMessage + "\n\n Would you" +
						" like to continue without a connection to the" +
						" server? This will be much more annoying " +
						"because you will be asked this question every time" +
						" we need to connect to the server.");
				msgBox.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						return;
					}
				}	);
				msgBox.setNegativeButton("No",
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						new AlertDialog.Builder(sself).
							setMessage("Sorry for the inconvenience. " +
									"GUARD the Bridge is now exiting.");
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
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

	private class SendUpdatesTask extends AsyncTask<Void, Integer, Integer>
	{
		private SSLSocket aSock = null;
		/* private boolean receivedErrors = false; */
		
		@Override
		protected Integer doInBackground(Void... params)
		{
			return sendBackgroundRides();
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
			
			if(mGDbHelper == null)
				mGDbHelper = new GtBDbAdapter(sself);
			mGDbHelper.open();
			/* Remove successful updates from pending list */
			mGDbHelper.removePendingUpdatesOnSuccess();
			mGDbHelper.close();
			
			/* this, right here, updates the frag */
			mVp = (ViewPager)findViewById(R.id.ridelist_pageview);
			m_GFPA = new GTBAdapter(getSupportFragmentManager());
			mVp.setAdapter(m_GFPA);

			/* I'm not sure we want a message to be displayed on success */
			/*
			if(receivedErrors)
			{
				AlertDialog.Builder msgBox = new AlertDialog.Builder(sself);
				msgBox.setMessage("We updated the patron information, " +
							"but something is wrong. Please look at the" +
							"error messages to see if you can fix them.");
				msgBox.setPositiveButton("Ok",
				 	new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						return;
					}
				}	);
				msgBox.show();
			}
			*/
		}

		public int sendBackgroundRides()
		{
			if(mGDbHelper == null)
				mGDbHelper = new GtBDbAdapter(sself);
			mGDbHelper.open();
			
			Request[] vReqs = mGDbHelper.fetchAllRequests();
			mGDbHelper.close();

			Request aPBReq = null;
			Response aPBRes = null;
			GtBSSLSocketFactoryWrapper aSSLSF = null;
			try
			{
				aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
			Log.v(TAG, "Sending Patrons");

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
					aSock = aSSLSF.createSSLSocket(sself);
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
					if(aSock != null)
						aSock.close();
					aSock = aSSLSF.createSSLSocket(sself);
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
					aSSLSF = new GtBSSLSocketFactoryWrapper(sself);
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
						if(aSock != null)
							aSock.close();
						aSSLSF.forceReHandshake(sself);
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
				if(mCDbHelper == null)
					mCDbHelper = new CarsGtBDbAdapter(sself);
				mCDbHelper.open();
				Request.Builder aReqBuilder = Request.newBuilder().
						setNReqId(4).
						setSReqType("UPDT").
						setNCarId(mCDbHelper.getCar()).
						setNParams(0, vReqs.length);
				Patron.PatronList.Builder aPLBuild = Patron.PatronList.
						newBuilder();
				mCDbHelper.close();
						
				/* Combine all requests */
				for(int idx = 0; idx < vReqs.length; ++idx)
				{
					Patron.PatronList currPat = vReqs[idx].getPlPatronList();
					for(int i = 0; i < currPat.getPatronCount(); ++i)
						aPLBuild.addPatron(currPat.getPatron(i));
				}
				aPBReq = aReqBuilder.
						setPlPatronList(aPLBuild.build()).
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
							aSSLSF.forceReHandshake(sself);
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
				byte[] vbuf = new byte[9];
				aIS.read(vbuf);  // Receive
				/* Handle messages smaller than 9 bytes; Bufs aren't
				 * terminated, so removes trailing 0s
				 */
				int nsize = (vbuf.length - 1);
				for (; nsize>0; nsize--)
				{
					if(vbuf[nsize] == 0)
					{
						continue;
					}
					break;
				}
				/* Copy the received buf into an array of the correct size
				 * so parsing is successful
				 */
				byte[] vbuf2 = new byte[nsize + 1];
				for(int i = 0; i != nsize + 1; i++)
					vbuf2[i] = vbuf[i];
				vbuf = vbuf2;
				try
				{
					Response apbTmpSize = null;
					apbTmpSize = Response.parseFrom(vbuf);
					vbuf = new byte[apbTmpSize.getNRespId()];

					if(aSock.isConnected())
					{
						aIS.read(vbuf);
						aPBRes = Response.parseFrom(vbuf);
						
						Log.v(TAG, "Response Buffer:");
						Log.v(TAG, TextFormat.shortDebugString(aPBRes));
						
						mGDbHelper.open();
						mGDbHelper.addUpdateError(aPBRes.getNResAdd(0),
								aPBRes.getSResAdd(0));
						mGDbHelper.close();
						/* receivedErrors = true; */
						Log.v(TAG, "Added errors to DB");
					}
					else
					{
						exceptionalMessage =
							"Our connection to the server was interrupted." +
							" :( Try again soon.";
						cancel(true);
						while(isCancelled());
					}
				} catch (InvalidProtocolBufferException e)
				{
					e.printStackTrace();
					String tmp = "";
					for(int i = 0; i<vbuf.length; i++)
						tmp = tmp + vbuf[i] + " ";
					Log.w(TAG, "Buffer Received: " + vbuf.length + " bytes : "
						+ tmp);
					e.printStackTrace();
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			return 0;
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
		}
   }
}