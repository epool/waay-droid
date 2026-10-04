package mx.eduardopool.waaydroid;

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
    private boolean readerReady;

    private Animation fadeIn = new AlphaAnimation(0, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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

        reader = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && setSpanishLanguage()) {
                    readerReady = true;
                    speak(getString(R.string.intro_think_of_number, cardAdapter.getMaxNumber()));
                } else {
                    Toast.makeText(MainActivity.this, R.string.tts_spanish_unavailable,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        fadeIn.setDuration(800);
    }

    @Override
    protected void onDestroy() {
        //Close the Text to Speech Library
        if(reader != null) {

            reader.stop();
            reader.shutdown();
        }
        super.onDestroy();
    }

    private boolean isGuessing;
    private String numberInMind = "";

    private void processSelection(String selection) {
        if (!isGuessing) {
            // The game is over; the buttons just repeat the answer.
            announceResult();
            return;
        }
        cardAdapter.passCard();
        stringBuilder.insert(0, selection);
        if (cardAdapter.getCurrentCard() < cardAdapter.getCardsNumber()) {
            gridview.startAnimation(fadeIn);
            cardAdapter.notifyDataSetChanged();
        } else {
            isGuessing = false;
            numberInMind = String.valueOf(Integer.parseInt(stringBuilder.toString(), 2));
            announceResult();
        }
    }

    private void announceResult() {
        String question = getString(R.string.result_question, numberInMind);
        Toast.makeText(this, question, Toast.LENGTH_LONG).show();
        speak(question);
    }

    private boolean setSpanishLanguage() {
        int result = reader.setLanguage(new Locale("es", "MX"));
        if (isLanguageUnavailable(result)) {
            result = reader.setLanguage(new Locale("es"));
        }
        return !isLanguageUnavailable(result);
    }

    private boolean isLanguageUnavailable(int result) {
        return result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED;
    }

    private void speak(String text) {
        if (readerReady) {
            reader.speak(text, TextToSpeech.QUEUE_ADD, null);
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
                Toast.makeText(this, R.string.reset_done, Toast.LENGTH_LONG).show();
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
