package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LogintoBridge extends ListActivity {
	private EditText mNetIdText;
    private EditText mAuthText;
    private String mCarNum;
    private CarsGtBDbAdapter mDbHelper;
    private TLSGtBDbAdapter mGDbHelper;
    private static final int CarNum_SELECT=0;
    private LogintoBridge self;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.self = this;
		
		//Re-establish connections to databases
		mDbHelper = new CarsGtBDbAdapter(this);
        mDbHelper.open();
        mGDbHelper = new TLSGtBDbAdapter (this);
        mGDbHelper.open();
        
        setContentView(R.layout.cars);
        setTitle(R.string.app_name);

        TextView carnumtext = (TextView) findViewById(R.id.carnum);
        carnumtext.setClickable(true);//Sets the text to be clickable
        
        carnumtext.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(self, CarNumList.class);
            	startActivityForResult(i, CarNum_SELECT);
            }

        });
	}
	
	 protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	        super.onActivityResult(requestCode, resultCode, intent);
	        System.out.println("On Return");
	        
	        setContentView(R.layout.main);
	        setTitle(R.string.app_name);
	        
	        TextView carnumtext = (TextView) findViewById(R.id.carnum);
	        carnumtext.setClickable(true);//Sets the text to be clickable
	        
	        mAuthText = (EditText) findViewById(R.id.authkey);
	        mNetIdText = (EditText) findViewById(R.id.netid);
	        
	        Button loginButton = (Button) findViewById(R.id.login);
	        
	        
	        loginButton.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) {
	            	int retval;
	            	if((retval = sendAuthCheck(mNetIdText, mAuthText, mCarNum)) != 0){
	            		dealwitherrors(retval);
	            	}
	            	else{
		                Intent i = new Intent (self, GuardtheBridge.class);
		                startActivity(i);
	            	}
	            }

	        });
	        mCarNum = Integer.toString(mDbHelper.getCar());
	        System.out.println("Ret Car Number: " + mCarNum);
	        TextView showcarnum = (TextView)findViewById(R.id.showcarnum);
	        showcarnum.setText("You Selected Car Number: " + mCarNum);
	        
	        carnumtext.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) {
	            	Intent i = new Intent(self, CarNumList.class);
	            	startActivityForResult(i, CarNum_SELECT);
	            }

	        });
	    }
	
	public int sendAuthCheck(EditText netid, EditText authcode, String carnum){
		SSLSession sslconn;
		Request aPBReq;
		Response aPBRes;
		GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper();
		
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
		try {
		System.out.println("Connected to: " + aSock.getInetAddress().getCanonicalHostName() + " on Port: " + aSock.getPort());
		System.out.println("Local Binding is on: " + aSock.getLocalAddress().getCanonicalHostName() + " on Port: " + aSock.getLocalPort());
		OutputStream aOS = aSock.getOutputStream();
		aPBReq = Request.newBuilder().
				setNReqId(2).
				setSReqType("CARS").
				build();
		aPBReq.writeTo(aOS);
		aOS.close();
		InputStream aIS = aSock.getInputStream();
		aPBRes = Response.parseFrom(aIS);
		aIS.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		String key = "AUTH";
		MessageDigest hash = null;
		byte[] hashstring;
		String stringcat;
		System.out.println("Nightly Key: " + authcode.getText().toString());
		System.out.println("NetID: " + netid.getText().toString());
		System.out.println("Car Number: " + carnum);
		try {
			/*send = new Socket(myserver, 4680);
			
			out = send.getOutputStream();
			out.write(key.getBytes());*/

			hash = MessageDigest.getInstance("SHA-256");
			stringcat = netid.getText().toString() + authcode.getText().toString() + carnum;
			hashstring = hash.digest(stringcat.getBytes());
			System.out.println("Hash: " + hashstring.length);
			//out.write(hashstring);
			
			byte[] accepted = new byte[1];
			/*in = send.getInputStream();
			in.read(accepted);*/
			accepted = "".getBytes();
			return Integer.parseInt(Byte.toString(accepted[0]));
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			System.out.println("NoSuchAlgo");
			return -3;
		}
	}
	
	public void dealwitherrors(int retval){
		switch (retval){
		case -1:
			break;
		case -2:
			break;
		case -3:
			break;
		case -4:
			break;
		default:
			break;
		}
	}

}
