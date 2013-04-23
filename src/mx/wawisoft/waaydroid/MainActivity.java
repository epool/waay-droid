package mx.wawisoft.waaydroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Button yesButton;
	private Button noButton;
	private StringBuilder stringBuilder;
	private CardAdapter cardAdapter;
	private GridView gridview;

	Animation fadeIn = new AlphaAnimation(0, 1);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	    
	    stringBuilder = new StringBuilder();

	    gridview = (GridView) findViewById(R.id.gridview);
	    cardAdapter = new CardAdapter(this);
	    gridview.setAdapter(cardAdapter);
	    
	    yesButton = (Button) findViewById(R.id.buttonYes);
	    yesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				processSelection("1");
			}
	    });
	    noButton = (Button) findViewById(R.id.buttonNo);
	    noButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				processSelection("0");
			}
	    });
	    
	    isGuessing = true;
	    
	    gridview.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
	        }
	    });
	    
	    fadeIn.setDuration(800);
	}
	
	private boolean isGuessing;
	private String numberInMind = "";
	
	private void processSelection(String selection) {
		if (isGuessing) {
			cardAdapter.passCard();
			stringBuilder.insert(0, selection);
		} else {
			Toast.makeText(this, "El numero que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
		}
		if (cardAdapter.getCurrentCard() < cardAdapter.getCardsNumber()) {
			gridview.startAnimation(fadeIn);
			cardAdapter.notifyDataSetChanged();
		} else {
			isGuessing = false;
			numberInMind = String.valueOf(Integer.parseInt(stringBuilder.toString(), 2));
			Toast.makeText(this, "El numero que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_reset:
			restartGuess();
			Toast.makeText(this, "Reset!", Toast.LENGTH_LONG).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void restartGuess() {
		isGuessing = true;
		numberInMind = "";
		stringBuilder.delete(0, stringBuilder.length());
		cardAdapter.setCurrentCard(0);
		cardAdapter.notifyDataSetChanged();
	}

}
