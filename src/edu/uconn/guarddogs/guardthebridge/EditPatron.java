package edu.uconn.guarddogs.guardthebridge;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;


public class EditPatron extends Activity {
	private final String TAG = "EP-GTBLOG";
	private GtBDbAdapter mGDbHelper;
	private Long mrowid;
	private EditPatron self;
	private PatronInfo m_aPI;
	
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		self = this;
		mGDbHelper = new GtBDbAdapter(this);
		mGDbHelper.open();
		setContentView(R.layout.editpatron);
		mrowid = (savedInstanceState == null) ? null :
			(Long)savedInstanceState.getSerializable(GtBDbAdapter.KEY_ROWID);
		if (mrowid == null)
		{
			Bundle bundle = getIntent().getExtras();
			mrowid = (bundle != null) ? bundle.getLong(GtBDbAdapter.KEY_ROWID) : null;
		}
		fillPatronInfo();
		mGDbHelper.close();
		TextView tvBack = (TextView) findViewById(R.id.editpatron_back);
		tvBack.setOnClickListener( new OnClickListener()
		{
			public void onClick(View v){
				setResult(RESULT_OK);
				finish();
			}
		});
		
		Button bCancel = (Button) findViewById(R.id.editpatron_cancel);
		bCancel.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				setResult(RESULT_OK);
				finish();
			}
		});
		
		Button bSave = (Button) findViewById(R.id.editpatron_save);
		bSave.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v){
				self.savePatronInfo();
				setResult(RESULT_OK);
				finish();
			}
		});
	}
	
	private void fillPatronInfo()
	{
		if (mrowid != null)
		{
			m_aPI = mGDbHelper.fetchPatron(mrowid + 1);
			if (m_aPI == null)
				return;
			EditText evName = null;
			if(m_aPI.getName() != null){
				evName = (EditText)findViewById(R.id.editpatron_nameVal);
				evName.setText(" " + m_aPI.getName());
			}
			if(m_aPI.getPassangers() > 0){
				evName = (EditText)findViewById(R.id.editpatron_passVal);
				evName.setText(" " + Integer.toString(m_aPI.getPassangers()));
			}
			if(m_aPI.getPickup() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_puVal);
				evName.setText(" " + m_aPI.getPickup());
			}
			if(m_aPI.getDropoff() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_doVal);
				evName.setText(" " + m_aPI.getDropoff());
			}
			if(m_aPI.getTimetaken() != null)
			{
				evName = (EditText)findViewById(R.id.editpatron_ttVal);
				evName.setText(" " + m_aPI.getTimetaken());
			}
		}
	}
	
	private void savePatronInfo()
	{
		if (mrowid != null)
		{
			mGDbHelper.open();
			if (m_aPI == null)
				m_aPI = mGDbHelper.fetchPatron(mrowid + 1);
			
			int npass = 0;
			try {
				String spass = ((EditText)findViewById(R.id.editpatron_passVal)).getText().toString();
				stripWhiteSpace(spass);
				npass = Integer.parseInt(((EditText)findViewById(R.id.editpatron_passVal)).getText().toString().substring(1, 2)); //Puts space before number
			} catch (NumberFormatException e)
			{
				Log.w(TAG, "Passangers is not an int: " + ((EditText)findViewById(R.id.editpatron_passVal)).getText().toString() + " : " + npass);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("The Number of passagers you entered is invalid");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           	    return;
		           }
		        });
				builder.show();
			}
			PatronInfo aPI = PatronInfo.newBuilder(m_aPI).
					setName(((EditText)findViewById(R.id.editpatron_nameVal)).getText().toString()).
					setPassangers(npass).
					setPickup(((EditText)findViewById(R.id.editpatron_puVal)).getText().toString()).
					setDropoff(((EditText)findViewById(R.id.editpatron_doVal)).getText().toString()).
					setTimeassigned(((EditText)findViewById(R.id.editpatron_ttVal)).getText().toString()).
					build();
			
			Log.v(TAG, "Updating Patron: " + mrowid + ": " + mGDbHelper.updatePatron(mrowid, aPI.toByteArray(), aPI.getPid()));
			mGDbHelper.close();
		}
	}
	
	private String stripWhiteSpace(String in)
	{
		return in;
	}
}
