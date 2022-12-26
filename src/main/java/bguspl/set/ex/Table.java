package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    protected final List<Integer>[] tokensToSlot;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        tokensToSlot = new LinkedList[slotToCard.length];
        for(int i = 0; i< tokensToSlot.length;i++){
            tokensToSlot[i] = new LinkedList<>();
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public synchronized int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public synchronized void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card,slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        if (slotToCard[slot] != null) {
            int card = slotToCard[slot];
            env.ui.removeCard(slot);
            cardToSlot[card] = null;
            slotToCard[slot] = null;
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        if (slotToCard[slot]!= null) {
            tokensToSlot[slot].add(player);
            env.ui.placeToken(player, slot);
        }
    }

    public boolean placeToken(int player,int slot, int card)
    {
        boolean placed = false;
        synchronized (this) {
            if (slotToCard[slot] != null && card == slotToCard[slot]) {
                int counter=0;
                for(int i=0;i<tokensToSlot.length&counter<3;i++)
                {
                    if (tokensToSlot[i].contains(player))
                        counter++;
                }
                if (counter<3){
                    placed = true;
                    placeToken(player, slot);}
            }
        }
        return placed;
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        for (int i=0; i<tokensToSlot[slot].size();i++)
        {
            if (tokensToSlot[slot].get(i)==player) {
                tokensToSlot[slot].remove(i);
                env.ui.removeToken(player,slot);
                return true;
            }
        }
        return false;
    }

    public synchronized int[] GetPlayerCards(int playerId)
    {
        int[] cards = new int[3];
        int cardIndex = 0;
        List<Integer>[] copyList = tokensToSlot.clone();
        for (int i=0; i<copyList.length;i++)
        {
            for (int j:copyList[i]) {
                if(j == playerId)
                    cards[cardIndex++] = slotToCard[i];
            }
        }
        return cards;
    }

    public void removeAllTokens() {
        for(int i = 0; i< tokensToSlot.length; i++) {
            tokensToSlot[i].clear();
        }
        env.ui.removeTokens();
    }
}

