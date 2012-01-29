package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronList;

public class GuardtheBridge extends ListActivity {
    /** Called when the activity is first created. */
	private static final int CarNum_SELECT=0;
	private GtBDbAdapter mDbHelper;
    private TLSGtBDbAdapter nGDbHelper;
    private GtBSSLSocketFactoryWrapper m_sslSFW;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activelist);
        Intent i = new Intent(this, LogintoBridge.class);
    	startActivityForResult(i, CarNum_SELECT);
    }
    
    public boolean onOptionItemSelected(MenuItem menu){
		return super.onOptionsItemSelected(menu);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        m_sslSFW = new GtBSSLSocketFactoryWrapper(this);
        initializeDb();
        try {
			retrieveRides();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        populateRides();
    }
    
    public void initializeDb(){
	   mDbHelper = new GtBDbAdapter(this);
	   mDbHelper.open();
	   nGDbHelper = new TLSGtBDbAdapter (this);
       nGDbHelper.open();
    }
    
   public void retrieveRides() throws IOException{
	   Request aPBReq = Request.newBuilder().
			   setNReqId(0).
			   setSReqType("CURR").
			   build();
	   SSLSocket sslSock = m_sslSFW.getSSLSocket();
	   aPBReq.writeTo(sslSock.getOutputStream());
	   /*Socket send;
	   OutputStream out = null;
	   InputStream in;
	   String myserver = "empathos.dyndns.org";*/
	   
	   String key = "CURR";
	   //int numbytes, results;
	   byte[] currrides;
	   //TODO: DH???
	   /*send = new Socket(myserver, 4680);
	   out = send.getOutputStream();
	   out.write(key.getBytes());
	   in = send.getInputStream();*/
	   /*numbytes = in.read();
	   currrides = new byte[numbytes];*/
	   
	   addToDb(Response.parseFrom(sslSock.getInputStream()).getPlPatronList());
	   
	   
	   /*results = in.read();//Number of results
	   while(results-- > 0){//Loop for each entry
		   byte[][] row = new byte[8][];//Array of bytes - each index contains data for it's column, respectfully
		   for (int i = 0; i<8; i++){//Per column
			   numbytes = in.read(); //Number of bytes coming
			   currrides = new byte[numbytes];//Initialize byte array to hold column data
			   in.read(currrides);//Store incoming data
			   row[i] = currrides;//
			   }
		   addToDb(row);//Save it
		   }
	   in = null;
	   out = null;*/
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
		   mDbHelper.createPatron(patron.toByteArray(), patron.getPid());
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