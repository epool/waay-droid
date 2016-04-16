package mx.eduardopool.waaydroid;

import java.util.ArrayList;
import java.util.List;

/**
 * Cards generator.
 * Created by EduardoPool on 17/08/14.
 */
public class MagicCardsGenerator {

    private int cardsNumber;
    private int maxNumber;
    private List<List<Integer>> cards;

    public MagicCardsGenerator(Integer cardsNumber) {
        this.cardsNumber = cardsNumber;
        this.cards = new ArrayList<>(cardsNumber);
        calculateCardsNumbers();
    }

    public void calculateCardsNumbers() {
        cards.clear();
        maxNumber = (int) Math.pow(2, cardsNumber);
        for (int cardIndex = 0; cardIndex < cardsNumber; cardIndex++) {
            cards.add(calculateNumbersForCardIndex(cardIndex));
        }
    }

    private List<Integer> calculateNumbersForCardIndex(int cardIndex) {
        List<Integer> card = new ArrayList<>();
        int jumpNumber = (int) Math.pow(2, cardIndex);
        boolean isAdd = true;
        for (int numberToAdd = jumpNumber, count = 0; numberToAdd < maxNumber; numberToAdd++, count++) {
            if (count == jumpNumber) {
                count = 0;
                isAdd = !isAdd;
            }
            if (isAdd) {
                card.add(numberToAdd);
            }
        }
        return card;
    }

    public int getCardsNumber() {
        return cardsNumber;
    }

    public void setCardsNumber(int cardsNumber) {
        this.cardsNumber = cardsNumber;
    }

    public List<List<Integer>> getCards() {
        return cards;
    }

    public void setCards(List<List<Integer>> cards) {
        this.cards = cards;
    }

}
