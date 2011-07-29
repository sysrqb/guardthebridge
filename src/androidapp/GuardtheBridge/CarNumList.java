package androidapp.GuardtheBridge;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CarNumList extends ListActivity {
    private CarsGtBDbAdapter mDbHelper;
    private TLSGtBDbAdapter nGDbHelper; //Ngin DB Helper
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new CarsGtBDbAdapter(this);
        mDbHelper.open();
		listCars();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		System.out.println("Index: " + mDbHelper.setCar(position+1));//Position starts at 0, so add 1
		System.out.println("Car Number: " + position);
		setResult(RESULT_OK);
		finish();
	}
	
	public void listCars(){
		String cars[];
		int num = numberOfCars();
		if(num < 0){
			num = 5;
		}
		cars = new String[num];
		for(int i = 0; i<num; i++){
			cars[i] = "Car " + (i+1);
		}
		setListAdapter(new ArrayAdapter<String>(this, R.layout.carnums, cars));
		System.out.println("Num of Cars: " + cars.length);
	}
		
	public int numberOfCars(){
		String myserver = "empathos.dyndns.org";
		GtBSSLHandler sslconn = new GtBSSLHandler(myserver, 4680);
		/*Socket send;
		OutputStream out = null;
		InputStream in;
		int bytesread;*/
		byte[] numofcars = {};
		String key = "CARS";
		System.out.println("Getting Car");
		
		//Establish connection
		sslconn.init();
		
		/*send = new Socket(myserver, 4680);
		System.out.println("Server IP: " + send.getInetAddress());
		System.out.println("Local IP: " + send.getLocalAddress());
		
		//Send type key
		out = send.getOutputStream();
		out.write(key.getBytes());
		
		//Receive number of cars from server
		in = send.getInputStream();
		numofcars = new byte[1];
		bytesread = in.read(numofcars);
		System.out.println("Bytes read: " + bytesread);
		if(bytesread == 0)
			in.read(numofcars);*/
		
		sslconn.send(key.getBytes());
		numofcars = sslconn.receive();			
		String number = Byte.toString(numofcars[0]);
		nGDbHelper.open();
		nGDbHelper.setSession(sslconn.flatten());
		System.out.println("Number of Cars: " + number);
		return Integer.parseInt(number); //LogintoBridge uses getInteger, not sure if this makes a difference
	}
}
