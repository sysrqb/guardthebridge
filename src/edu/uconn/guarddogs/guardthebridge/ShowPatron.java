package edu.uconn.guarddogs.guardthebridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;

public class ShowPatron extends Activity {
	private GtBDbAdapter mGDbHelper;
	private Long mrowid;
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mGDbHelper = new GtBDbAdapter(this);
		mGDbHelper.open();
		setContentView(R.layout.showpatron);
		mrowid = (savedInstanceState == null) ? null :
			(Long)savedInstanceState.getSerializable(GtBDbAdapter.KEY_ROWID);
		if (mrowid == null)
		{
			Bundle bundle = getIntent().getExtras();
			mrowid = (bundle != null) ? bundle.getLong(GtBDbAdapter.KEY_ROWID) : null;
		}
		fillPatronInfo();
	}
	
	public void fillPatronInfo()
	{
		if (mrowid != null)
		{
			PatronInfo aPI = mGDbHelper.fetchPatron(mrowid + 1);
			if (aPI == null)
				return;
			TextView tvName = null;
			if(aPI.getName() != null){
				tvName = (TextView)findViewById(R.id.showpatron_nameVal);
				tvName.setText(" " + aPI.getName());
			}
			if(aPI.getPassangers() > 0){
				tvName = (TextView)findViewById(R.id.showpatron_passVal);
				tvName.setText(" " + Integer.toString(aPI.getPassangers()));
			}
			if(aPI.getPickup() != null)
			{
				tvName = (TextView)findViewById(R.id.showpatron_puVal);
				tvName.setText(" " + aPI.getPickup());
			}
			if(aPI.getDropoff() != null)
			{
				tvName = (TextView)findViewById(R.id.showpatron_doVal);
				tvName.setText(" " + aPI.getDropoff());
			}
			if(aPI.getTimetaken() != null)
			{
				tvName = (TextView)findViewById(R.id.showpatron_ttVal);
				tvName.setText(" " + aPI.getTimetaken());
			}
		}
	}

}
