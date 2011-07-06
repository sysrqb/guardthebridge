package androidapp.GuardtheBridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class GuardtheBridge extends ListActivity {
    /** Called when the activity is first created. */
	private static final int CarNum_SELECT=0;
	private GtBDbAdapter mDbHelper;
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
        initializeDb();
        retrieveRides();
    }
    
    public void initializeDb(){
	   mDbHelper = new GtBDbAdapter(this);
	   mDbHelper.open();    	
    }
    
   public void retrieveRides(){
	   Socket send;
		OutputStream out = null;
		InputStream in;
		String myserver = "empathos.dyndns.org";
		String key = "CURR";
		int numbytes, results;
		byte[] currrides;
		//TODO: DH???
		
		try{
			send = new Socket(myserver, 4680);
			
			out = send.getOutputStream();
			out.write(key.getBytes());
			
			in = send.getInputStream();
			numbytes = in.read();
			currrides = new byte[numbytes];
			
			results = in.read();//Number of results
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
			out = null;
		}catch (UnknownHostException e){
			//TODO: Check signal strength
		}catch (IOException e){
			//TODO: Same as above
		}
   }
   
   public void addToDb(byte[][] currrides){
	   //TODO: Add to SQLITE DB
   }
}