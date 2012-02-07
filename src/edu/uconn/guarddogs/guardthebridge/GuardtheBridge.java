package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import edu.uconn.guarddogs.guardthebridge.Communication.*;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;

public class GuardtheBridge extends ListActivity {
	private static final String TAG = "GTB";
	private CarsGtBDbAdapter mDbHelper;
	private GtBDbAdapter mGDbHelper;
    private TLSGtBDbAdapter nGDbHelper;
    private GtBSSLSocketFactoryWrapper m_sslSF;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activelist);
        m_sslSF = new GtBSSLSocketFactoryWrapper(this);
        initializeDb();
        try {
			retrieveRides();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    
   public void retrieveRides() throws IOException{
	   Request aPBReq = Request.newBuilder().
			   setNReqId(1).
			   setSReqType("CURR").
			   setNCarId(mDbHelper.getCar()).
			   build();
	   Log.v(TAG, "Request type: " + aPBReq.getSReqType());
	   Log.v(TAG, "Request ID: " + aPBReq.getNReqId());
	   Log.v(TAG, "Request Size: " + aPBReq.isInitialized());
	   Log.v(TAG, "SReqType = " + aPBReq.getSReqType() + " " + aPBReq.getSerializedSize());
	   SSLSocket aSock = m_sslSF.getSSLSocket();
	   if (aSock.isClosed())
		   aSock = m_sslSF.createSSLSocket(this);
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
	   addToDb(Response.parseFrom(vbuf).getPlPatronList());
	   Log.v(TAG, "Added to DB");
   	}
   
   public void addToDb(PatronList list){
	   //TODO: Add to SQLITE DB
	   /*String val[] = new String[9];
	   int val1[] = new int[2];
	   if (patron.hasName())
		   val[0] = patron.getName();
	   if (patron.hasPickup())
		   val[1] = patron.getPickup();
	   if (patron.hasDropoff())
		   val[2] = patron.getDropoff();
	   if (patron.hasPhone())
		   val[3] = patron.getPhone();
	   if (patron.hasStatus())
		   val[4] = patron.getStatus();
	   if (patron.hasNotes())
		   val[5] = patron.getNotes();
	   if (patron.hasTimetaken())
		   val[6] = patron.getTimetaken();
	   if (patron.hasTimeassigned())
		   val[7] = patron.getTimeassigned();
	   if (patron.hasTimedone())
		   val[8] = patron.getTimedone();
	   
	   if (patron.hasPassangers())
		   val1[0] = patron.getPassangers();
	   if (patron.hasPid())
		   val1[1] = patron.getPid();
	   
	   PatronInfo pI = new PatronInfo(val, val1);*/
	   for (Patron.PatronInfo patron : list.getPatronList())
		   mGDbHelper.createPatron(patron.toByteArray(), patron.getPid());
   }
   
   public void populateRides(){
	   /*Patron[] pL = mDbHelper.fetchAllPatrons();
	   int[] to = new int[]{R.id.nameVal, R.id.ttVal};
	   ArrayList<Map<String, String>> listmap = new ArrayList<Map<String, String>>(pL.length);
	   TreeMap<String, String> map = new TreeMap<String, String>();
	   String[] from = null;
	   for (int i = 0; i < pL.length; i++){
		   from = new String[]{pL[i].getName(), pL[i].getTimetaken()};
		   map.put(pL[i].getTimetaken(), Integer.toString(i));
		   listmap.add(map);
	   }
	   setListAdapter(new SimpleAdapter(this, listmap, R.id.list, from, to));*/
   }
}