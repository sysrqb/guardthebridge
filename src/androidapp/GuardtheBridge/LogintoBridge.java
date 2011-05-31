package androidapp.GuardtheBridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LogintoBridge extends Activity {
	private EditText mNetIdText;
    private EditText mAuthText;
    private String mCarNum;
    private GtBDbAdapter mDbHelper;
    private static final int CarNum_SELECT=0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mDbHelper = new GtBDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.main);
        setTitle(R.string.app_name);
        
        TextView carnumtext = (TextView) findViewById(R.id.carnum);
        carnumtext.setClickable(true);//Sets the text to be clickable
        
        Button loginButton = (Button) findViewById(R.id.login);
        
        
        loginButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	sendAuthCheck(mNetIdText, mAuthText, mCarNum);
                setResult(RESULT_OK);
                finish();
            }

        });
        
        carnumtext.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(this, CarNumList.class);
            	startActivityForResult(i, CarNum_SELECT);
            }

        });
	}

}
