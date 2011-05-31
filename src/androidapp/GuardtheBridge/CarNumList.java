package androidapp.GuardtheBridge;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CarNumList extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.carnums);
		ArrayList<String> carList = retrieveCars(); //Either returns ArrayList or Cursor
		listCars(carList);
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		setResult(RESULT_OK);
		finish();
	}

	//TODO Need to implement 
	public ArrayList<String> retrieveCars(){
		int num = 5;
		ArrayList<String> carList = new ArrayList<String>();
		for(int i = 0; i<num; i++){
			carList.add("Car " + (i+1));
		}
		return carList;
	}
	
	public void listCars(ArrayList<String> cars){
		ArrayAdapter<String> aA = new ArrayAdapter<String>(this, R.id.cars);
		setListAdapter(aA);
	}
	
	

}
