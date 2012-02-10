package edu.uconn.guarddogs.guardthebridge.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.net.ssl.SSLSocket;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.uconn.guarddogs.guardthebridge.Communication.Request;
import edu.uconn.guarddogs.guardthebridge.Communication.Response;
import edu.uconn.guarddogs.guardthebridge.GtBDbAdapter;
import edu.uconn.guarddogs.guardthebridge.GtBSSLSocketFactoryWrapper;
import edu.uconn.guarddogs.guardthebridge.GuardtheBridge;
import edu.uconn.guarddogs.guardthebridge.Patron;
import edu.uconn.guarddogs.guardthebridge.Patron.PatronInfo;

public class GuardtheBridgeTest extends
		ActivityInstrumentationTestCase2<GuardtheBridge> {
	private static final String TAG = "GTB-Test";
	private GuardtheBridge m_aGTBAct;
	public GuardtheBridgeTest(){
		super("edu.uconn.guarddogs.guardthebridge", GuardtheBridge.class);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
		setActivityInitialTouchMode(false);
		m_aGTBAct = getActivity();
	}
	
	public void testGetCurrent()
	{
		GtBSSLSocketFactoryWrapper sslSF = new GtBSSLSocketFactoryWrapper(m_aGTBAct);
		Request aPBReq = Request.newBuilder().
				   setNReqId(1).
				   setSReqType("CURR").
				   setNCarId(5).
				   build();
		Log.v(TAG, "Testing CurrIsInit");
		assertTrue("CurrIsInit", aPBReq.isInitialized());
		Log.v(TAG, "Testing CurrId");
		assertEquals("CurrId", aPBReq.getNReqId(), 0);
		Log.v(TAG, "Testing CurrType");
		assertEquals("CurrType", aPBReq.getSReqType(), "CURR");
		Log.v(TAG, "Testing CurrCar");
		assertEquals("CurrCar", aPBReq.getNCarId(), 5);
		Log.v(TAG, "Testing CurrId");
		byte [] vbuft = aPBReq.toByteArray();
		Request aTemp;
		try {
			aTemp = Request.parseFrom(vbuft);
			assertEquals("CurrId", aTemp.getNReqId(), 0);
		} catch (InvalidProtocolBufferException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		Log.v(TAG, "Getting Socket");
		SSLSocket aSock = sslSF.getSSLSocket();
		if (aSock.isClosed())
			aSock = sslSF.createSSLSocket(this.getActivity());
		try
		{
			Log.v(TAG, "Sending Size");
			OutputStream aOS = aSock.getOutputStream();
			aOS.write(aPBReq.getSerializedSize());
			byte[] vbuf = aPBReq.toByteArray();
			//aPBReq.writeTo(aOS);
			Log.v(TAG, "Sending Payload");
			aOS.write(vbuf);
			InputStream aIS = aSock.getInputStream();
			vbuf = new byte[1];
			Log.v(TAG, "Receiving Payload");
			aIS.read(vbuf);
			vbuf = new byte[vbuf[0]];
			Log.v(TAG, "Parsing Payload");
			Response.parseFrom(vbuf).getPlPatronList();
		}catch (IOException e)
		{
			e.printStackTrace();
		}
		
		GtBDbAdapter aGDbHelper = new GtBDbAdapter(m_aGTBAct);
		aGDbHelper.open();
		PatronInfo atmp = aGDbHelper.fetchPatron(1);
		Patron.PatronInfo[] vPI = aGDbHelper.fetchAllPatrons();
	}	
}
