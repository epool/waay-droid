package mx.wawisoft.waaydroid;

import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
	
	private TextToSpeech reader;

	private Animation fadeIn = new AlphaAnimation(0, 1);
	
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
	    
	    reader = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.SUCCESS) {
					int result = TextToSpeech.LANG_COUNTRY_AVAILABLE;
					if (result == TextToSpeech.LANG_MISSING_DATA
							|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
//						Toast.makeText(getApplication(), "Lenguaje no soportado",
//								Toast.LENGTH_LONG).show();
					} else {
//						Toast.makeText(getApplication(), "Lenguaje soportado",
//								Toast.LENGTH_LONG).show();
						reader.speak("piensa en un número del 1 al 31 y dejame adivinar cual es...", TextToSpeech.QUEUE_ADD, null);
					}

				}
			}	    	
	    });
		reader.isLanguageAvailable(new Locale("spa"));
		
	    fadeIn.setDuration(800);
	}
	
	private boolean isGuessing;
	private String numberInMind = "";
	
	private void processSelection(String selection) {
		if (isGuessing) {
			cardAdapter.passCard();
			stringBuilder.insert(0, selection);
		} else {
			Toast.makeText(this, "¿El numero que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
			reader.speak("¿El numero que pensaste fue " + numberInMind + "?", TextToSpeech.QUEUE_ADD, null);
		}
		if (cardAdapter.getCurrentCard() < cardAdapter.getCardsNumber()) {
			gridview.startAnimation(fadeIn);
			cardAdapter.notifyDataSetChanged();
		} else {
			isGuessing = false;
			numberInMind = String.valueOf(Integer.parseInt(stringBuilder.toString(), 2));
			Toast.makeText(this, "¿El numero que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
			reader.speak("¿El numero que pensaste fue " + numberInMind + "?", TextToSpeech.QUEUE_ADD, null);
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
	
//	private static final int RESULT_SPEECH = 1;
//	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX");	        
//    try {
//        startActivityForResult(intent, RESULT_SPEECH);
//    } catch (ActivityNotFoundException a) {
//        Toast t = Toast.makeText(getApplicationContext(),
//                "Opps! Your device doesn't support Speech to Text",
//                Toast.LENGTH_SHORT);
//        t.show();
//    }
//	@Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data); 
//        switch (requestCode) {
//        case RESULT_SPEECH:
//            if (resultCode == RESULT_OK && null != data) {
//                ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);                
//                Toast.makeText(this, text.get(0), Toast.LENGTH_LONG).show();
//            }
//            break; 
//        }
//    }
	
}
