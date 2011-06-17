package androidapp.GuardtheBridge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CarNumList extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		listCars();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		setResult(RESULT_OK);
		finish();
	}
	
	public void listCars(){
		String cars[];
		int num = numberOfCars();
		if(num < 0){
			num = 1;
		}
		cars = new String[num];
		for(int i = 0; i<num; i++){
			cars[i] = "Car " + (i+1);
		}
		setListAdapter(new ArrayAdapter<String>(this, R.layout.carnums, cars));
		System.out.println("Num of Cars: " + cars.length);
	}
	
	public int numberOfCars(){
		Socket send;
		OutputStream out = null;
		ObjectInputStream in;
		String myserver = "empathos.dyndns.org";
		System.out.println("Get Car");
		try {
			send = new Socket(myserver, 4680);
			out = send.getOutputStream();
			out.write("CARS".getBytes());
			in = new ObjectInputStream(send.getInputStream());
			String numb = (String) in.readObject();
			return Integer.parseInt(numb);
		} catch (UnknownHostException e1) {
			System.out.println("UnknownHostException");
			return -1;
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("StreamCorruptionException");
			return -2;
		} catch (IOException e1) {
			System.out.println("IOException");
			return -3;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("ClassNotFoundException: String class not found");
			return -4;
		}
	}
}
