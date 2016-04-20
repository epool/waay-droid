package mx.eduardopool.waaydroid

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AlphaAnimation
import mx.eduardopool.waaydroid.databinding.ActivityMainBinding
import org.jetbrains.anko.toast
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null

    private val stringBuilder = StringBuilder()
    private var cardAdapter: CardAdapter? = null

    private var reader: TextToSpeech? = null

    private val fadeIn = AlphaAnimation(0f, 1f)
    private var isGuessing: Boolean = false
    private var numberInMind = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        mBinding!!.buttonYes.setOnClickListener { processSelection("1") }
        mBinding!!.buttonNo.setOnClickListener { processSelection("0") }

        isGuessing = true

        cardAdapter = CardAdapter(this)
        mBinding!!.gridView.adapter = cardAdapter

        reader = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                reader!!.speak("piensa en un número del 1 al 31 y déjame adivinar cual es...", TextToSpeech.QUEUE_ADD, null)
            }
        })
        reader!!.isLanguageAvailable(Locale("spa"))

        fadeIn.duration = 800
    }

    override fun onDestroy() {
        //Close the Text to Speech Library
        if (reader != null) {

            reader!!.stop()
            reader!!.shutdown()
        }
        super.onDestroy()
    }

    private fun processSelection(selection: String) {
        if (isGuessing) {
            cardAdapter!!.passCard()
            stringBuilder.insert(0, selection)
        } else {
            toast("¿El número que pensaste fue $numberInMind?")
            reader!!.speak("¿El número que pensaste fue $numberInMind?", TextToSpeech.QUEUE_ADD, null)
        }
        if (cardAdapter!!.currentCard < cardAdapter!!.cardsNumber) {
            mBinding!!.gridView.startAnimation(fadeIn)
            cardAdapter!!.notifyDataSetChanged()
        } else {
            isGuessing = false
            numberInMind = Integer.parseInt(stringBuilder.toString(), 2).toString()
            toast("¿El número que pensaste fue $numberInMind?")
            reader!!.speak("¿El número que pensaste fue $numberInMind?", TextToSpeech.QUEUE_ADD, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset -> {
                restartGuess()
                toast("Reset!")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun restartGuess() {
        isGuessing = true
        numberInMind = ""
        stringBuilder.delete(0, stringBuilder.length)
        cardAdapter!!.currentCard = 0
        cardAdapter!!.notifyDataSetChanged()
    }

}
