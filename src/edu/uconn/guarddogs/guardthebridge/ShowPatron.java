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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;

public class ShowPatron extends Activity {
	private GtBDbAdapter mGDbHelper;
	private Long mpid;
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mGDbHelper = new GtBDbAdapter(this);
		mGDbHelper.open();
		setContentView(R.layout.showpatron);
		mpid = (savedInstanceState == null) ? null :
			(Long)savedInstanceState.getSerializable(GtBDbAdapter.KEY_ROWID);
		if (mpid == null)
		{
			Bundle bundle = getIntent().getExtras();
			mpid = (bundle != null) ? bundle.getLong(GtBDbAdapter.KEY_ROWID) : null;
		}
		fillPatronInfo();
		
		TextView tvBack = (TextView) findViewById(R.id.showpatron_back);
		tvBack.setOnClickListener( new OnClickListener()
		{
			public void onClick(View v){
				setResult(RESULT_OK);
				finish();
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
