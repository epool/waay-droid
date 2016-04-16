package mx.eduardopool.waaydroid;

import android.app.Activity;
import android.databinding.DataBindingUtil;
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
import android.widget.Toast;

import java.util.Locale;

import mx.eduardopool.waaydroid.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    private ActivityMainBinding mActivityMainBinding;

    private StringBuilder stringBuilder = new StringBuilder();
    private CardAdapter cardAdapter;

    private TextToSpeech reader;

    private Animation fadeIn = new AlphaAnimation(0, 1);
    private boolean isGuessing;
    private String numberInMind = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mActivityMainBinding.buttonYes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                processSelection("1");
            }
        });
        mActivityMainBinding.buttonNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                processSelection("0");
            }
        });

        isGuessing = true;

        cardAdapter = new CardAdapter(this);
        mActivityMainBinding.gridView.setAdapter(cardAdapter);
        mActivityMainBinding.gridView.setOnItemClickListener(new OnItemClickListener() {
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
                        reader.speak("piensa en un número del 1 al 31 y déjame adivinar cual es...", TextToSpeech.QUEUE_ADD, null);
                    }

                }
            }
        });
        reader.isLanguageAvailable(new Locale("spa"));

        fadeIn.setDuration(800);
    }

    @Override
    protected void onDestroy() {
        //Close the Text to Speech Library
        if (reader != null) {

            reader.stop();
            reader.shutdown();
        }
        super.onDestroy();
    }

    private void processSelection(String selection) {
        if (isGuessing) {
            cardAdapter.passCard();
            stringBuilder.insert(0, selection);
        } else {
            Toast.makeText(this, "¿El número que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
            reader.speak("¿El número que pensaste fue " + numberInMind + "?", TextToSpeech.QUEUE_ADD, null);
        }
        if (cardAdapter.getCurrentCard() < cardAdapter.getCardsNumber()) {
            mActivityMainBinding.gridView.startAnimation(fadeIn);
            cardAdapter.notifyDataSetChanged();
        } else {
            isGuessing = false;
            numberInMind = String.valueOf(Integer.parseInt(stringBuilder.toString(), 2));
            Toast.makeText(this, "¿El número que pensaste fue " + numberInMind + "?", Toast.LENGTH_LONG).show();
            reader.speak("¿El número que pensaste fue " + numberInMind + "?", TextToSpeech.QUEUE_ADD, null);
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
