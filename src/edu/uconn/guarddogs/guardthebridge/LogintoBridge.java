package edu.uconn.guarddogs.guardthebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;

public class LogintoBridge extends ListActivity {
	private static final String TAG = "LIB-GTBLOG";
	private EditText mNetIdText;
    private EditText mAuthText;
    private String mCarNum;
    private CarsGtBDbAdapter mDbHelper;
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
	        Log.v(TAG, "On Return");
	        
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
	        Log.v(TAG, "Ret Car Number: " + mCarNum);
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
		Request aPBReq;
		Response aPBRes;
		GtBSSLSocketFactoryWrapper aSSLSF = new GtBSSLSocketFactoryWrapper();
		
		SSLSocket aSock = aSSLSF.getSSLSocket();
		if(aSock.isClosed())
		{
			aSock = aSSLSF.createSSLSocket(this);
		}
		try {
			OutputStream aOS = aSock.getOutputStream();
			aPBReq = Request.newBuilder().
				setNReqId(2).
				setSReqType("AUTH").
				addSParams(netid.getText().toString()).
				addSParams(authcode.getText().toString()).
				addSParams(carnum).
				build();
			Log.v(TAG, "Number of Params: " + aPBReq.getSParamsCount());
			Log.v(TAG, "Nightly Key: " + authcode.getText().toString());
			Log.v(TAG, "NetID: " + netid.getText().toString());
			Log.v(TAG, "Car Number: " + carnum);
			Log.v(TAG, "Serialized Size: " + aPBReq.getSerializedSize());
			aOS.write(aPBReq.getSerializedSize());
			aPBReq.writeTo(aOS);
			aOS.close();
			InputStream aIS = aSock.getInputStream();
			byte[] vbuf = new byte[14];
			aIS.read(vbuf);
			aSock.close();
			aPBRes = Response.parseFrom(vbuf);
			aIS.close();
			return aPBRes.getNRespId();
		} catch (IOException e){
			e.printStackTrace();
			getConnFailedDialog("Connection to server could not be established. " + 
					"Please try again in a minute or call Dispatch.");
			return -2;
		}
	}
	
	public void dealwitherrors(int retval){
		switch (retval){
		case -1:
			getConnFailedDialog("I'm sorry, but please retype the authentication code." 
					+ "The one you entered could not be verified");
			break;
		case -2:
			getConnFailedDialog("I'm sorry, but please retype your NetID." 
					+ "The one you entered could not be verified");
			break;
		case -3:
			
		case -4:
			
		default:
			getConnFailedDialog("I'm sorry, but an unknown error occurred. " +
					"Please call dispatch/the supervisor if this persists.");
			break;
		}
	}
	
	private void getConnFailedDialog(String msg){
		Log.w(TAG, "Failed to Connect to server");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
           	    return;
           }
        });
		builder.show();
	}
}
