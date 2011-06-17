package androidapp.GuardtheBridge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    private static final int CarNum_SELECT=0;
    private LogintoBridge self;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.self = this;
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
	        System.out.println("On Return");
	        
	        setContentView(R.layout.main);
	        setTitle(R.string.app_name);
	        
	        TextView carnumtext = (TextView) findViewById(R.id.carnum);
	        carnumtext.setClickable(true);//Sets the text to be clickable
	        
	        
	        Button loginButton = (Button) findViewById(R.id.login);
	        
	        
	        loginButton.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) {
	            	int retval;
	            	if((retval = sendAuthCheck(mNetIdText, mAuthText, mCarNum)) != 0){
	            		dealwitherrors(retval);
	            	}
	            	else{
		                setResult(RESULT_OK);
		                finish();
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
		Socket send;
		OutputStream out = null;
		ObjectInputStream in;
		String myserver = "empathos.dyndns.org";
		MessageDigest hash = null;
		byte[] hashstring;
		String stringcat;
		try {
			send = new Socket(myserver, 4680);
			out = send.getOutputStream();
			hash = MessageDigest.getInstance("SHA-256");
			stringcat = netid.getText().toString() + authcode.getText().toString() + carnum;
			hashstring = hash.digest(stringcat.getBytes());
			out.write(hashstring);
			in = new ObjectInputStream(send.getInputStream());
			String returncode = (String) in.readObject();
			return Integer.getInteger(returncode); //CarNumList uses parseInt, not sure if this makes a difference
		} catch (UnknownHostException e1) {
			System.out.println("UnknownHostException");
			return -1;
		}catch (IOException e) {
			System.out.println("IOException");
			return -2;
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			System.out.println("NoSuchAlgo");
			return -3;
		}catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("ClassNotFoundException: String class not found");
			return -4;
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
