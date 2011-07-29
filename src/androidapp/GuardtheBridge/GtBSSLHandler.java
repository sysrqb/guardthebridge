package androidapp.GuardtheBridge;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class GtBSSLHandler implements Serializable{
	/**
	 * Default Serial Version
	 */
	private static final long serialVersionUID = 1L;
	private String host;
	private int port;
	private SSLEngine ssle;
	private ByteBuffer lAppData, lNetData, pAppData, pNetData;
	private SSLSession ssls;
	
	public GtBSSLHandler(String hn, int pn){
		host = hn; //hostname
		port = pn; //port number
	}
	
	public SSLEngine init(){
		//Load keys and trusted files
		final char[] passphrase = "".toCharArray();
		try {
			KeyStore kskey = KeyStore.getInstance("JKS");
			kskey.load(new FileInputStream("PK"), passphrase);
			KeyStore kstrust = KeyStore.getInstance("JKS");
			kstrust.load(new FileInputStream("TC"), passphrase);
			
			//Init instance of KeyManger of SunX509
			KeyManagerFactory km = KeyManagerFactory.getInstance("SunX509");
			km.init(kskey, passphrase);
			
			//Init instance of TrustedManager of SunX509 
			//to accept/reject connections
			TrustManagerFactory tm = 
				TrustManagerFactory.getInstance("SunX509");
			tm.init(kstrust);
			
			//Init SSLContext instance
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(km.getKeyManagers(), tm.getTrustManagers(), null);
			
			//Get engine instance
			SSLEngine engine = sslContext.createSSLEngine(host, port);
			
			//Enable for client mode
			engine.setUseClientMode(true);
			ssle = engine;
			ssls = handshake();
			return engine;
	  	} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private SSLSession handshake(){
		SSLEngine engine = ssle;
		try {
			//Create non-blocking socket
			SocketChannel sC = SocketChannel.open();
			sC.configureBlocking(false);
			sC.connect(new InetSocketAddress(engine.getPeerHost(), engine.getPeerPort()));
			
			//Wait while it finishes establishing connection
			//TODO
			//...figure out a better way to do this
			while(!sC.finishConnect()){
			}
			
			//Get the buffers
			SSLSession session = engine.getSession();
			ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
			ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
			ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
			ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
			
			//Begin Handshake
			engine.beginHandshake();
			
			SSLEngineResult.HandshakeStatus hss = engine.getHandshakeStatus();
			SSLEngineResult res;
			while(hss != SSLEngineResult.HandshakeStatus.FINISHED && 
					hss != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING){
				switch (hss){
				case NEED_UNWRAP :
					//Receive hs data from peer
					if(sC.read(peerNetData) < 0){
						//Write completed, closed channel, deal with it
						sC.close();
						break;
					}
					//Process incoming data
					peerNetData.flip();
					res = engine.unwrap(peerNetData, peerAppData);
					peerNetData.compact();
					hss = engine.getHandshakeStatus();
					
					//Check current status
					switch(res.getStatus()){
					case OK :
						break;
					//TODO
						//Handle other cases (BOF, BUF, CLOSED, etc 
						//from malformed packets and malicious MITM
					}
					break;
				
				case NEED_WRAP :
					//Make sure the local buffer is empty
					myNetData.clear();
					
					//Wrap it all up...better to be safe than sorry
					res = engine.wrap(myAppData, myNetData);
					hss = res.getHandshakeStatus();
					
					//Check new status
					switch (res.getStatus()){
					case OK :
						//Send it on its way
						while (myNetData.hasRemaining()){
							myNetData.flip();
							if (sC.write(myNetData) < 0){
								//Write completed, closed channel, deal with it
								sC.close();
								break;
							}							
						}
						break;
						
					//TODO
					//Handle other cases (BOF, BUF, CLOSED, etc) 
					}
					break;
					
				case NEED_TASK :
					//Handle blocking tasks
					break;
					
					//TODO
					//Handle other stats: FINISHED and NOT_HAND
				}		
			}
			lAppData = myAppData;
			lNetData = myNetData;
			pNetData = peerNetData;
			return session;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public SSLEngine getEngine(){
		return ssle;
	}
	
	public SSLSession getSession(){
		return ssls;
	}
	
	public ByteBuffer[] getBuffers(){
		ByteBuffer[] temp = new ByteBuffer[4];
		temp[0] = lAppData;
		temp[1] = lNetData;
		temp[2] = pAppData;
		temp[3] = pNetData;
		return temp;
	}
	
	public int send(byte[] buf){
		SSLEngine engine = ssle;
		ByteBuffer myAppData = ByteBuffer.allocate( (engine.getSession() ).getApplicationBufferSize());
		ByteBuffer myNetData = ByteBuffer.allocate( (engine.getSession() ).getPacketBufferSize());
		myAppData.put(buf);
		myAppData.flip();
		
		try {
			SocketChannel sC = SocketChannel.open();
			sC.configureBlocking(false);
			sC.connect(new InetSocketAddress(host, port));		
			while (myAppData.hasRemaining()){
				//Prepare buf for encrypted travel
				SSLEngineResult res = engine.wrap(myAppData, myNetData);
				
				//Check status
				if (res.getStatus() == SSLEngineResult.Status.OK){ //Ready to send
					myAppData.compact();
					
					//Send the communique
					while (myNetData.hasRemaining()){
						int num = sC.write(myNetData);
						if (num == -1)
							//Connection Closed
							break;
						else if (num == 0){
							//No bytes written
							break;
						}
					}
				}
			}
			sC.close();
			return 0;
		} catch (SSLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -3;
		}
	}
	
	public byte[] receive(){
		SSLEngine engine = ssle;
		ByteBuffer peerAppData = ByteBuffer.allocate( (engine.getSession() ).getApplicationBufferSize());
		ByteBuffer peerNetData = ByteBuffer.allocate( (engine.getSession() ).getPacketBufferSize());
		ByteBuffer ret = ByteBuffer.allocate(1);
		ret.put("empty".getBytes());
		
		try {
			SocketChannel sC = SocketChannel.open();
			sC.configureBlocking(false);
			sC.connect(new InetSocketAddress(host, port));
			
			//Read data from server
			int num = sC.read(peerNetData);
			if (num == -1)
				//Connection Closed
				return ret.array();
			else if (num == 0)
				//No bytes read
				return ret.array();
			else {
				sC.close();
				peerNetData.flip();
				SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
				if (res.getStatus() == SSLEngineResult.Status.OK){
					peerNetData.compact();
					
					//return received info
					if (peerAppData.hasRemaining())
						return peerAppData.array();
					else
						return ret.array();
				}
			}
			return ret.array();
		} catch (SSLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return receive();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return receive();
		}
	}
	
	public ObjectOutputStream flatten(){
		ByteArrayOutputStream baout = new ByteArrayOutputStream();
		ObjectOutputStream oout = null; 
		try {
			oout = new ObjectOutputStream(baout);
			oout.writeObject(this);
			oout.close();
			return oout;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public GtBSSLHandler inflate(byte[] in){
		ByteArrayInputStream bain = null;
		ObjectInputStream oin;
		GtBSSLHandler sslh;
		bain = new ByteArrayInputStream(in);
		try {
			oin = new ObjectInputStream(bain);
			sslh = (GtBSSLHandler) oin.readObject();
			return sslh;
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	//TODO
	/*Confirm Handshake is successful
	Generate and Sign cert
	Continue implementing SSLEngine.wraps/unwraps*/


}
