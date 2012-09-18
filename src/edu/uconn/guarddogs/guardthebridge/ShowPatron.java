/*
 * GUARD the Bridge
 * Copyright (C) 2012  Matthew Finkel <Matthew.Finkel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package edu.uconn.guarddogs.guardthebridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;

public class ShowPatron extends Activity {
	private static final int PATRON_EDIT = 101;
	private GtBDbAdapter mGDbHelper;
	private Long mpid;
	private ShowPatron self = null;
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mGDbHelper = new GtBDbAdapter(this);
		self = this;
		mGDbHelper.open();
		setContentView(R.layout.showpatron);
		mpid = (savedInstanceState == null) ? null :
			(Long)savedInstanceState.getSerializable(GtBDbAdapter.KEY_ROWID);
		if (mpid == null)
		{
			Bundle bundle = getIntent().getExtras();
			mpid = (bundle != null) ? bundle.getLong(GtBDbAdapter.KEY_ROWID) : null;
			if (mpid == null)
			{
				Intent i = new Intent (self, GuardtheBridge.class);
                startActivity(i);				
			}
				
		}
		fillPatronInfo();
		
		TextView tvBack = (TextView) findViewById(R.id.showpatron_back);
		tvBack.setOnClickListener( new OnClickListener()
		{
			public void onClick(View v){
				Intent i = new Intent (self, GuardtheBridge.class);
                startActivity(i);
			}
		});
		Button bEdit = (Button) findViewById(R.id.showpatron_edit);
		bEdit.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent i = new Intent (self, EditPatron.class);
				i.putExtra(GtBDbAdapter.KEY_ROWID, mpid);
                startActivityForResult(i, PATRON_EDIT);
			}
		});
	}
	
	public void fillPatronInfo()
	{
		if (mpid != null)
		{
			PatronInfo aPI = mGDbHelper.fetchPatron(mpid);
			if (aPI == null)
				return;
			TextView tvName = null;
			if(aPI.getName() != null){
				tvName = (TextView)findViewById(R.id.showpatron_nameVal);
				tvName.setText(" " + aPI.getName());
			}
			if(aPI.getPhone() != null){
				tvName = (TextView)findViewById(R.id.showpatron_phoneVal);
				tvName.setText(" " + aPI.getPhone());
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
			if(aPI.getRidecreated() != null)
			{
				tvName = (TextView)findViewById(R.id.showpatron_ttVal);
				tvName.setText(" " + aPI.getRidecreated());
			}
			if(aPI.getStatus() != null)
			{
				tvName = (TextView)findViewById(R.id.showpatron_statusVal);
				tvName.setText(" " + aPI.getStatus());
			}
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		fillPatronInfo();
	}

}
