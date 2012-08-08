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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Looper;
import android.telephony.*;
import android.util.Log;

public class GtBSSLSocketFactoryWrapper {
	private static final String TAG = "SSF-GTBLOG";
	private String HOST = "192.168.5.29";
	private int PORT = 4680;
	private static SSLSocket m_sslSocket = null;
	private static SSLSocketFactory m_aSSF;
	private static KeyStore m_kstrust, m_kskey;
	private static SSLContext m_aSSLContext = null;
	private Context m_ctx = null;
	private boolean successfullyEstablishedConn = false;
	private boolean isCurrentSignalStrengthHigh = false;
	
	
	
	/** 
	 * Constructor - Empty
	 * If calling this, also call loadStores and createConnection
	 * to replicate affect of the main constructor.
	 */
	public GtBSSLSocketFactoryWrapper(){}

	/**
	 * Constructor - Sets up SSL connection with server.
	 * @param i_aCtx The current context that provides access 
	 *   to the builtin keystores.
	 * @throws GTBSSLSocketException If the exception can't be 
	 *   handled by propagation. This replaces another exception 
	 *   and contains a message that described the error that can
	 *   be displayed to the user so they can handle it.
	 * @throws UnrecoverableKeyException If the key can not be recovered
	 *   (Whatever that means)
	 * @throws KeyStoreException If initiating the KeyManager fails
	 * @throws NoSuchAlgorithmException If a required algorithm is 
	 *   not available
	 */
	public GtBSSLSocketFactoryWrapper(Context i_aCtx) throws 
			GTBSSLSocketException, UnrecoverableKeyException,
			KeyStoreException, NoSuchAlgorithmException, SignalException
	{

		m_ctx = i_aCtx;
		if(!isCurrentSignalStrengthHigh)
			setSignalStrengthListener();
		
		/* We currently have an open socket connection, use it */
		if(m_sslSocket != null && successfullyEstablishedConn)
		{
			if(m_sslSocket.getEnableSessionCreation())
			{
				return;
			}
			else
				createConnection();
		}
		/* We attempted to make a connection in the past but were
		 * unsuccessful. Attempt to establish the connection now 
		 * and handshake.
		 */
		else if(m_aSSLContext != null)
			createConnection();
		else
		{
			loadStores();
			createConnection();
		}
	}
	
	
	/**
	 * Load KeyStores and passphrase from res/raw/ directory.
	 * KeyStore is called gtbbks.
	 * Passphrase is contained in kspass. 
	 * @param ctx The current or a context that has access to the integrated
	 *   file structure. 
	 * @return The passphrase to decipher the KeyStore
	 * @throws GTBSSLSocketException If the actual exception that is thrown can
	 *   only be handled by human intervention. The message that is contained
	 *   when catching the exception can be logged or shown in a message to
	 *   the user.
	 */
	public String loadKeyStore(Context ctx) throws GTBSSLSocketException
	{
		DataInputStream a_ksis = null;
		KeyStore kskey = null;
		String kspass = "";
		
		try 
		{
			kskey = KeyStore.getInstance("BKS");
		} catch (KeyStoreException e) 
		{
			Log.e(TAG, "KeyStore could not be created as BKS!");
			throw new GTBSSLSocketException(
					"KeyStore could not be created as BKS!");
		}
		try 
		{
			a_ksis = new DataInputStream(ctx.getResources().
					openRawResource(R.raw.kspass));
			kspass = a_ksis.readLine();
		} catch (NotFoundException e) 
		{
			Log.e(TAG, "Can not find KeyStore password file!" +
					" Make sure it was packaged with this app!");
			throw new GTBSSLSocketException(
					"Can not find KeyStore password file!" +
					" Make sure it was packaged with this app!");
		} catch (IOException e) 
		{
			Log.e(TAG, "Can not read from input stream!" +
					" Unknown KeyStore password!");
			throw new GTBSSLSocketException("Can not read from input stream!" +
					" Unknown KeyStore password!");
		}
		try
		{
			try 
			{
				InputStream aIS = ctx.getResources().
						openRawResource(R.raw.gtbbks);
				kskey.load(aIS, kspass.toCharArray());
			} catch (CertificateException e) 
			{
				Log.e(TAG, "Failed to read Certificates!");
				throw new GTBSSLSocketException(
						"Failed to read Certificates!");
			} catch (NotFoundException e) 
			{
				Log.e(TAG, "Could not find KeyStore!" +
						" Make sure it was packaged with this app!");
				throw new GTBSSLSocketException(
						"Could not find KeyStore!" +
						" Make sure it was packaged with this app!");
			} catch (NoSuchAlgorithmException e) 
			{
				Log.e(TAG, "Can not use Certificate's algorithms!");
				throw new GTBSSLSocketException(
						"Can not use Certificate's algorithms!");
			} 
			//java.security.cert.Certificate aCertGDT1 = kskey.getCertificate("gdt1");
			//Log.i(TAG, "GDT1 PubKey Format: " + aCertGDT1.getPublicKey().getFormat());
			a_ksis.close();
			m_kskey = kskey;
		} /*catch(KeyStoreException e)
		{
			Log.e(TAG, "gdt1 KeyStore is not initialized!");
			throw new GTBSSLSocketException("gdt1 KeyStore is not initialized!");
		}*/catch (IOException e) 
		{
			Log.e(TAG, "Can not read from input stream!");
			throw new GTBSSLSocketException(
					"Can not read from input stream!");
		}
		return kspass;
	}
	
	/**
	 * Loads TrustedKeyStore from /res/raw/ directory.
	 * @param ctx
	 * @return The TrustManagerFactory containing the KeyStore that holds
	 *   all trusted certificate chains.
	 * @throws GTBSSLSocketException
	 */
	public TrustManagerFactory loadTrustKeyStore(Context ctx) throws 
		GTBSSLSocketException
	{
		DataInputStream a_tksis = 
				new DataInputStream(ctx.getResources().
						openRawResource(R.raw.tkspass));
		KeyStore kstrust;
		TrustManagerFactory tmf;
		
		try {
			kstrust = KeyStore.getInstance("BKS");
		
			String tkspass = a_tksis.readLine();
			try
			{
				kstrust.load(ctx.getResources().openRawResource(R.raw.trust),
						tkspass.toCharArray());
			} catch (CertificateException e) {
				Log.e(TAG, "Failed to read trust Certificates!");
				throw new GTBSSLSocketException(
						"Failed to read trust Certificates!");
			} catch (NotFoundException e) {
				Log.e(TAG, "Could not find Trust KeyStore!");
				throw new GTBSSLSocketException(
						"Could not find Trust KeyStore!");
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Can not use Trust Certificates' algorithms!");
				throw new GTBSSLSocketException(
						"Can not use Trust Certificates' algorithms!");
			}
				
			Log.i(TAG, "Avaiable KMF algorithms: " + 
					TrustManagerFactory.getDefaultAlgorithm());
			try {
				tmf = TrustManagerFactory.getInstance("X509");
			}  catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Can not use X509 algorithms in Trust KeyManager!");
				throw new GTBSSLSocketException(
						"Can not use X509 algorithms in Trust KeyManager!");
			}
			tmf.init(kstrust);
			Log.i(TAG, "Algorithm being used for TMF: " + tmf.getAlgorithm());
		} catch (KeyStoreException e) {
			Log.e(TAG, "Can not use Bouncycastle KeyStore!");
			throw new GTBSSLSocketException(
					"Can not use Bouncycastle KeyStore!");
		}catch (IOException e) {
			Log.e(TAG, "Can not read from input stream!");
			throw new GTBSSLSocketException("Can not read from input stream!");
		}
		
		try {
			a_tksis.close();
		} catch (IOException e) {
			Log.w(TAG, "Error closing DataStream");
			try {
				a_tksis.close();
			} catch (IOException ex) {
				Log.w(TAG, "Error closing DataStream - Second try.");
				ex.printStackTrace();
			}
		}			
		m_kstrust = kstrust;
		return tmf;
	}
	
	public void loadStores() throws GTBSSLSocketException, 
		UnrecoverableKeyException, KeyStoreException, 
		NoSuchAlgorithmException
	{
		Context ctx = m_ctx;
		TrustManagerFactory tmf = null;
		KeyManagerFactory kmf = null;
		KeyStore kskey = null;
		String kspass = "";
		
		kspass = loadKeyStore(ctx);
		tmf = loadTrustKeyStore(ctx);
		
		kskey = m_kskey;
		
		try {
			kmf = KeyManagerFactory.getInstance("X509");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Can not use X509 algorithms in KeyManager!");
			throw new GTBSSLSocketException(
					"Can not use X509 algorithms in KeyManager!");
		}
		
		
		/**
		 * Throws UnrecoverableKeyException, KeyStoreException and 
		 * NoSuchAlgorithmException
		 */
		kmf.init(kskey, kspass.toCharArray());
		
		SSLContext aSC = null;
		if (m_aSSLContext == null)
		{
			aSC = SSLContext.getInstance("TLSv1");
			try {
				aSC.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			} catch (KeyManagementException e) {
				Log.e(TAG, "Unknown failure from KeyManager!" +
						" Sorry I can't tell you what is actually wrong.");
				throw new GTBSSLSocketException(
						"Unknown failure from KeyManager!" +
						" Sorry I can't tell you what is actually wrong.");
			}
			SSLContext.setDefault(aSC);
			m_aSSLContext = aSC;
		}
	}
	
	/**
	 * Establish TCP connection over TLS with gtbserver. 
	 *   Loads KeyStores if necessary,creates socket, and performs
	 *   TLS handshake. If the handshake is successful then the connection
	 *   was successfully establish. If not, the method blocks and sleeps
	 *   for 25 seconds, trying again on wake up.This is attempted three (3)
	 *   times.
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws GTBSSLSocketException Lumps together exceptions that can not
	 * be handled by to user so we can fail nicely.
	 * @throws SignalException If we don't have a data connection
	 */
	public void createConnection() throws UnrecoverableKeyException, 
		KeyStoreException, NoSuchAlgorithmException, GTBSSLSocketException,
		SignalException
	{
		if (m_aSSLContext == null)
			loadStores();
		if(!haveDataConnection())
		{
			Log.w(TAG, "Insufficient Signal Strength");
			throw new SignalException("Insufficient Signal Strength");
		}
		SSLContext aSC = m_aSSLContext;
		SSLSocket aSS = null;
		
		Log.v(TAG, "Establishing Connection to server...");
		try {
			aSS = (SSLSocket)aSC.getSocketFactory().createSocket(HOST, PORT);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Failed to connect to host: " + HOST + ":" +
					PORT + "! " + e.getMessage());
			throw new GTBSSLSocketException("We couldn't make a connection.");
		} catch (IOException e) {
			Log.w(TAG, "IOException Thrown: " + e.getMessage());
			throw new GTBSSLSocketException("We couldn't make a connection.");
		}
		Log.v(TAG, "Connected to: " + 
				aSS.getInetAddress().getCanonicalHostName() 
				+ " on Port: " + aSS.getPort());
		Log.v(TAG, "Local Binding is on: " + 
				aSS.getLocalAddress().getCanonicalHostName() + " on Port: " 
				+ aSS.getLocalPort());
		Log.v(TAG, "Connection Established. Handshaking...");
		aSS.setUseClientMode(true);
		try {
			aSS.startHandshake();
			successfullyEstablishedConn = true;
			/* Resume sessions from now on */
			aSS.setEnableSessionCreation(false);
		} catch (IOException e)
		{
			Log.w(TAG, "Handshake Failed");
			successfullyEstablishedConn = false;
			try {
				aSS.startHandshake();
				successfullyEstablishedConn = true;
				aSS.setEnableSessionCreation(false);
			} catch (IOException ex)
			{
				Log.w(TAG, "Failed to establish connection!");
				successfullyEstablishedConn = false;
				//TODO
				// We need to back off and retry after some period of time!
				/*
				 * m_sslSocket = null;
				 * new GtBSSLSocketFactoryWrapper(i_aCtx);
				 */
				for(int i = 0; i<3; i++)
				{
					try
					{
						try 
						{
							Thread.sleep(25000);
						} catch (InterruptedException e1) {
							aSS.startHandshake();
							successfullyEstablishedConn = true;
							aSS.setEnableSessionCreation(false);
						}
						if(!aSS.getEnableSessionCreation())
						{
							aSS.startHandshake();
							successfullyEstablishedConn = true;
							aSS.setEnableSessionCreation(false);
						}
					} catch (IOException ex2)
					{
						successfullyEstablishedConn = false;
						continue;
					}
					break;
				}
			}
		}
		if(successfullyEstablishedConn)
			m_sslSocket = aSS;
	}
	
	
	/**
	 * IF socket is already establish but the handshake failed call
	 *   this method to start another handshake session.
	 *   
	 * This recreates the process of calling createConnection 
	 * @param i_aCtx
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws GTBSSLSocketException
	 * 
	 * @see CreateConnection
	 */
	public void forceReHandshake(Context i_aCtx) throws 
		UnrecoverableKeyException, KeyStoreException,
		NoSuchAlgorithmException, GTBSSLSocketException
	{
		try
		{
			Log.v(TAG, "Initiating rehandshake");
			if(m_sslSocket == null)
				try
			{
				m_sslSocket = createSSLSocket(i_aCtx);
			} catch(GTBSSLSocketException e)
			{
				throw e;
			}
				m_sslSocket.startHandshake();
				successfullyEstablishedConn = true;
				/* Resume sessions from now on */
				m_sslSocket.setEnableSessionCreation(false);
		} catch (IOException e)
		{
			successfullyEstablishedConn = false;
			Log.w(TAG, "Rehandshake Failure");
		}
	}
	
	/**
	 * Closes the current connection to the server and returns a new instance
	 *   of GtBSLLSocketFactoryWrapper.
	 * @param i_aCtx
	 * @return
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws GTBSSLSocketException
	 */
	public GtBSSLSocketFactoryWrapper getNewSSLSFW(Context i_aCtx) throws 
		UnrecoverableKeyException, KeyStoreException, 
		NoSuchAlgorithmException, GTBSSLSocketException
	{
		Log.i(TAG, "Re-Establishing Connection; from scratch.");
		try {
			m_sslSocket.close();
		} catch (IOException e) {
			Log.w(TAG, "getNewSSLSFW: Error while closing connection");
		}
		return new GtBSSLSocketFactoryWrapper(i_aCtx);
	}
	
	/**
	 * Establish another connection with server and handshake.
	 * If this is called, call forceReHandshake afterwards.
	 * @param i_aCtx
	 * @return
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws GTBSSLSocketException
	 */
	public SSLSocket createSSLSocket (Context i_aCtx) throws 
			UnrecoverableKeyException, KeyStoreException, 
			NoSuchAlgorithmException, GTBSSLSocketException
	{
		m_sslSocket = (SSLSocket) createSocket(null, 0, true);
		m_sslSocket.setUseClientMode(true);
		return m_sslSocket;
	}
	
	/**
	 * Create a new connection to the specified host.
	 * 
	 * Call haveDataConnection before use.
	 * @param host If null, falls back to HOST value
	 * @param port If 0, falls back to PORT
	 * @param autoClose Unused
	 * @return A new Socket or null on error.
	 */
	public Socket createSocket(String host, int port, boolean autoClose)
		throws GTBSSLSocketException
	{
		if(host==null)
			host = HOST;
		if(port==0)
			port = PORT;
		
		try {
			return m_aSSLContext.getSocketFactory().createSocket(host, port);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Failed to connect to host: " + HOST + ":" +
					PORT + "! " + e.getMessage());
			throw new GTBSSLSocketException("We couldn't make a connection.");
		} catch (IOException e) {
			Log.w(TAG, "IOException Thrown: " + e.getMessage());
			throw new GTBSSLSocketException("We couldn't make a connection.");
		}
	}
	
	/**
	 * Accessor
	 * @return The current instance of the SocketFactory
	 */
	public SSLSocketFactory getSSLSocketFactory()
	{
		return m_aSSF;	
	}
	
	/**
	 * Accessor
	 * 
	 * Return the current SSLSocket if we have successfully established
	 * a connection with the server. Else, throw an exception
	 * that indicates we must successfully handshake before we can
	 * use the connection.
	 * @return The current SSL Socket
	 * @throws GTBSSLSocketException If we failed to handshake
	 * @see haveEstablishedConnection
	 */
	public SSLSocket getSSLSocket() throws GTBSSLSocketException
	{
		if(successfullyEstablishedConn)
			return m_sslSocket;
		else
			throw new GTBSSLSocketException("No Connection");
	}
	
	/**
	 * Accessor
	 * @return The KeyStore
	 */
	public KeyStore getKSKey(){
		return m_kskey;
	}
	
	/**
	 * Accessor
	 * @return The Trusted KeyStore
	 */
	public KeyStore getKSTrust(){
		return m_kstrust;
	}
	
	/**
	 * Get an SSL Session
	 * @return The SSLSession for the connection. A handshake will
	 *   be initiated if necessary and it will block until completed.
	 */
	public SSLSession getSession() {
		return m_sslSocket.getSession();
	}
	
	/**
	 * Mutator
	 * @param i_aCtx Set the current Context
	 */
	public void setContext(Context i_aCtx)
	{
		m_ctx = i_aCtx;
	}
	
	/** Acceessor
	 * 
	 * We have established a connection.
	 */
	public boolean haveEstablishedConnection()
	{
		return successfullyEstablishedConn;
	}
	
	/** Checks that we, at least, have a connection  
	 * @deprecated Prefer nonblocking
	 * @see haveDataConnection
	 */
	public void setSignalStrengthListener()
	{
		TelephonyManager telManager;
	    PhoneStateListener signalListener;
	    Log.v(TAG, "Launching Signal Listener");


        if(Looper.myLooper() == null)
      	  Looper.prepare();
	    signalListener=new PhoneStateListener() {
	           
	       public void onSignalStrengthsChanged(SignalStrength signalStrength)
	       {
	    	   TelephonyManager mgr = (TelephonyManager) 
	    			   m_ctx.getSystemService(Context.TELEPHONY_SERVICE);
	    	   /* I don't think this is necessary now. Only checking 
	    	    * for a connected Data state then should suffice.
	    	    */
	    	   /*if(signalStrength.isGsm())
	    	   {
	    		   Log.v(TAG, "Phone is using GSM");
	    		   Log.v(TAG, "Signal Strength: " + 
	    				   signalStrength.getGsmSignalStrength());
	    		  
	    		   isCurrentSignalStrengthHigh = 
	    				   signalStrength.getGsmSignalStrength() > 5 ? 
	    						   true : false;
	    	   }
	    	   else
	    	   {

	    		   Log.v(TAG, "Phone is using NOT GSM");
	    		   Log.v(TAG, "CDMA RSSI: " + 
	    				   signalStrength.getCdmaDbm());
	    		   Log.v(TAG, "EvDo RSSI: " + 
	    				   signalStrength.getEvdoDbm());
	    		  
	    		   int strength = signalStrength.getCdmaDbm();
	    		   if(strength == 0)
	    			   strength = signalStrength.getEvdoDbm();
	    		   isCurrentSignalStrengthHigh = strength > -100 ? true : false;
	    	   }
	    	   if(!isCurrentSignalStrengthHigh)
	    		   Log.w(TAG, "We don't currently have a strong signal");*/

	   	    if(mgr.getDataState() == TelephonyManager.DATA_CONNECTED)
	   	    	isCurrentSignalStrengthHigh = true;
	       }
	    }; 
	    telManager = (TelephonyManager) 
	    		m_ctx.getSystemService(Context.TELEPHONY_SERVICE);
	    telManager.listen(signalListener, 
	    		PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	    if(telManager.getDataState() == TelephonyManager.DATA_CONNECTED)
	    	isCurrentSignalStrengthHigh = true;
	}
	
	/** @returns true if we currently have a data connection, false otherwise 
	 * 
	 * This is preferred to setSignalStrengthListener and blockOnLowSignal.
	 * 
	 * @see setSignalStrengthListener
	 * @see blockOnLowSignal
	 * */ 
	public boolean haveDataConnection()
	{
		TelephonyManager telManager;
		telManager = (TelephonyManager) 
	    		m_ctx.getSystemService(Context.TELEPHONY_SERVICE);
	    if(telManager.getDataState() == TelephonyManager.DATA_CONNECTED)
	    	isCurrentSignalStrengthHigh = true;
	    else
	    	isCurrentSignalStrengthHigh = false;
	    return isCurrentSignalStrengthHigh;
	}
	
	/** Block until we have strong signal
	 * 
	 * Check every second if our signal is strong enough to
	 * establish a useful connection.
	 * 
	 * @return true when we do
	 * @deprecated Prefer nonblocking
	 * @see haveDataConnection
	 */
	public boolean blockOnLowSignal()
	{
		while(!isCurrentSignalStrengthHigh)
		{
			Log.v(TAG, "Signal is Low");
			try
			{
				Thread.sleep(1000);
			} catch(InterruptedException e)
			{
				/* It doesn't matter if we're interrupted. 
				 * Continue checking.
				 */
			}
			if(isCurrentSignalStrengthHigh)
				break;
		}
		Log.v(TAG, "Signal is no longer too low to establish connection");
		return true;
	}
}