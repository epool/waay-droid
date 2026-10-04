package mx.eduardopool.waaydroid;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 *
 * Created by EduardoPool on 17/08/14.
 */
public class CardAdapter extends BaseAdapter {
    private Context mContext;
    private MagicCardsGenerator magicCardsGenerator;
    private Integer currentCard;
    private Integer cardsNumber;

    public CardAdapter(Context c) {
        mContext = c;
        cardsNumber = 5;
        currentCard = 0;
        magicCardsGenerator = new MagicCardsGenerator(cardsNumber);
    }

    public int getCount() {
        return visibleCard().size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            textView = new TextView(mContext);
            int cellSize = mContext.getResources().getDimensionPixelSize(R.dimen.card_cell_size);
            textView.setLayoutParams(new GridView.LayoutParams(cellSize, cellSize));
            textView.setGravity(Gravity.CENTER);
            textView.setBackgroundResource(R.drawable.black_border);
        } else {
            textView = (TextView) convertView;
        }

        textView.setText(visibleCard().get(position).toString());
        return textView;
    }

    public void passCard() {
        currentCard++;
    }

    public Integer getCurrentCard() {
        return currentCard;
    }

    public void setCurrentCard(Integer currentCard) {
        this.currentCard = currentCard;
    }

    public Integer getCardsNumber() {
        return cardsNumber;
    }

    /** Highest number the cards can reveal: (2 ^ cardsNumber) - 1. */
    public int getMaxNumber() {
        return (1 << cardsNumber) - 1;
    }

    // Once every card has been answered currentCard equals cardsNumber, which is one past
    // the last card; keep showing the last card instead of indexing out of bounds.
    private List<Integer> visibleCard() {
        int index = Math.min(currentCard, cardsNumber - 1);
        return magicCardsGenerator.getCards().get(index);
    }
}