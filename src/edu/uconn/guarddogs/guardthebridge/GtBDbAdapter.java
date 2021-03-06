/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Modifications By: 
 * Copyright (C) 2012 Matthew Finkel <Matthew.Finkel@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.uconn.guarddogs.guardthebridge;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class GtBDbAdapter {

    /*public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_ROWID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_CELL = "cell";
    public static final String KEY_RIDERS = "riders";
    public static final String KEY_FROMLOC = "fromloc";
    public static final String KEY_DROPOFF = "dropoff";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_STATUS = "status";
    public static final String KEY_TIMEDONE = "timedone";*/
	
	public static final String KEY_PATRON = "body";
    public static final String KEY_ROWID = "id";
    public static final String KEY_PID = "pid";
    public static final String KEY_STATUS = "status";
    public static final String KEY_UPDATESTATUS = "updtstatus";
    public static final String KEY_UPDATEERR = "updterror";

    private static final String TAG = "GtBDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private int nThreadSafe = 0;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_TABLE = "openrides";
    private static final String DATABASE_TABLE_CLOSED = "closedrides";
    private static final String DATABASE_TABLE_PENDING = "pendingrides";
    private static final String DATABASE_TABLE_UPDATEERR = "updateerrors";
    
    private static final String SAFE_DATABASE_CREATE =
        "create table " + DATABASE_TABLE + "( " + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_PID + " integer, " + KEY_PATRON + " blob, " + KEY_STATUS + " text);";
    
    private static final String SAFE_DATABASE_CLOSED_CREATE =
            "create table " + DATABASE_TABLE_CLOSED + "( " + KEY_ROWID + " integer primary key autoincrement, "
            + KEY_PID + " integer, " + KEY_PATRON + " blob, " + KEY_STATUS + " text);";
    
    private static final String SAFE_DATABASE_PENDING_CREATE =
            "create table " + DATABASE_TABLE_PENDING + "( " + KEY_ROWID + 
            " integer primary key autoincrement, " + KEY_PATRON + " blob, " 
            + KEY_UPDATESTATUS + " text);";
    
    private static final String SAFE_DATABASE_UPDATEERR_CREATE =
            "create table " + DATABASE_TABLE_UPDATEERR + "( " + KEY_ROWID + 
            " integer primary key autoincrement, " + KEY_PID + " integer, " 
            + KEY_UPDATEERR + " text);";

    private static final String DATABASE_NAME = "saferides";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(SAFE_DATABASE_CREATE);
            db.execSQL(SAFE_DATABASE_CLOSED_CREATE);
            db.execSQL(SAFE_DATABASE_PENDING_CREATE);
            db.execSQL(SAFE_DATABASE_UPDATEERR_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public GtBDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public GtBDbAdapter open() throws SQLException {
    		nThreadSafe++;
    		if (mDbHelper == null)
    			mDbHelper = new DatabaseHelper(mCtx);
    		mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
    	nThreadSafe--;
    	if (nThreadSafe == 0)
    		mDbHelper.close();
    	else
    		Log.v(TAG, "Another thread is still using this, delaying close until all threads done!");
    }


    public boolean isClosed()
    {
    	return (nThreadSafe == 0);
    }


    /**
     * Create a new patron index using the pid and info provided. If the patron is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createPatron(byte[] message, int pid, String status) {
    	if(!isNewPatron(pid))
    	{
    		if(status.compareToIgnoreCase("reassigned") == 0)
    			return (deletePatron((long)pid) ? 0 : -1);
    		else
    			deletePatron((long)pid);
    	}
    		
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATRON, message);
        initialValues.put(KEY_PID, pid);
        initialValues.put(KEY_STATUS, status);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the patron with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deletePatron(long pid) {

        return mDb.delete(DATABASE_TABLE, KEY_PID + "=" + pid, null) > 0;
    }
    
    public boolean deleteClosedPatron(long pid) {

        return mDb.delete(DATABASE_TABLE_CLOSED, KEY_PID + "=" + pid, null) > 0;
    }

    /**
     * Return a Cursor over the list of all patrons in the database
     * @param ridetype Type of ride to return; 0: Open, 1: Closed
     * @return Cursor over all patrons
     */
	public PatronInfo[] fetchAllPatrons(int ridetype)  
	{
		Log.v(TAG, "fetchAllPatrons");
		Cursor mCursor = null;
		if (ridetype == 0)
		{
			mCursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_PATRON,
					KEY_PID}, null, null, null, null, null);
		}
		else
			if (ridetype == 1)
			{
				mCursor = mDb.query(DATABASE_TABLE_CLOSED, new String[] {KEY_ROWID, KEY_PATRON,
						KEY_PID}, null, null, null, null, null);
			}
			else
				return null;
        Log.v(TAG, "mCursor = " + mCursor.getCount());
        if (mCursor.getCount() > 0)
        {
	        PatronInfo[] vPI = new PatronInfo[mCursor.getCount()];
	        mCursor.moveToFirst();
	        for(int i = 0; i<mCursor.getCount(); i++)
	        {
	        	try 
	        	{
	        		Log.v(TAG, "Index: " + i);
	        		vPI[i] = PatronInfo.parseFrom(mCursor.getBlob(1));
	        		mCursor.moveToNext();
	        	} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
	        }
	        return vPI;
        }
        return new PatronInfo[0];
    }
	
	public ArrayList<Integer> fetchAllPid()
	{
		Log.v(TAG, "fetchAllPid");
        Cursor mCursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_PATRON, KEY_PID}, 
        		null, null, null, null, null);
        Log.v(TAG, "mCursor = " + mCursor.getCount());
        if (mCursor.getCount() > 0)
        {
	        mCursor.moveToFirst();
	        ArrayList<Integer> vPid = new ArrayList<Integer>(mCursor.getCount());
	        for(int i = 0; i<mCursor.getCount(); i++)
	        {
	        	Log.v(TAG, "Index: " + mCursor.getInt(0));
	        	vPid.add(mCursor.getInt(2));
        		mCursor.moveToNext();
	        }
	        return vPid;
        }
        ArrayList<Integer> holder = new ArrayList<Integer>(1);
        holder.add(0);
        return holder;
	}

    /**
     * Return a Cursor positioned at the patron that matches the given rowId
     * 
     * @param pid id of patron to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if patron could not be found/retrieved
     */
	public PatronInfo fetchPatron(long pid) throws SQLException {
		Log.v(TAG, "Requesing " + pid + " patron");
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {
                    KEY_PATRON, KEY_ROWID}, KEY_PID + "=" + pid, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            
            if (mCursor.getCount() == 0)
            {
            	Log.v(TAG, "Requesing " + pid + " patron");
                mCursor =
                    mDb.query(true, DATABASE_TABLE_CLOSED, new String[] {
                            KEY_PATRON, KEY_ROWID}, KEY_PID + "=" + pid, null,
                            null, null, null, null);
                if (mCursor != null)
                    mCursor.moveToFirst();
                else
                	return null;
            }
	        PatronInfo patInfo = null;
	        byte[] patron = mCursor.getBlob(0);
	    	try {
				patInfo = PatronInfo.parseFrom(patron);
	    	} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        return patInfo;
        }
        return null;
    }
	
	public boolean isNewPatron(int pid) throws SQLException {

        Cursor aCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID}, KEY_PID + "=" + pid, null,
                    null, null, null, null);
        if (aCursor != null)
        	aCursor.moveToFirst();
        if (aCursor.getCount() > 0)
        	return false;
        aCursor = 
        mDb.query(true, DATABASE_TABLE_CLOSED, new String[] {
                KEY_ROWID}, KEY_PID + "=" + pid, null,
                null, null, null, null);
        if (aCursor != null) {
            aCursor.moveToFirst();
        }
        return (aCursor.getCount() > 0) ? false : true;
    }

    /**
     * Update the patron using the details provided. The patron to be updated is
     * specified using the pid, and it is altered to use the message values 
     * passed in
     * 
     * @param pid id of patron to update
     * @param status value to set patron status to
     * @param message value to set message to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updatePatron(byte[] message, int pid, int status) {
        ContentValues args = new ContentValues();
        args.put(KEY_PATRON, message);
        args.put(KEY_PID, pid);
        Log.v(TAG, "Updating patron " + pid + " " + status + " " + message);
        
        switch(status)
        {
        case 0:
        	return mDb.update(DATABASE_TABLE, args, KEY_PID + "=" + pid, null) > 0;
        case 1:
        	return mDb.update(DATABASE_TABLE_CLOSED, args, KEY_PID + "=" + pid, null) > 0;
        default:
        	return mDb.update(DATABASE_TABLE, args, KEY_PID + "=" + pid, null) > 0;
        }
        
    }
    
    public boolean isClosed(int pid)
    {
    	Cursor aCursor =
                mDb.query(true, DATABASE_TABLE_CLOSED, new String[] {
                        KEY_ROWID}, KEY_PID + "=" + pid, null,
                        null, null, null, null);
            if (aCursor != null)
            	aCursor.moveToFirst();
            return (aCursor.getCount() > 0) ? true : false;
    }
    
    public long setDone(long rowId, byte[] message, int pid)
    {
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATRON, message);
        initialValues.put(KEY_PID, pid);
        initialValues.put(KEY_STATUS, "done");
        
        if (!isClosed(pid))
        {
	        long insrtRow = mDb.insert(DATABASE_TABLE_CLOSED, null, initialValues);
	    	deletePatron(pid);
	    	return insrtRow;
        }
        else
        {
        	return mDb.update(DATABASE_TABLE_CLOSED, initialValues, KEY_PID + "=" + pid, null);
        }
    }
    
    public long setCanceled(long rowId, byte[] message, int pid)
    {
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATRON, message);
        initialValues.put(KEY_PID, pid);
        initialValues.put(KEY_STATUS, "cancelled");

        if (!isClosed(pid))
        {
	        long insrtRow = mDb.insert(DATABASE_TABLE_CLOSED, null, initialValues);
	    	deletePatron(pid);
	    	return insrtRow;
        }
        else
        {
        	return mDb.update(DATABASE_TABLE_CLOSED, initialValues, KEY_PID + "=" + pid, null);
        }
    }
    
    public long setRiding(long rowId, byte[] message, int pid)
    {
    	long nRetVal = 0;
    	ContentValues args = new ContentValues();
        args.put(KEY_PATRON, message);
        args.put(KEY_PID, pid);
        args.put(KEY_STATUS, "riding");

        if (!isClosed(pid))
        {
        	if ((nRetVal = mDb.update(DATABASE_TABLE, args, KEY_PID + "=" + pid, null)) > 0)
        	{
	    	 return nRetVal;
        	}
	    	return 0;
        }
        else
        {
        	if ((nRetVal = mDb.insert(DATABASE_TABLE, null, args)) > 0)
        	{
	    	 deleteClosedPatron(pid);
	    	 return nRetVal;
        	}
        }
        return 0;
    }
    
    public long setAssigned(long rowId, byte[] message, int pid)
    {
    	long nRetVal = 0;
    	ContentValues args = new ContentValues();
        args.put(KEY_PATRON, message);
        args.put(KEY_PID, pid);
        args.put(KEY_STATUS, "assigned");

        if (!isClosed(pid))
        {
        	if ((nRetVal = mDb.update(DATABASE_TABLE, args, KEY_PID + "=" + pid, null)) > 0)
        	{
	    	 return nRetVal;
        	}
	    	return 0;
        }
        else
        {
        	if ((nRetVal = mDb.insert(DATABASE_TABLE, null, args)) > 0)
        	{
	    	 deleteClosedPatron(pid);
	    	 return nRetVal;
        	}
        }
        return 0;
    }
    
    public long setWaiting(long rowId, byte[] message, int pid)
    {
    	long nRetVal = 0;
    	ContentValues args = new ContentValues();
        args.put(KEY_PATRON, message);
        args.put(KEY_PID, pid);
        args.put(KEY_STATUS, "waiting");

        if (!isClosed(pid))
        {
        	if ((nRetVal = mDb.update(DATABASE_TABLE, args, KEY_PID + "=" + pid, null)) > 0)
        	{
	    	 return nRetVal;
        	}
	    	return 0;
        }
        else
        {
        	if ((nRetVal = mDb.insert(DATABASE_TABLE, null, args)) > 0)
        	{
	    	 deleteClosedPatron(pid);
	    	 return nRetVal;
        	}
        }
        return 0;
    }
    
    public long setStatus(long rowId, byte[] message, int pid, String status)
    {
    	Log.v(TAG, "Setting Status for " + pid + ": " + status);
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATRON, message);
        initialValues.put(KEY_PID, pid);
        initialValues.put(KEY_STATUS, status);

        if (status.compareToIgnoreCase("done") == 0 || status.compareToIgnoreCase("cancelled") == 0)
        {
	        if (!isClosed(pid))
	        {
		        long insrtRow = mDb.insert(DATABASE_TABLE_CLOSED, null, initialValues);
		    	deletePatron(pid);
		    	return insrtRow;
	        }
	        else
	        {
		        if (status.compareToIgnoreCase("done") == 0)
		        {
		        	setDone(rowId, message, pid);
		        }
		        else if (status.compareToIgnoreCase("cancelled") == 0)
		        {
		        	setCanceled(rowId, message, pid);
		        }
	        }
	        return 0;
        }
        if (status.compareToIgnoreCase("waiting") == 0 ||
        	status.compareToIgnoreCase("riding") == 0 ||
        	status.compareToIgnoreCase("assigned") == 0)
        {
                
            if (isClosed(pid))
            {
            	long insrtRow = mDb.insert(DATABASE_TABLE, null, initialValues);
		    	deleteClosedPatron(pid);
            	if (status.compareToIgnoreCase("waiting") == 0)
		        {
		        	setWaiting(rowId, message, pid);
		        }
		        else if (status.compareToIgnoreCase("riding") == 0)
		        {
		        	setRiding(rowId, message, pid);
		        }
		        else if (status.compareToIgnoreCase("assigned") == 0)
		        {
		        	setRiding(rowId, message, pid);
		        }
		    	return insrtRow;
            }
            else
            {
            	if (status.compareToIgnoreCase("waiting") == 0)
		        {
		        	setWaiting(rowId, message, pid);
		        }
		        else if (status.compareToIgnoreCase("riding") == 0)
		        {
		        	setRiding(rowId, message, pid);
		        }
		        else if (status.compareToIgnoreCase("assigned") == 0)
		        {
		        	setRiding(rowId, message, pid);
		        }
            	return 0;
            }
        }
        return -1;
    }
    
    /**
     * Get the ROWID that correlates to PID
     */
    public int getROWID(long pid) throws SQLException 
    {
		Log.v(TAG, "Requesing row of patron " + pid);
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID},
            		KEY_PID + "=" + pid, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            
            if (mCursor.getCount() == 0)
            {
            	Log.v(TAG, "Requesing row of patron " + pid);
                mCursor =
                    mDb.query(true, DATABASE_TABLE_CLOSED, new String[] {KEY_ROWID},
                    		KEY_PID + "=" + pid, null,
                            null, null, null, null);
                if (mCursor != null)
                    mCursor.moveToFirst();
                else
                	throw new SQLException();
            }
	        int rowid = mCursor.getInt(0);
	        return rowid;
        }
        throw new SQLException();
    }
    
    /**
     * Get the ROWID that correlates to PID
     */
    public int getPID(long rowid) throws SQLException 
    {
		Log.v(TAG, "Requesing row of patron at " + rowid);
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_PID},
            		KEY_ROWID + "=" + rowid, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            
            if (mCursor.getCount() == 0)
            {
            	Log.v(TAG, "Requesing row of patron at " + rowid);
                mCursor =
                    mDb.query(true, DATABASE_TABLE_CLOSED, new String[] {KEY_PID},
                    		KEY_ROWID + "=" + rowid, null,
                            null, null, null, null);
                if (mCursor != null)
                    mCursor.moveToFirst();
                else
                	throw new SQLException();
            }
	        int pid = mCursor.getInt(0);
	        return pid;
        }
        throw new SQLException();
    }
    
    /**
     * Add pending update so that we can try to send it again later.
     * If the request is successfully added return the new rowId, 
     * otherwise return a -1 to indicate failure.
     * 
     * @param request The pending request
     * @return rowId or -1 if failed
     */
    public long addPendingUpdate(byte[] request)
    {
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATRON, request);
        initialValues.put(KEY_UPDATESTATUS, "pending");

        return mDb.insert(DATABASE_TABLE_PENDING, null, initialValues);
    }
    
    /**
     * Remove pending updates from DB if they were successfully updated
     * server-side.
     */
    public void removePendingUpdatesOnSuccessWithNoErrors()
    {
    	Request[] reqs = fetchAllRequests();
    	for(int i = 0; i < reqs.length; ++i)
    	{
    		Patron.PatronList aPL = reqs[i].getPlPatronList();
    		int pats = aPL.getPatronCount();
    		for(int idx = 0; i < pats; ++i)
    		{
    			int pid  = aPL.getPatron(idx).getPid();
    			/* If pid has no errors associated with it, then remove
    			 * it from the list.
    			 */
    			if(pidHasError(pid))
    					continue;
    			else
    			{
    		    	ContentValues initialValues = new ContentValues();
    		        initialValues.put(KEY_PID, pid);

    		        mDb.delete(DATABASE_TABLE_PENDING, KEY_PID + "=" + pid, null);
    			}
    		}
    	}
    }
    
    /**
     * Remove pending updates from DB if they were successfully updated
     * server-side.
     */
    public void removePendingUpdatesOnSuccess()
    {
    	mDb.delete(DATABASE_TABLE_PENDING, KEY_UPDATESTATUS + "='pending'", null);
	}
    
    /**
     * Return a Cursor over the list of all pending requests in the table
     * @return Cursor over all patrons
     */
	public Request[] fetchAllRequests()  
	{
		Log.v(TAG, "fetchAllRequests Now");
		Cursor mCursor = null;
		mCursor = mDb.query(DATABASE_TABLE_PENDING, new String[] {KEY_PATRON},
				null, null, null, null, null);
        Log.v(TAG, "mCursor = " + mCursor.getCount());
        if (mCursor.getCount() > 0)
        {
	        Request[] vReq = new Request[mCursor.getCount()];
	        mCursor.moveToFirst();
	        for(int i = 0; i<mCursor.getCount(); i++)
	        {
	        	try 
	        	{
	        		Log.v(TAG, "Index: " + i);
	        		vReq[i] = Request.parseFrom(mCursor.getBlob(0));
	        		mCursor.moveToNext();
	        	} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
	        }
	        return vReq;
        }
        return new Request[0];
    }
	
	/**
     * Add update error so that we can try to correct it.
     * If the insertion is successfully added return the new rowId, 
     * otherwise return a -1 to indicate failure.
     * 
     * @param pid The PID that generated the error
     * @param err The error
     * @return rowId or -1 if failed
     */
    public long addUpdateError(int pid, String err)
    {
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PID, pid);
        initialValues.put(KEY_UPDATESTATUS, err);

        return mDb.insert(DATABASE_TABLE_UPDATEERR, null, initialValues);
    }
    
    /**
     * Remove the update error
     * 
     * @param pid The PID that generated the error
     * @return rowId or -1 if failed
     */
    public boolean removeUpdateError(int pid)
    {
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PID, pid);

        return mDb.delete(DATABASE_TABLE_UPDATEERR, KEY_PID + "=" + pid, null) > 0;
    }
    
    /**
     * Return a Cursor over the list of all pending requests in the table
     * @return Cursor over all patrons
     */
	public String[] fetchAllErrors()  
	{
		Log.v(TAG, "fetchAllRequests Now");
		Cursor mCursor = null;
		mCursor = mDb.query(DATABASE_TABLE_UPDATEERR, new String[] {KEY_PID, KEY_UPDATEERR},
				null, null, null, null, null);
        Log.v(TAG, "mCursor = " + mCursor.getCount());
        if (mCursor.getCount() > 0)
        {
	        String[] vErr = new String[mCursor.getCount()];
	        mCursor.moveToFirst();
	        for(int i = 0; i<mCursor.getCount(); i++)
	        {
        		Log.v(TAG, "Index: " + i);
        		/* Message to be displayed to the user as "ROWID: <ERROR MESSAGE>"
        		 * ROWID will be the same number displayed next to the patron info 
        		 */
        		try
        		{
        			vErr[i] = Integer.toString(getROWID(mCursor.getInt(0))) 
        					+ ": " + mCursor.getString(1);
        		} catch (SQLException ex)
        		{
        			/* ROWID could not be found, skip this error message */
        			removeUpdateError(mCursor.getInt(0));
        		}
        		mCursor.moveToNext();
	        	
	        }
	        return vErr;
        }
        return new String[0];
    }
	
	public boolean pidHasError(int pid)
	{
		Log.v(TAG, "Does pid " + pid + " have an update error?");
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_UPDATEERR, new String[] {KEY_PID},
            		KEY_PID + "=" + pid, null,
                    null, null, null, null);

        if (mCursor.getCount() != 0)
        	return true;
        else
        	return false;
	}
	
}
