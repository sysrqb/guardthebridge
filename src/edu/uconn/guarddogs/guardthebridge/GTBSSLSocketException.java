package edu.uconn.guarddogs.guardthebridge;

public class GTBSSLSocketException extends Exception {
	String m_msg;
	
	GTBSSLSocketException(String msg)
	{
		super();
		m_msg = msg;
	}

}
