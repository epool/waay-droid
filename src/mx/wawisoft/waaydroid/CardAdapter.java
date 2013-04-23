package mx.wawisoft.waaydroid;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

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
       return magicCardsGenerator.getCards().get(currentCard).size();
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
        	textView.setLayoutParams(new GridView.LayoutParams(150, 150));
        	textView.setGravity(Gravity.CENTER);
        	textView.setBackgroundResource(R.drawable.black_border);
        } else {
        	textView = (TextView) convertView;
        }

        textView.setText(magicCardsGenerator.getCards().get(currentCard).get(position).toString());
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
}