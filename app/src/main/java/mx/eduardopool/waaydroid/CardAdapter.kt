package mx.eduardopool.waaydroid

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.TextView

/**

 * Created by EduardoPool on 17/08/14.
 */
class CardAdapter(
        private val mContext: Context,
        var currentCard: Int = 0,
        val cardsNumber: Int = 5,
        private val magicCardsGenerator: MagicCardsGenerator = MagicCardsGenerator(cardsNumber)
) : BaseAdapter() {

    override fun getCount(): Int {
        return magicCardsGenerator.cards[currentCard].size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    // create a new ImageView for each item referenced by the Adapter
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val textView: TextView?
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            textView = TextView(mContext)
            textView.layoutParams = AbsListView.LayoutParams(150, 150)
            textView.gravity = Gravity.CENTER
            textView.setBackgroundResource(R.drawable.black_border)
        } else {
            textView = convertView as TextView?
        }

        textView?.text = magicCardsGenerator.cards[currentCard][position].toString()
        return textView
    }

    fun passCard() {
        currentCard++
    }
}