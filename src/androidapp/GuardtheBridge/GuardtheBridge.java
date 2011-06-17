package androidapp.GuardtheBridge;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class GuardtheBridge extends ListActivity {
    /** Called when the activity is first created. */
	private static final int CarNum_SELECT=0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activelist);
        Intent i = new Intent(this, LogintoBridge.class);
    	startActivityForResult(i, CarNum_SELECT);
    }
    
    public boolean onOptionItemSelected(MenuItem menu){
		return super.onOptionsItemSelected(menu);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }
}