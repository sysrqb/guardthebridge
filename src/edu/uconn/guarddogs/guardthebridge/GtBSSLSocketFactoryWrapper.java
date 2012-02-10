package edu.uconn.guarddogs.guardthebridge;

import java.io.DataInputStream;
import java.io.IOException;
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
import android.util.Log;

public final class GtBSSLSocketFactoryWrapper {
	private static final String TAG = "SSF-GTBLOG";
	private final String HOST = "empathos.dyndns.org";
	private final int PORT = 4680;
	private static SSLSocket m_sslSocket = null;
	private static SSLSocketFactory m_aSSF;
	private static KeyStore m_kstrust, m_kskey;
	private static SSLContext m_aSSLContext = null;
	
	public GtBSSLSocketFactoryWrapper(){}

	public GtBSSLSocketFactoryWrapper(Context i_aCtx){
		
		if(m_sslSocket != null)
			return;
		
		Context aCtx = i_aCtx;
		
		DataInputStream a_tksis = new DataInputStream(aCtx.getResources().openRawResource(R.raw.tkspass));
		DataInputStream a_ksis = new DataInputStream(aCtx.getResources().openRawResource(R.raw.kspass));
		
		KeyStore kstrust;
		try {
			TrustManagerFactory tmf;
			KeyManagerFactory kmf;
			
			KeyStore kskey = KeyStore.getInstance("BKS");
			String kspass = a_ksis.readLine();
			kskey.load(aCtx.getResources().openRawResource(R.raw.gtbbks), kspass.toCharArray());
			System.out.println("Contains gdt1: " + kskey.containsAlias("gdt1"));
			System.out.println("Printing keystore");
			java.security.cert.Certificate aCertGDT1 = kskey.getCertificate("gdt1");
			System.out.println("GDT1 PubKey Format: " + aCertGDT1.getPublicKey().getFormat());
			kmf = KeyManagerFactory.getInstance("X509");
			a_ksis.close();
			m_kskey = kskey;
			
			
			kstrust = KeyStore.getInstance("BKS");
			String tkspass = a_tksis.readLine();
			kstrust.load(aCtx.getResources().openRawResource(R.raw.trust), tkspass.toCharArray());
			System.out.println("Avaiable KMF algorithms: " + TrustManagerFactory.getDefaultAlgorithm());
			tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(kstrust);
			System.out.println("Algorithm being used for TMF: " + tmf.getAlgorithm());
			
			a_tksis.close();			
			m_kstrust = kstrust;
			
			kmf.init(kskey, kspass.toCharArray());
			
			SSLContext aSC = null;
			if (m_aSSLContext == null)
			{
				aSC = SSLContext.getInstance("TLSv1");
				aSC.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				SSLContext.setDefault(aSC);
				m_aSSLContext = aSC;
			}
			else
			{
				aSC = m_aSSLContext;
			}
			Log.v(TAG, "Establishing Connection to server...");
			m_sslSocket = (SSLSocket)aSC.getSocketFactory().createSocket(HOST, PORT);
			Log.v(TAG, "Connected to: " + m_sslSocket.getInetAddress().getCanonicalHostName() + " on Port: " + m_sslSocket.getPort());
			Log.v(TAG, "Local Binding is on: " + m_sslSocket.getLocalAddress().getCanonicalHostName() + " on Port: " + m_sslSocket.getLocalPort());
			Log.v(TAG, "Connection Established. Handshaking...");
			m_sslSocket.setUseClientMode(true);
			try {
				m_sslSocket.startHandshake();
			} catch (IOException e)
			{
				Log.w(TAG, "Handshake Failed");
				e.printStackTrace();
				try {
				m_sslSocket.startHandshake();
				} catch (IOException ex)
				{
					Log.w(TAG, "Failed to establish connection!");
					m_sslSocket = null;
					new GtBSSLSocketFactoryWrapper(i_aCtx);
				}
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void forceReHandshake(Context i_aCtx)
	{
		try {
			Log.v(TAG, "Initiating rehandshake");
			m_sslSocket.startHandshake();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Rehandshake Failure");
			e.printStackTrace();
			System.out.println("Re-Establishing Connection..");
			m_sslSocket = null;
			new GtBSSLSocketFactoryWrapper(i_aCtx);
		}
	}
	
	public GtBSSLSocketFactoryWrapper getNewSSLSFW(Context i_aCtx)
	{
		System.out.println("Re-Establishing Connection; from scratch.");
		m_sslSocket = null;
		return new GtBSSLSocketFactoryWrapper(i_aCtx);
	}
	
	public SSLSocket reconnect()
	{
		SSLSocket tmp = (SSLSocket)createSocket(null, 0, true);
		tmp.setUseClientMode(true);
		return tmp;
	}
	
	public SSLSocket createSSLSocket (Context i_aCtx){
		m_sslSocket = (SSLSocket) createSocket(null, 0, true);
		forceReHandshake(i_aCtx);
		return m_sslSocket;
	}
	
	public Socket createSocket(String host, int port, boolean autoClose)
	{
		if(host==null)
			host = HOST;
		if(port==0)
			port = PORT;
		
		try {
			return m_aSSLContext.getSocketFactory().createSocket(host, port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public SSLSocketFactory getSSLSocketFactory()
	{
		return m_aSSF;	
	}
	
	public SSLSocket getSSLSocket()
	{
		return m_sslSocket;
	}
	
	public KeyStore getKSKey(){
		return m_kskey;
	}
	
	public KeyStore getKSTrust(){
		return m_kstrust;
	}
	
	public SSLSession getSession() {
		return m_sslSocket.getSession();
	}
}
