package androidapp.GuardtheBridge;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
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
    private GtBDbAdapter mDbHelper;
    private static final int CarNum_SELECT=0;
    private LogintoBridge self;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.self = this;
		mDbHelper = new GtBDbAdapter(this);
        mDbHelper.open();

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
                setResult(RESULT_OK);
                finish();
            }

        });
        
        carnumtext.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(self, CarNumList.class);
            	startActivityForResult(i, CarNum_SELECT);
            }

        });
	}
	
	public int sendAuthCheck(EditText netid, EditText authcode, String carnum){
		byte[] addr = "192.168.2.25".getBytes();
		InetAddress server = null;
		Socket send;
		OutputStream out;
		MessageDigest hash = null;
		byte[] hashstring;
		String stringcat;
		try {
			server.getByAddress(addr);
		} catch (UnknownHostException e) {
			return -1;
		}
		try {
			send = new Socket(server, 4680);
		} catch (IOException e) {
			return -2;
		}
		try {
			out = send.getOutputStream();
		} catch (IOException e) {
			return -3;
		}
		try {
			hash = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		stringcat = netid.getText().toString() + authcode.getText().toString() + carnum;
		hashstring = hash.digest(stringcat.getBytes());
		try {
			out.write(hashstring);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public void dealwitherrors(int retval){
		switch (retval){
		case -1:
			break;
		case -2:
			break;
		case -3:
			break;
		default:
			break;
		}
	}

}
