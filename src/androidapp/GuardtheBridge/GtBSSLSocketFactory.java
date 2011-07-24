package androidapp.GuardtheBridge;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

public class GtBSSLSocketFactory extends SSLSocketFactory {

	@Override
	public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSupportedCipherSuites() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
